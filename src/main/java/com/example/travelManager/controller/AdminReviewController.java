package com.example.travelManager.controller;

import com.example.travelManager.domain.hotel.HotelReview;
import com.example.travelManager.domain.restaurant.RestaurantReview;
import com.example.travelManager.domain.tour.TourReview;
import com.example.travelManager.exception.ResourceNotFoundException;
import com.example.travelManager.repository.hotel.HotelReviewRepository;
import com.example.travelManager.repository.restaurant.RestaurantReviewRepository;
import com.example.travelManager.repository.tour.TourReviewRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin/reviews")
@RequiredArgsConstructor
public class AdminReviewController {

    private final TourReviewRepository tourReviewRepository;
    private final HotelReviewRepository hotelReviewRepository;
    private final RestaurantReviewRepository restaurantReviewRepository;

    @GetMapping
    public ResponseEntity<Map<String, Object>> list(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "serviceType", required = false) String serviceType,
            @RequestParam(name = "rating", required = false) String rating,
            @RequestParam(name = "visible", required = false) String visible) {

        List<ReviewItem> all = buildAll();

        all = all.stream()
                .filter(r -> serviceType == null || "ALL".equals(serviceType) || serviceType.equalsIgnoreCase(r.getServiceType()))
                .filter(r -> rating == null || "ALL".equals(rating) || r.getRating() == Integer.parseInt(rating))
                .filter(r -> visible == null || "ALL".equals(visible) || r.isVisible() == Boolean.parseBoolean(visible))
                .filter(r -> search == null || search.isBlank()
                        || r.getAuthorName().toLowerCase().contains(search.toLowerCase())
                        || r.getServiceName().toLowerCase().contains(search.toLowerCase())
                        || r.getComment().toLowerCase().contains(search.toLowerCase()))
                .sorted((a, b) -> {
                    Instant ia = a.getCreatedAtInstant() != null ? a.getCreatedAtInstant() : Instant.EPOCH;
                    Instant ib = b.getCreatedAtInstant() != null ? b.getCreatedAtInstant() : Instant.EPOCH;
                    return ib.compareTo(ia);
                })
                .collect(Collectors.toList());

        int total = all.size();
        int fromIdx = Math.min(page * size, total);
        int toIdx   = Math.min(fromIdx + size, total);

        Map<String, Object> result = new HashMap<>();
        result.put("content", all.subList(fromIdx, toIdx));
        result.put("totalPages", (int) Math.ceil((double) total / size));
        result.put("totalElements", total);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{id}/reply")
    public ResponseEntity<Map<String, String>> reply(
            @PathVariable("id") String id,
            @RequestBody Map<String, String> body) {
        String reply = body.getOrDefault("reply", "");
        if (id.startsWith("TOUR-")) {
            long rid = Long.parseLong(id.substring(5));
            TourReview r = tourReviewRepository.findById(rid)
                    .orElseThrow(() -> new ResourceNotFoundException("Review not found"));
            r.setAdminReply(reply);
            tourReviewRepository.save(r);
        } else if (id.startsWith("HOTEL-")) {
            long rid = Long.parseLong(id.substring(6));
            HotelReview r = hotelReviewRepository.findById(rid)
                    .orElseThrow(() -> new ResourceNotFoundException("Review not found"));
            r.setAdminReply(reply);
            hotelReviewRepository.save(r);
        } else if (id.startsWith("REST-")) {
            long rid = Long.parseLong(id.substring(5));
            RestaurantReview r = restaurantReviewRepository.findById(rid)
                    .orElseThrow(() -> new ResourceNotFoundException("Review not found"));
            r.setAdminReply(reply);
            restaurantReviewRepository.save(r);
        }
        return ResponseEntity.ok(Map.of("reply", reply));
    }

    @PatchMapping("/{id}/visibility")
    public ResponseEntity<Map<String, Object>> toggleVisibility(
            @PathVariable("id") String id,
            @RequestBody Map<String, Object> body) {
        boolean visible = Boolean.parseBoolean(body.getOrDefault("visible", "true").toString());
        if (id.startsWith("TOUR-")) {
            long rid = Long.parseLong(id.substring(5));
            TourReview r = tourReviewRepository.findById(rid)
                    .orElseThrow(() -> new ResourceNotFoundException("Review not found"));
            r.setHidden(!visible);
            tourReviewRepository.save(r);
        } else if (id.startsWith("HOTEL-")) {
            long rid = Long.parseLong(id.substring(6));
            HotelReview r = hotelReviewRepository.findById(rid)
                    .orElseThrow(() -> new ResourceNotFoundException("Review not found"));
            r.setHidden(!visible);
            hotelReviewRepository.save(r);
        } else if (id.startsWith("REST-")) {
            long rid = Long.parseLong(id.substring(5));
            RestaurantReview r = restaurantReviewRepository.findById(rid)
                    .orElseThrow(() -> new ResourceNotFoundException("Review not found"));
            r.setHidden(!visible);
            restaurantReviewRepository.save(r);
        }
        return ResponseEntity.ok(Map.of("visible", visible));
    }

    private List<ReviewItem> buildAll() {
        List<ReviewItem> items = new ArrayList<>();

        for (TourReview r : tourReviewRepository.findAll()) {
            ReviewItem item = new ReviewItem();
            item.setId("TOUR-" + r.getId());
            item.setServiceType("TOUR");
            item.setServiceName(r.getTour() != null ? r.getTour().getName() : "");
            item.setAuthorName(r.getUser() != null ? r.getUser().getName() : "Ẩn danh");
            if (r.getUser() != null && r.getUser().getAvatar() != null) {
                item.setAuthorAvatar("data:image/jpeg;base64," +
                        Base64.getEncoder().encodeToString(r.getUser().getAvatar()));
            }
            item.setRating(r.getRating());
            item.setComment(r.getComment() != null ? r.getComment() : "");
            item.setAdminReply(r.getAdminReply());
            item.setVisible(!r.isHidden());
            item.setCreatedAt(r.getCreatedAt() != null ? r.getCreatedAt().toString() : "");
            item.setCreatedAtInstant(r.getCreatedAt());
            items.add(item);
        }

        for (HotelReview r : hotelReviewRepository.findAll()) {
            ReviewItem item = new ReviewItem();
            item.setId("HOTEL-" + r.getId());
            item.setServiceType("HOTEL");
            item.setServiceName(r.getHotel() != null ? r.getHotel().getName() : "");
            item.setAuthorName(r.getUser() != null ? r.getUser().getName() : "Ẩn danh");
            if (r.getUser() != null && r.getUser().getAvatar() != null) {
                item.setAuthorAvatar("data:image/jpeg;base64," +
                        Base64.getEncoder().encodeToString(r.getUser().getAvatar()));
            }
            item.setRating(r.getRating());
            item.setComment(r.getComment() != null ? r.getComment() : "");
            item.setAdminReply(r.getAdminReply());
            item.setVisible(!r.isHidden());
            item.setCreatedAt(r.getCreatedAt() != null ? r.getCreatedAt().toString() : "");
            item.setCreatedAtInstant(r.getCreatedAt());
            items.add(item);
        }

        for (RestaurantReview r : restaurantReviewRepository.findAll()) {
            ReviewItem item = new ReviewItem();
            item.setId("REST-" + r.getId());
            item.setServiceType("RESTAURANT");
            item.setServiceName(r.getRestaurant() != null ? r.getRestaurant().getName() : "");
            item.setAuthorName(r.getUser() != null ? r.getUser().getName() : "Ẩn danh");
            if (r.getUser() != null && r.getUser().getAvatar() != null) {
                item.setAuthorAvatar("data:image/jpeg;base64," +
                        Base64.getEncoder().encodeToString(r.getUser().getAvatar()));
            }
            item.setRating(r.getRating());
            item.setComment(r.getComment() != null ? r.getComment() : "");
            item.setAdminReply(r.getAdminReply());
            item.setVisible(!r.isHidden());
            item.setCreatedAt(r.getCreatedAt() != null ? r.getCreatedAt().toString() : "");
            item.setCreatedAtInstant(r.getCreatedAt());
            items.add(item);
        }
        return items;
    }

    @Data
    public static class ReviewItem {
        private String id;
        private String authorName;
        private String authorAvatar;
        private String serviceType;
        private String serviceName;
        private int rating;
        private String comment;
        private List<String> images;
        private String adminReply;
        private boolean visible;
        private String createdAt;
        private Instant createdAtInstant;
    }
}

