package com.example.travelManager.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;

/**
 * Client cho OpenTripMap (https://dev.opentripmap.org) — dùng để backfill dữ liệu
 * tham khảo (ảnh, mô tả) cho Hotel/Restaurant/Tour. Free tier, không cần billing,
 * thay thế cho Google Places ở đúng tính năng backfill (picker thủ công của admin
 * vẫn dùng GooglePlacesService như cũ, không đổi).
 *
 * Lưu ý: fetch body dạng String rồi tự parse bằng Jackson 2 ObjectMapper — Spring
 * Boot 4 dùng Jackson 3 (package tools.jackson.*) làm converter mặc định cho
 * RestClient, không tương thích với JsonNode của Jackson 2 (com.fasterxml.jackson.*)
 * mà project đang dùng, nên .body(JsonNode.class) trực tiếp sẽ lỗi InvalidDefinitionException.
 */
@Slf4j
@Service
public class OpenTripMapService {

    private static final String BASE_URL = "https://api.opentripmap.com/0.1/en/places";

    @Value("${opentripmap.api-key:}")
    private String apiKey;

    /** Ảnh backfill lưu thẳng vào DB dưới dạng BLOB — phải chặn file quá lớn làm phình DB. */
    private static final int MAX_DOWNLOADED_IMAGE_BYTES = 5 * 1024 * 1024;

    /**
     * Timeout bắt buộc: backfill duyệt toàn bộ bảng và gọi API bên ngoài trong vòng lặp,
     * không có timeout thì một URL treo là giữ luôn thread xử lý request hàng phút.
     */
    private final RestClient restClient = RestClient.builder()
            .requestFactory(createRequestFactory())
            .build();

    private static org.springframework.http.client.ClientHttpRequestFactory createRequestFactory() {
        var factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(java.time.Duration.ofSeconds(5));
        factory.setReadTimeout(java.time.Duration.ofSeconds(15));
        return factory;
    }
    private final ObjectMapper mapper = new ObjectMapper();

    public record GeoPoint(double lon, double lat) {}

    public record PoiCandidate(String xid, String name) {}

    public record PoiDetails(String name, String previewUrl, String description) {}

    // Free tier OpenTripMap giới hạn rất chặt (~10 request/giây) — throttle để backfill
    // hàng loạt (nhiều entity x nhiều call/entity) không bị 429 giữa chừng.
    private static final long MIN_INTERVAL_MS = 400;
    private volatile long lastCallAt = 0;

    private synchronized void throttle() {
        long wait = lastCallAt + MIN_INTERVAL_MS - System.currentTimeMillis();
        if (wait > 0) {
            try {
                Thread.sleep(wait);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        lastCallAt = System.currentTimeMillis();
    }

    private JsonNode getJson(String url, Object... uriVars) {
        throttle();
        String body = restClient.get().uri(url, uriVars).retrieve().body(String.class);
        if (body == null || body.isBlank()) return null;
        try {
            return mapper.readTree(body);
        } catch (Exception e) {
            log.error("Không parse được JSON từ OpenTripMap: {}", url, e);
            return null;
        }
    }

    /** Tra cứu toạ độ từ tên địa danh (dùng cho Tour — không có sẵn lat/lng). */
    public GeoPoint geocode(String placeName) {
        JsonNode response = getJson(BASE_URL + "/geoname?apikey={key}&name={name}", apiKey, placeName);
        if (response == null || !response.has("lon") || !response.has("lat")) {
            return null;
        }
        return new GeoPoint(response.get("lon").asDouble(), response.get("lat").asDouble());
    }

    /**
     * Tìm địa điểm quanh 1 toạ độ khớp tên với nameHint — CHỈ trả về khi có khớp tên
     * thật sự (không đoán bừa điểm gần nhất), tránh gắn nhầm ảnh/mô tả của 1 địa điểm
     * không liên quan (VD gắn ảnh đài phun nước cho 1 nhà hàng cụ thể).
     * kinds: lọc theo loại OpenTripMap (VD "foods" cho nhà hàng), để trống nếu muốn tìm rộng.
     */
    public PoiCandidate findBestMatch(double lon, double lat, String nameHint, int radiusMeters, String kinds) {
        String kindsParam = kinds != null && !kinds.isBlank() ? "&kinds=" + kinds : "";
        JsonNode response = getJson(
                BASE_URL + "/radius?apikey={key}&radius={radius}&limit=30&lon={lon}&lat={lat}&format=json" + kindsParam,
                apiKey, radiusMeters, lon, lat);
        if (response == null || !response.isArray()) {
            return null;
        }
        List<PoiCandidate> candidates = new ArrayList<>();
        for (JsonNode item : response) {
            String xid = item.has("xid") ? item.get("xid").asText() : null;
            String name = item.has("name") ? item.get("name").asText() : null;
            if (xid == null || name == null || name.isBlank()) continue;
            candidates.add(new PoiCandidate(xid, name));
        }
        if (candidates.isEmpty()) return null;

        String normalizedHint = normalize(nameHint);
        return candidates.stream()
                .filter(c -> normalize(c.name()).contains(normalizedHint) || normalizedHint.contains(normalize(c.name())))
                .findFirst()
                .orElse(null);
    }

    /** Lấy chi tiết (ảnh + mô tả) theo xid. */
    public PoiDetails getDetails(String xid) {
        JsonNode response = getJson(BASE_URL + "/xid/{xid}?apikey={key}", xid, apiKey);
        if (response == null) return null;

        String name = response.has("name") ? response.get("name").asText() : null;

        String previewUrl = null;
        if (response.has("preview") && response.get("preview").has("source")) {
            previewUrl = response.get("preview").get("source").asText();
        }

        String description = null;
        if (response.has("wikipedia_extracts") && response.get("wikipedia_extracts").has("text")) {
            description = response.get("wikipedia_extracts").get("text").asText();
        } else if (response.has("info") && response.get("info").has("descr")) {
            description = response.get("info").get("descr").asText();
        }

        return new PoiDetails(name, previewUrl, description);
    }

    public byte[] downloadImage(String url) {
        throttle();
        try {
            byte[] bytes = restClient.get().uri(url).retrieve().body(byte[].class);
            if (bytes != null && bytes.length > MAX_DOWNLOADED_IMAGE_BYTES) {
                log.warn("Bỏ qua ảnh backfill vì vượt {}MB: {}",
                        MAX_DOWNLOADED_IMAGE_BYTES / (1024 * 1024), url);
                return null;
            }
            return bytes;
        } catch (Exception e) {
            log.error("Không tải được ảnh từ OpenTripMap: {}", url, e);
            return null;
        }
    }

    private String normalize(String s) {
        return s == null ? "" : s.toLowerCase().trim();
    }
}
