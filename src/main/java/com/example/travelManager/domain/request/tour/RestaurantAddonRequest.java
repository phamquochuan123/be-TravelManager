package com.example.travelManager.domain.request.tour;

import java.time.LocalDate;
import java.time.LocalTime;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** Một bữa ăn khách chọn kèm tour. */
@Data
public class RestaurantAddonRequest {

    @NotNull
    private Long restaurantId;

    /**
     * Ngày đặt bàn, phải nằm trong thời gian tour.
     *
     * Trước đây trường này không tồn tại: giao diện cho khách chọn ngày, in lại ngày đó
     * trong phần tóm tắt đơn, rồi server lặng lẽ ghi đè bằng ngày khởi hành. Nay khách
     * đặt được nhiều bữa rải nhiều ngày nên ngày phải đi lên thật.
     */
    @NotNull
    private LocalDate bookingDate;

    @NotNull
    private LocalTime bookingTime;
}
