package com.example.travelManager.controller;

import com.example.travelManager.domain.hotel.Hotel;
import com.example.travelManager.domain.restaurant.Restaurant;
import com.example.travelManager.domain.tour.Tour;
import com.example.travelManager.domain.tour.TourImage;
import com.example.travelManager.repository.hotel.HotelRepository;
import com.example.travelManager.repository.restaurant.RestaurantRepository;
import com.example.travelManager.repository.tour.TourImageRepository;
import com.example.travelManager.repository.tour.TourRepository;
import com.example.travelManager.service.OpenTripMapService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.rowset.serial.SerialBlob;

/**
 * Backfill dữ liệu tham khảo (ảnh, mô tả) từ OpenTripMap cho các
 * Hotel/Restaurant/Tour đã có sẵn trong DB nhưng còn thiếu ảnh/mô tả — KHÔNG
 * ghi đè dữ liệu admin đã tự nhập tay, chỉ điền vào chỗ còn trống.
 * Admin bấm nút để chạy thủ công (không tự chạy lúc khởi động server).
 *
 * Dùng OpenTripMap (free, không cần billing) thay vì Google Places cho riêng
 * tính năng backfill này — Google Places (GooglePlacesService) vẫn giữ nguyên
 * cho picker thủ công của admin khi tạo/sửa Hotel/Restaurant/Tour.
 *
 * Lưu ý: OpenTripMap không có "rating" kiểu đánh giá chất lượng như Google
 * (chỉ có "rate" là mức độ nổi bật/quan trọng của địa điểm), nên KHÔNG dùng
 * để điền vào Restaurant.averageRating — chỉ điền ảnh + mô tả.
 */
@Slf4j
@RestController
@RequestMapping("/admin/places/backfill")
@RequiredArgsConstructor
public class AdminPlacesBackfillController {

    private static final int RADIUS_METERS = 2000;
    private static final int TOUR_RADIUS_METERS = 8000;

    private final OpenTripMapService openTripMapService;
    private final HotelRepository hotelRepository;
    private final RestaurantRepository restaurantRepository;
    private final TourRepository tourRepository;
    private final TourImageRepository tourImageRepository;

    @PostMapping("/hotels")
    public ResponseEntity<BackfillResult> backfillHotels() {
        BackfillResult result = new BackfillResult();
        for (Hotel hotel : hotelRepository.findAll()) {
            boolean needsPhoto = hotel.getPhoto() == null;
            boolean needsDescription = hotel.getDescription() == null || hotel.getDescription().isBlank();
            if (!needsPhoto && !needsDescription) {
                result.skipped++;
                continue;
            }
            result.total++;
            try {
                OpenTripMapService.GeoPoint point = resolvePoint(hotel.getLatitude(), hotel.getLongitude(),
                        hotel.getName(), hotel.getCity());
                OpenTripMapService.PoiDetails details = findDetails(point, hotel.getName(), RADIUS_METERS, "accomodations");
                if (details == null) {
                    result.notFound++;
                    continue;
                }
                boolean changed = false;
                if (needsPhoto && details.previewUrl() != null) {
                    changed |= applyPhoto(details.previewUrl(), hotel::setPhoto);
                }
                if (needsDescription && details.description() != null && !details.description().isBlank()) {
                    hotel.setDescription(details.description());
                    changed = true;
                }
                if (changed) {
                    hotelRepository.save(hotel);
                    result.updated++;
                } else {
                    result.notFound++;
                }
            } catch (Exception e) {
                log.error("Backfill OpenTripMap thất bại cho hotel id={}", hotel.getId(), e);
                result.failed++;
            }
        }
        return ResponseEntity.ok(result);
    }

    @PostMapping("/restaurants")
    public ResponseEntity<BackfillResult> backfillRestaurants() {
        BackfillResult result = new BackfillResult();
        for (Restaurant restaurant : restaurantRepository.findAll()) {
            boolean needsPhoto = restaurant.getPhoto() == null;
            boolean needsDescription = restaurant.getDescription() == null || restaurant.getDescription().isBlank();
            if (!needsPhoto && !needsDescription) {
                result.skipped++;
                continue;
            }
            result.total++;
            try {
                OpenTripMapService.GeoPoint point = resolvePoint(restaurant.getLatitude(), restaurant.getLongitude(),
                        restaurant.getName(), restaurant.getCity());
                OpenTripMapService.PoiDetails details = findDetails(point, restaurant.getName(), RADIUS_METERS, "foods");
                if (details == null) {
                    result.notFound++;
                    continue;
                }
                boolean changed = false;
                if (needsPhoto && details.previewUrl() != null) {
                    changed |= applyPhoto(details.previewUrl(), restaurant::setPhoto);
                }
                if (needsDescription && details.description() != null && !details.description().isBlank()) {
                    restaurant.setDescription(details.description());
                    changed = true;
                }
                if (changed) {
                    restaurantRepository.save(restaurant);
                    result.updated++;
                } else {
                    result.notFound++;
                }
            } catch (Exception e) {
                log.error("Backfill OpenTripMap thất bại cho restaurant id={}", restaurant.getId(), e);
                result.failed++;
            }
        }
        return ResponseEntity.ok(result);
    }

    @PostMapping("/tours")
    public ResponseEntity<BackfillResult> backfillTours() {
        BackfillResult result = new BackfillResult();
        for (Tour tour : tourRepository.findAll()) {
            boolean needsPhoto = tour.getImages() == null || tour.getImages().isEmpty();
            boolean needsDescription = tour.getDescription() == null || tour.getDescription().isBlank();
            if (!needsPhoto && !needsDescription) {
                result.skipped++;
                continue;
            }
            result.total++;
            try {
                OpenTripMapService.GeoPoint point = resolvePoint(null, null, tour.getDestination(), tour.getDestination());
                OpenTripMapService.PoiDetails details = findDetails(point, tour.getDestination(), TOUR_RADIUS_METERS, "interesting_places");
                if (details == null) {
                    result.notFound++;
                    continue;
                }
                boolean changed = false;
                if (needsPhoto && details.previewUrl() != null) {
                    byte[] bytes = openTripMapService.downloadImage(details.previewUrl());
                    if (bytes != null && bytes.length > 0) {
                        TourImage image = new TourImage();
                        image.setTour(tour);
                        image.setImageData(new SerialBlob(bytes));
                        image.setSortOrder(0);
                        tourImageRepository.save(image);
                        changed = true;
                    }
                }
                if (needsDescription && details.description() != null && !details.description().isBlank()) {
                    tour.setDescription(details.description());
                    tourRepository.save(tour);
                    changed = true;
                }
                if (changed) {
                    result.updated++;
                } else {
                    result.notFound++;
                }
            } catch (Exception e) {
                log.error("Backfill OpenTripMap thất bại cho tour id={}", tour.getId(), e);
                result.failed++;
            }
        }
        return ResponseEntity.ok(result);
    }

    private OpenTripMapService.GeoPoint resolvePoint(Double lat, Double lng, String name, String fallbackPlace) {
        if (lat != null && lng != null) {
            return new OpenTripMapService.GeoPoint(lng, lat);
        }
        String query = fallbackPlace != null && !fallbackPlace.isBlank() ? fallbackPlace : name;
        return openTripMapService.geocode(query);
    }

    private OpenTripMapService.PoiDetails findDetails(OpenTripMapService.GeoPoint point, String name, int radiusMeters, String kinds) {
        if (point == null) return null;
        OpenTripMapService.PoiCandidate candidate =
                openTripMapService.findBestMatch(point.lon(), point.lat(), name, radiusMeters, kinds);
        if (candidate == null) return null;
        return openTripMapService.getDetails(candidate.xid());
    }

    private boolean applyPhoto(String previewUrl, java.util.function.Consumer<java.sql.Blob> setter) throws Exception {
        byte[] bytes = openTripMapService.downloadImage(previewUrl);
        if (bytes == null || bytes.length == 0) return false;
        setter.accept(new SerialBlob(bytes));
        return true;
    }

    @Data
    public static class BackfillResult {
        private int total;
        private int updated;
        private int skipped;
        private int notFound;
        private int failed;
    }
}
