package com.example.travelManager.service;

import com.example.travelManager.domain.response.place.PlaceCandidateDto;
import com.example.travelManager.domain.response.place.PlacePhotoDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * Proxy các API của Google Places (New) — khoá API chỉ nằm ở server, không bao giờ lộ ra frontend.
 * Docs: https://developers.google.com/maps/documentation/places/web-service/text-search
 */
@Slf4j
@Service
public class GooglePlacesService {

    private static final String SEARCH_TEXT_URL = "https://places.googleapis.com/v1/places:searchText";
    private static final String SEARCH_FIELD_MASK = "places.id,places.displayName,places.formattedAddress,"
            + "places.addressComponents,places.location,places.photos,"
            + "places.rating,places.userRatingCount,places.editorialSummary";

    @Value("${google.places.api-key}")
    private String apiKey;

    private final RestClient restClient = RestClient.create();
    private final ObjectMapper mapper = new ObjectMapper();

    public record PhotoBytesResult(byte[] bytes, String contentType) {}

    public List<PlaceCandidateDto> textSearch(String query) {
        // Fetch dạng String rồi tự parse bằng Jackson 2 — Spring Boot 4 dùng Jackson 3
        // (tools.jackson.*) làm converter mặc định cho RestClient, không tương thích
        // trực tiếp với JsonNode của Jackson 2 (com.fasterxml.jackson.*).
        String rawBody = restClient.post()
                .uri(SEARCH_TEXT_URL)
                .header("X-Goog-Api-Key", apiKey)
                .header("X-Goog-FieldMask", SEARCH_FIELD_MASK)
                .contentType(MediaType.APPLICATION_JSON)
                .body(java.util.Map.of("textQuery", query, "languageCode", "vi"))
                .retrieve()
                .body(String.class);
        JsonNode response = parseJson(rawBody);

        List<PlaceCandidateDto> candidates = new ArrayList<>();
        if (response == null || !response.has("places")) {
            return candidates;
        }
        for (JsonNode place : response.get("places")) {
            PlaceCandidateDto dto = new PlaceCandidateDto();
            dto.setPlaceId(text(place, "id"));
            dto.setDisplayName(place.has("displayName") ? text(place.get("displayName"), "text") : null);
            dto.setFormattedAddress(text(place, "formattedAddress"));
            dto.setCity(guessCity(place.get("addressComponents")));
            if (place.has("location")) {
                JsonNode loc = place.get("location");
                dto.setLatitude(loc.has("latitude") ? loc.get("latitude").asDouble() : null);
                dto.setLongitude(loc.has("longitude") ? loc.get("longitude").asDouble() : null);
            }
            dto.setPhotos(parsePhotos(place.get("photos")));
            dto.setRating(place.has("rating") ? place.get("rating").asDouble() : null);
            dto.setUserRatingCount(place.has("userRatingCount") ? place.get("userRatingCount").asInt() : null);
            if (place.has("editorialSummary")) {
                dto.setEditorialSummary(text(place.get("editorialSummary"), "text"));
            }
            candidates.add(dto);
        }
        return candidates;
    }

    public PhotoBytesResult fetchPhotoBytes(String photoName, int maxWidthPx) {
        String mediaUrl = "https://places.googleapis.com/v1/" + photoName + "/media"
                + "?maxWidthPx=" + maxWidthPx + "&skipHttpRedirect=true&key=" + apiKey;
        JsonNode mediaInfo;
        try {
            mediaInfo = parseJson(restClient.get().uri(mediaUrl).retrieve().body(String.class));
        } catch (org.springframework.web.client.ResourceAccessException e) {
            log.error("Lỗi kết nối tới Google Places API khi lấy ảnh (photoName={})", photoName);
            throw new IllegalStateException("Không thể kết nối tới Google Places API, vui lòng thử lại sau");
        }
        if (mediaInfo == null || !mediaInfo.has("photoUri")) {
            throw new IllegalStateException("Google không trả về ảnh cho photo reference: " + photoName);
        }
        String photoUri = mediaInfo.get("photoUri").asText();

        return restClient.get()
                .uri(URI.create(photoUri))
                .exchange((request, response) -> {
                    if (!response.getStatusCode().is2xxSuccessful()) {
                        log.error("Google trả lỗi khi tải ảnh (photoName={}, status={})",
                                photoName, response.getStatusCode());
                        throw new IllegalStateException("Không tải được ảnh từ Google, vui lòng thử lại sau");
                    }
                    byte[] bytes = response.getBody().readAllBytes();
                    HttpHeaders headers = response.getHeaders();
                    String contentType = headers.getContentType() != null
                            ? headers.getContentType().toString()
                            : MediaType.IMAGE_JPEG_VALUE;
                    return new PhotoBytesResult(bytes, contentType);
                });
    }

    private List<PlacePhotoDto> parsePhotos(JsonNode photosNode) {
        List<PlacePhotoDto> photos = new ArrayList<>();
        if (photosNode == null || !photosNode.isArray()) return photos;
        for (JsonNode p : photosNode) {
            String name = text(p, "name");
            if (name == null) continue;
            int widthPx = p.has("widthPx") ? p.get("widthPx").asInt() : 0;
            int heightPx = p.has("heightPx") ? p.get("heightPx").asInt() : 0;
            photos.add(new PlacePhotoDto(name, widthPx, heightPx));
        }
        return photos;
    }

    private String guessCity(JsonNode addressComponents) {
        if (addressComponents == null || !addressComponents.isArray()) return null;
        String fallback = null;
        for (JsonNode component : addressComponents) {
            JsonNode types = component.get("types");
            if (types == null) continue;
            for (JsonNode t : types) {
                String type = t.asText();
                if ("locality".equals(type)) {
                    return text(component, "longText");
                }
                if ("administrative_area_level_1".equals(type) && fallback == null) {
                    fallback = text(component, "longText");
                }
            }
        }
        return fallback;
    }

    private String text(JsonNode node, String field) {
        return node != null && node.has(field) ? node.get(field).asText() : null;
    }

    private JsonNode parseJson(String body) {
        if (body == null || body.isBlank()) return null;
        try {
            return mapper.readTree(body);
        } catch (Exception e) {
            log.error("Không parse được JSON từ Google Places", e);
            return null;
        }
    }
}
