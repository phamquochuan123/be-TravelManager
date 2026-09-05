package com.example.travelManager.service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

/**
 * Tra toạ độ từ tên + địa chỉ bằng Nominatim (OpenStreetMap).
 *
 * Vì sao không dùng GooglePlacesService cho việc này: Places API (New) đang bị tắt
 * trên project Google Cloud nên picker thủ công của admin trả 403, và bật lại thì
 * cần billing. Nominatim miễn phí, không cần API key, đủ tốt để ghim bản đồ.
 *
 * Dùng khi admin lưu khách sạn/nhà hàng mà không chọn địa điểm từ picker — không có
 * bước này thì mọi cơ sở thêm mới đều thiếu toạ độ và trang chi tiết không vẽ được
 * bản đồ.
 *
 * Cùng lưu ý Jackson như OpenTripMapService: lấy body dạng String rồi tự parse bằng
 * ObjectMapper của Jackson 2, không .body(JsonNode.class) trực tiếp.
 */
@Slf4j
@Service
public class NominatimGeocodingService {

    private static final String SEARCH_URL = "https://nominatim.openstreetmap.org/search";

    /** Nominatim yêu cầu User-Agent nhận dạng được, thiếu là bị chặn. */
    private static final String USER_AGENT = "TravelManager/1.0 (do an tot nghiep)";

    /** Chính sách Nominatim: tối đa 1 request/giây. */
    private static final long MIN_INTERVAL_MS = 1100;

    /**
     * Kết quả nằm xa tâm thành phố quá ngưỡng này thì coi là trùng tên ở nơi khác.
     * 80km: đủ rộng cho "thành phố" cỡ hòn đảo (Phú Quốc, Côn Đảo, Bali), vẫn đủ
     * chặt để loại kiểu "Novotel Ha Long Bay" ra toạ độ cách Hạ Long 175km.
     */
    private static final double NGUONG_LECH_KM = 80;

    /**
     * Chỉ thử 2 cách gọi tên. Mỗi lần thử tốn hơn 1 giây vì throttle, mà đây nằm
     * ngay trong request lưu form của admin — thử nhiều thành ra treo giao diện.
     */
    private static final int SO_LAN_THU = 2;

    private final RestClient restClient = RestClient.builder()
            .requestFactory(createRequestFactory())
            .build();

    private static org.springframework.http.client.ClientHttpRequestFactory createRequestFactory() {
        var factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(java.time.Duration.ofSeconds(4));
        factory.setReadTimeout(java.time.Duration.ofSeconds(8));
        return factory;
    }

    private final ObjectMapper mapper = new ObjectMapper();

    /** Tâm thành phố đổi rất chậm — cache để không tra lại ở mỗi lần lưu. */
    private final ConcurrentHashMap<String, Optional<ToaDo>> cacheTamThanhPho = new ConcurrentHashMap<>();

    private final Object khoaThrottle = new Object();
    private long lanGoiCuoi = 0;

    public record ToaDo(double lat, double lon) {}

    /**
     * Toạ độ của một cơ sở, hoặc rỗng nếu không tra được / kết quả không đáng tin.
     * Không bao giờ ném exception: thiếu toạ độ chỉ làm mất bản đồ nhúng, không
     * được phép làm hỏng thao tác lưu của admin.
     */
    public Optional<ToaDo> tim(String ten, String diaChi, String thanhPho) {
        if ((ten == null || ten.isBlank()) && (diaChi == null || diaChi.isBlank())) {
            return Optional.empty();
        }
        try {
            String quocGia = suyRaQuocGia(thanhPho);
            Optional<ToaDo> tam = layTamThanhPho(thanhPho, quocGia);

            for (String truyVan : dungTruyVan(ten, diaChi, thanhPho, quocGia)) {
                Optional<ToaDo> hit = goiNominatim(truyVan);
                if (hit.isEmpty()) continue;

                if (tam.isPresent()) {
                    double km = khoangCachKm(tam.get(), hit.get());
                    if (km > NGUONG_LECH_KM) {
                        log.warn("Bo qua toa do cho '{}': cach tam {} {} km", ten, thanhPho, Math.round(km));
                        continue;
                    }
                }
                return hit;
            }
        } catch (Exception e) {
            log.warn("Geocode that bai cho '{}': {}", ten, e.getMessage());
        }
        return Optional.empty();
    }

    private List<String> dungTruyVan(String ten, String diaChi, String thanhPho, String quocGia) {
        String tp = thanhPho == null ? "" : thanhPho.trim();
        String ungVien1 = ghepChuoi(ten, tp, quocGia);
        String ungVien2 = ghepChuoi(diaChi, tp, quocGia);
        return java.util.stream.Stream.of(ungVien1, ungVien2)
                .filter(s -> !s.isBlank())
                .distinct()
                .limit(SO_LAN_THU)
                .toList();
    }

    private String ghepChuoi(String... phan) {
        return java.util.Arrays.stream(phan)
                .filter(s -> s != null && !s.isBlank())
                .map(String::trim)
                .reduce((a, b) -> a + ", " + b)
                .orElse("");
    }

    private Optional<ToaDo> layTamThanhPho(String thanhPho, String quocGia) {
        if (thanhPho == null || thanhPho.isBlank()) return Optional.empty();
        return cacheTamThanhPho.computeIfAbsent(
                thanhPho.toLowerCase(),
                k -> goiNominatim(ghepChuoi(thanhPho, quocGia)));
    }

    private Optional<ToaDo> goiNominatim(String truyVan) {
        if (truyVan == null || truyVan.isBlank()) return Optional.empty();
        choThrottle();
        try {
            String url = SEARCH_URL + "?q=" + URLEncoder.encode(truyVan, StandardCharsets.UTF_8)
                    + "&format=json&limit=1";
            String body = restClient.get()
                    .uri(url)
                    .header("User-Agent", USER_AGENT)
                    .retrieve()
                    .body(String.class);

            if (body == null || body.isBlank()) return Optional.empty();
            JsonNode arr = mapper.readTree(body);
            if (!arr.isArray() || arr.isEmpty()) return Optional.empty();

            JsonNode first = arr.get(0);
            if (!first.hasNonNull("lat") || !first.hasNonNull("lon")) return Optional.empty();
            return Optional.of(new ToaDo(first.get("lat").asDouble(), first.get("lon").asDouble()));
        } catch (Exception e) {
            log.warn("Goi Nominatim loi ('{}'): {}", truyVan, e.getMessage());
            return Optional.empty();
        }
    }

    private void choThrottle() {
        synchronized (khoaThrottle) {
            long doiThem = MIN_INTERVAL_MS - (System.currentTimeMillis() - lanGoiCuoi);
            if (doiThem > 0) {
                try { Thread.sleep(doiThem); }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            }
            lanGoiCuoi = System.currentTimeMillis();
        }
    }

    /** Thành phố nước ngoài trong dữ liệu tour — thiếu tên nước thì Nominatim hay tra nhầm. */
    private String suyRaQuocGia(String thanhPho) {
        if (thanhPho == null) return "Vietnam";
        String tp = thanhPho.toLowerCase();
        if (tp.contains("singapore")) return "Singapore";
        if (tp.contains("bali"))      return "Indonesia";
        if (tp.contains("seoul"))     return "South Korea";
        if (tp.contains("tokyo"))     return "Japan";
        if (tp.contains("bangkok") || tp.contains("pattaya")) return "Thailand";
        return "Vietnam";
    }

    private double khoangCachKm(ToaDo a, ToaDo b) {
        double R = 6371.0;
        double dLat = Math.toRadians(b.lat() - a.lat());
        double dLon = Math.toRadians(b.lon() - a.lon());
        double h = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                 + Math.cos(Math.toRadians(a.lat())) * Math.cos(Math.toRadians(b.lat()))
                 * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return R * 2 * Math.atan2(Math.sqrt(h), Math.sqrt(1 - h));
    }
}
