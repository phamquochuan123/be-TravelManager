package com.example.travelManager.controller;

import com.example.travelManager.domain.hotel.BookedRoom;
import com.example.travelManager.domain.restaurant.RestaurantBooking;
import com.example.travelManager.domain.tour.TourBooking;
import com.example.travelManager.exception.ResourceNotFoundException;
import com.example.travelManager.repository.hotel.BookedRoomRepository;
import com.example.travelManager.repository.restaurant.RestaurantBookingRepository;
import com.example.travelManager.repository.tour.TourBookingRepository;
import com.example.travelManager.util.constant.hotel.HotelBookingStatus;
import com.example.travelManager.util.constant.restaurant.RestaurantBookingStatus;
import com.example.travelManager.util.constant.tour.BookingStatus;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin/orders")
@RequiredArgsConstructor
public class AdminOrderController {

    private final TourBookingRepository tourBookingRepository;
    private final RestaurantBookingRepository restaurantBookingRepository;
    private final BookedRoomRepository hotelBookingRepository;

    @GetMapping("/counts")
    public ResponseEntity<Map<String, Object>> counts(
            @RequestParam(name = "serviceType", required = false) String serviceType) {
        List<OrderItem> all = buildAll().stream()
                .filter(o -> serviceType == null || serviceType.equalsIgnoreCase(o.getServiceType()))
                .collect(Collectors.toList());
        Map<String, Long> byStatus = all.stream()
                .collect(Collectors.groupingBy(o -> o.getStatus(), Collectors.counting()));

        Map<String, Object> result = new HashMap<>();
        result.put("PENDING",   byStatus.getOrDefault("PENDING", 0L));
        result.put("CONFIRMED", byStatus.getOrDefault("CONFIRMED", 0L));
        result.put("COMPLETED", byStatus.getOrDefault("COMPLETED", 0L));
        result.put("CANCELLED", byStatus.getOrDefault("CANCELLED", 0L));
        result.put("total", (long) all.size());
        return ResponseEntity.ok(result);
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> list(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "serviceType", required = false) String serviceType,
            @RequestParam(name = "from", required = false) String from,
            @RequestParam(name = "to", required = false) String to) {

        Instant fromInstant = from != null ? LocalDate.parse(from).atStartOfDay(ZoneId.systemDefault()).toInstant() : null;
        Instant toInstant   = to   != null ? LocalDate.parse(to).plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant() : null;

        List<OrderItem> all = buildAll().stream()
                .filter(o -> status == null || status.equalsIgnoreCase(o.getStatus()))
                .filter(o -> serviceType == null || serviceType.equalsIgnoreCase(o.getServiceType()))
                .filter(o -> search == null || search.isBlank()
                        || o.getCustomerName().toLowerCase().contains(search.toLowerCase())
                        || o.getCustomerEmail().toLowerCase().contains(search.toLowerCase())
                        || o.getServiceName().toLowerCase().contains(search.toLowerCase())
                        || o.getId().toLowerCase().contains(search.toLowerCase()))
                .filter(o -> fromInstant == null || (o.getCreatedAtInstant() != null && !o.getCreatedAtInstant().isBefore(fromInstant)))
                .filter(o -> toInstant == null   || (o.getCreatedAtInstant() != null && o.getCreatedAtInstant().isBefore(toInstant)))
                .sorted((a, b) -> {
                    Instant ia = a.getCreatedAtInstant() != null ? a.getCreatedAtInstant() : Instant.EPOCH;
                    Instant ib = b.getCreatedAtInstant() != null ? b.getCreatedAtInstant() : Instant.EPOCH;
                    return ib.compareTo(ia);
                })
                .collect(Collectors.toList());

        int total = all.size();
        int fromIdx = Math.min(page * size, total);
        int toIdx = Math.min(fromIdx + size, total);

        Map<String, Object> result = new HashMap<>();
        result.put("content", all.subList(fromIdx, toIdx));
        result.put("totalPages", (int) Math.ceil((double) total / size));
        result.put("totalElements", total);
        return ResponseEntity.ok(result);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(
            @PathVariable("id") String id,
            @RequestBody Map<String, String> body) {

        String newStatus = body.get("status");
        String note = body.get("note");

        if (id.startsWith("TOUR-")) {
            long bookingId = Long.parseLong(id.substring(5));
            TourBooking b = tourBookingRepository.findById(bookingId)
                    .orElseThrow(() -> new ResourceNotFoundException("Tour booking not found"));
            b.setStatus(BookingStatus.valueOf(newStatus));
            b.setAdminNote(note);
            tourBookingRepository.save(b);
            return ResponseEntity.ok(toTourItem(b));
        } else if (id.startsWith("REST-")) {
            long bookingId = Long.parseLong(id.substring(5));
            RestaurantBooking b = restaurantBookingRepository.findById(bookingId)
                    .orElseThrow(() -> new ResourceNotFoundException("Restaurant booking not found"));
            b.setStatus(RestaurantBookingStatus.valueOf(newStatus));
            b.setAdminNote(note);
            restaurantBookingRepository.save(b);
            return ResponseEntity.ok(toRestItem(b));
        } else if (id.startsWith("HOTEL-")) {
            long bookingId = Long.parseLong(id.substring(6));
            BookedRoom b = hotelBookingRepository.findById(bookingId)
                    .orElseThrow(() -> new ResourceNotFoundException("Hotel booking not found"));
            b.setStatus(HotelBookingStatus.valueOf(newStatus));
            hotelBookingRepository.save(b);
            return ResponseEntity.ok(toHotelItem(b));
        } else {
            throw new ResourceNotFoundException("Order not found: " + id);
        }
    }

    private List<OrderItem> buildAll() {
        List<OrderItem> items = new ArrayList<>();
        tourBookingRepository.findAllByOrderByCreatedAtDesc().forEach(b -> items.add(toTourItem(b)));
        restaurantBookingRepository.findAllByOrderByCreatedAtDesc().forEach(b -> items.add(toRestItem(b)));
        hotelBookingRepository.findAll().forEach(b -> items.add(toHotelItem(b)));
        return items;
    }

    private OrderItem toTourItem(TourBooking b) {
        OrderItem o = new OrderItem();
        o.setId("TOUR-" + b.getId());
        o.setServiceType("TOUR");
        o.setServiceName(b.getTour() != null ? b.getTour().getName() : "");
        o.setCustomerName(b.getContactName() != null ? b.getContactName() : "");
        o.setCustomerEmail(b.getContactEmail() != null ? b.getContactEmail() : "");
        o.setCustomerPhone(b.getContactPhone());
        BigDecimal computedTotal = b.getFinalPrice() != null ? b.getFinalPrice()
                : (b.getOriginalPrice() != null ? b.getOriginalPrice() : BigDecimal.ZERO)
                .add(b.getPackageHotelPrice() != null ? b.getPackageHotelPrice() : BigDecimal.ZERO)
                .subtract(b.getDiscountAmount() != null ? b.getDiscountAmount() : BigDecimal.ZERO);
        o.setTotalAmount(computedTotal);
        o.setStatus(b.getStatus() != null ? b.getStatus().name() : "PENDING");
        o.setCreatedAt(b.getCreatedAt() != null ? b.getCreatedAt().toString() : "");
        o.setCreatedAtInstant(b.getCreatedAt());
        if (b.getDeparture() != null && b.getDeparture().getDepartureDate() != null) {
            o.setUseDate(b.getDeparture().getDepartureDate().toString());
        } else {
            String ca = o.getCreatedAt();
            o.setUseDate(ca.length() >= 10 ? ca.substring(0, 10) : ca);
        }

        // Hotel booking info
        if (b.getBookedRoom() != null) {
            BookedRoom br = b.getBookedRoom();
            if (br.getRoom() != null && br.getRoom().getHotel() != null) {
                o.setHotelName(br.getRoom().getHotel().getName());
                o.setRoomType(br.getRoom().getRoomType());
            }
            o.setCheckInDate(br.getCheckInDate() != null ? br.getCheckInDate().toString() : null);
            o.setCheckOutDate(br.getCheckOutDate() != null ? br.getCheckOutDate().toString() : null);
        }

        // Restaurant booking info
        if (b.getRestaurantBooking() != null) {
            RestaurantBooking rb = b.getRestaurantBooking();
            if (rb.getRestaurant() != null) {
                o.setRestaurantName(rb.getRestaurant().getName());
            }
            o.setRestaurantBookingDate(b.getRestaurantBooking().getBookingDate() != null
                    ? b.getRestaurantBooking().getBookingDate().toString() : null);
        }

        o.setPackageHotelPrice(b.getPackageHotelPrice());
        o.setPackageRestaurantPrice(b.getPackageRestaurantPrice());
        o.setPackageDiscountPercent(b.getTour() != null ? b.getTour().getPackageDiscountPercent() : null);

        // Price breakdown
        List<Map<String, Object>> breakdown = new ArrayList<>();
        if (b.getOriginalPrice() != null) breakdown.add(Map.of("label", "Giá tour", "amount", b.getOriginalPrice()));
        if (b.getPackageHotelPrice() != null && b.getPackageHotelPrice().compareTo(BigDecimal.ZERO) > 0)
            breakdown.add(Map.of("label", "Khách sạn", "amount", b.getPackageHotelPrice()));
        // Restaurant is paid on-site — shown separately in the UI, not included in payment total
        if (b.getDiscountAmount() != null && b.getDiscountAmount().compareTo(BigDecimal.ZERO) > 0)
            breakdown.add(Map.of("label", "Giảm giá", "amount", b.getDiscountAmount().negate()));
        o.setBreakdown(breakdown);

        if (b.getAdminNote() != null) {
            Instant changedAt = b.getUpdatedAt() != null ? b.getUpdatedAt() : b.getCreatedAt();
            Map<String, Object> entry = new HashMap<>();
            entry.put("status", b.getStatus() != null ? b.getStatus().name() : "PENDING");
            entry.put("note", b.getAdminNote());
            entry.put("changedAt", changedAt != null ? changedAt.toString() : "");
            o.setStatusHistory(List.of(entry));
        }
        return o;
    }

    private OrderItem toRestItem(RestaurantBooking b) {
        OrderItem o = new OrderItem();
        o.setId("REST-" + b.getId());
        o.setServiceType("RESTAURANT");
        o.setServiceName(b.getRestaurant() != null ? b.getRestaurant().getName() : "");
        o.setCustomerName(b.getContactName() != null ? b.getContactName() : "");
        o.setCustomerEmail(b.getContactEmail() != null ? b.getContactEmail() : "");
        o.setCustomerPhone(b.getContactPhone());
        o.setTotalAmount(BigDecimal.ZERO);
        o.setStatus(b.getStatus() != null ? b.getStatus().name() : "PENDING");
        o.setCreatedAt(b.getCreatedAt() != null ? b.getCreatedAt().toString() : "");
        o.setCreatedAtInstant(b.getCreatedAt());
        o.setUseDate(b.getBookingDate() != null ? b.getBookingDate().toString() : "");
        if (b.getAdminNote() != null) {
            Instant changedAt = b.getUpdatedAt() != null ? b.getUpdatedAt() : b.getCreatedAt();
            Map<String, Object> entry = new HashMap<>();
            entry.put("status", b.getStatus() != null ? b.getStatus().name() : "PENDING");
            entry.put("note", b.getAdminNote());
            entry.put("changedAt", changedAt != null ? changedAt.toString() : "");
            o.setStatusHistory(List.of(entry));
        }
        return o;
    }

    private OrderItem toHotelItem(BookedRoom b) {
        OrderItem o = new OrderItem();
        o.setId("HOTEL-" + b.getBookingId());
        o.setServiceType("HOTEL");
        String hotelName = (b.getRoom() != null && b.getRoom().getHotel() != null)
                ? b.getRoom().getHotel().getName() : "Phòng khách sạn";
        o.setServiceName(hotelName);
        o.setCustomerName(b.getGuestFullName() != null ? b.getGuestFullName() : "");
        o.setCustomerEmail(b.getGuestEmail() != null ? b.getGuestEmail() : "");
        o.setTotalAmount(BigDecimal.ZERO);
        o.setStatus(b.getStatus() != null ? b.getStatus().name() : "PENDING");
        o.setUseDate(b.getCheckInDate() != null ? b.getCheckInDate().toString() : "");
        o.setCreatedAt("");
        o.setCreatedAtInstant(null);
        return o;
    }

    @Data
    public static class OrderItem {
        private String id;
        private String customerName;
        private String customerEmail;
        private String customerAvatar;
        private String customerPhone;
        private String serviceType;
        private String serviceName;
        private String serviceImage;
        private String useDate;
        private BigDecimal totalAmount;
        private String status;
        private String createdAt;
        private Instant createdAtInstant;
        private List<Map<String, Object>> breakdown;
        private List<Map<String, Object>> statusHistory;
        // Tour package — hotel info
        private String hotelName;
        private String roomType;
        private String checkInDate;
        private String checkOutDate;
        private BigDecimal packageHotelPrice;
        // Tour package — restaurant info
        private String restaurantName;
        private String restaurantBookingDate;
        private BigDecimal packageRestaurantPrice;
        // Tour package — discount
        private Double packageDiscountPercent;
    }
}

