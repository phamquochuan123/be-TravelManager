package com.example.travelManager.domain.request.tour;

import java.util.List;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TourBookingRequest {

    @NotNull
    private Long departureId;

    @Min(1)
    private int numAdults;

    @Min(0)
    private int numChildren;

    @NotBlank
    private String contactName;

    @NotBlank
    private String contactPhone;

    @NotBlank
    private String contactEmail;

    private String couponCode;

    private String note;

    // Package options
    private Long roomId;         // bắt buộc nếu tour >= 2 đêm

    /**
     * Các bữa ăn kèm theo, rỗng hoặc null nếu khách không chọn nhà hàng nào.
     *
     * Thay cho cặp restaurantId + restaurantBookingTime đơn lẻ trước đây. @Valid để
     * ràng buộc bên trong từng phần tử được kiểm thật — thiếu nó thì @NotNull trong
     * RestaurantAddonRequest bị bỏ qua hoàn toàn và dữ liệu thiếu ngày sẽ lọt xuống
     * tận tầng lưu.
     */
    @jakarta.validation.Valid
    private List<RestaurantAddonRequest> restaurants;
}
