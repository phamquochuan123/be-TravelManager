package com.example.travelManager.controller;

import com.example.travelManager.domain.response.place.PlaceCandidateDto;
import com.example.travelManager.service.GooglePlacesService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Admin dùng để tìm địa điểm trên Google (theo tên) và lấy ảnh gợi ý khi tạo/sửa
 * Hotel/Restaurant/Destination/Tour — thay vì tự gõ địa chỉ + tự tìm ảnh.
 * Khoá Google Places chỉ nằm ở GooglePlacesService (server-side), không lộ ra frontend.
 */
@RestController
@RequestMapping("/admin/places")
@RequiredArgsConstructor
public class AdminPlacesController {

    private final GooglePlacesService googlePlacesService;

    @GetMapping("/search")
    public ResponseEntity<Map<String, List<PlaceCandidateDto>>> search(
            @RequestParam("query") String query) {
        if (query == null || query.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(Map.of("candidates", googlePlacesService.textSearch(query)));
    }

    @GetMapping("/photo")
    public ResponseEntity<byte[]> photo(
            @RequestParam("name") String name,
            @RequestParam(name = "maxWidthPx", defaultValue = "800") int maxWidthPx) {
        if (name == null || name.isBlank() || !name.contains("/photos/")) {
            return ResponseEntity.badRequest().build();
        }
        GooglePlacesService.PhotoBytesResult result = googlePlacesService.fetchPhotoBytes(name, maxWidthPx);
        MediaType contentType;
        try {
            contentType = MediaType.parseMediaType(result.contentType());
        } catch (Exception e) {
            contentType = MediaType.IMAGE_JPEG;
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400")
                .contentType(contentType)
                .body(result.bytes());
    }
}
