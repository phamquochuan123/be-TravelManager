package com.example.travelManager.controller;

import com.example.travelManager.domain.hotel.BookedRoom;
import com.example.travelManager.domain.restaurant.RestaurantBooking;
import com.example.travelManager.domain.tour.TourBooking;
import com.example.travelManager.repository.hotel.BookedRoomRepository;
import com.example.travelManager.repository.restaurant.RestaurantBookingRepository;
import com.example.travelManager.repository.tour.TourBookingRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminRecentOrdersController {

    private final TourBookingRepository tourBookingRepository;
    private final RestaurantBookingRepository restaurantBookingRepository;
    private final BookedRoomRepository bookedRoomRepository;

    @GetMapping("/recent-orders")
    public ResponseEntity<List<RecentOrderItem>> recentOrders() {
        List<RecentOrderItem> items = new ArrayList<>();

        for (TourBooking b : tourBookingRepository.findAllByOrderByCreatedAtDesc()) {
            RecentOrderItem item = new RecentOrderItem();
            item.setId("TOUR-" + b.getId());
            item.setCustomerName(b.getContactName() != null ? b.getContactName() : "");
            item.setServiceType("TOUR");
            item.setServiceName(b.getTour() != null ? b.getTour().getName() : "");
            item.setAmount(b.getFinalPrice() != null ? b.getFinalPrice() : BigDecimal.ZERO);
            item.setStatus(b.getStatus() != null ? b.getStatus().name() : "PENDING");
            item.setCreatedAt(b.getCreatedAt());
            items.add(item);
        }

        for (RestaurantBooking b : restaurantBookingRepository.findAllByOrderByCreatedAtDesc()) {
            RecentOrderItem item = new RecentOrderItem();
            item.setId("REST-" + b.getId());
            item.setCustomerName(b.getContactName() != null ? b.getContactName() : "");
            item.setServiceType("RESTAURANT");
            item.setServiceName(b.getRestaurant() != null ? b.getRestaurant().getName() : "");
            item.setAmount(BigDecimal.ZERO);
            item.setStatus(b.getStatus() != null ? b.getStatus().name() : "PENDING");
            item.setCreatedAt(b.getCreatedAt());
            items.add(item);
        }

        for (BookedRoom b : bookedRoomRepository.findAll()) {
            RecentOrderItem item = new RecentOrderItem();
            item.setId("HOTEL-" + b.getBookingId());
            item.setCustomerName(b.getGuestFullName() != null ? b.getGuestFullName() : "");
            item.setServiceType("HOTEL");
            String hotelName = (b.getRoom() != null && b.getRoom().getHotel() != null)
                    ? b.getRoom().getHotel().getName() : "Phòng khách sạn";
            item.setServiceName(hotelName);
            item.setAmount(BigDecimal.ZERO);
            item.setStatus(b.getStatus() != null ? b.getStatus().name() : "PENDING");
            item.setCreatedAt(null);
            items.add(item);
        }

        items.sort(Comparator.comparing(
                RecentOrderItem::getCreatedAt,
                Comparator.nullsLast(Comparator.reverseOrder())
        ));

        return ResponseEntity.ok(items.stream().limit(10).toList());
    }

    @Data
    public static class RecentOrderItem {
        private String id;
        private String customerName;
        private String serviceType;
        private String serviceName;
        private BigDecimal amount;
        private String status;
        private Instant createdAt;
    }
}
