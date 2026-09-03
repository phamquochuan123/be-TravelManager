package com.example.travelManager.domain.hotel;

import java.time.LocalDate;

import com.example.travelManager.util.constant.hotel.HotelBookingStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class BookedRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long bookingId;

    @Column(name = "check_in_date")
    private LocalDate checkInDate;

    @Column(name = "check_out_date")
    private LocalDate checkOutDate;

    @Column(name = "guest_full_name")
    private String guestFullName;

    @Column(name = "guest_email")
    private String guestEmail;

    @Column(name = "num_of_adults")
    private int numOfAdults;

    @Column(name = "num_of_children")
    private int numOfChildren;

    @Column(name = "total_num_of_guests")
    private int totalNumOfGuests;

    @Column(name = "booking_confirmation_code")
    private String bookingConfirmationCode;

    /**
     * Tổng tiền chốt tại thời điểm đặt (giá phòng × số đêm).
     * Phải lưu lại thay vì tính lại lúc thanh toán: admin sửa giá phòng giữa lúc khách
     * đặt và lúc khách trả tiền thì khách sẽ bị tính theo giá mới.
     * Null với booking cũ tạo trước khi có cột này — chỗ đọc phải có nhánh fallback.
     */
    @Column(name = "total_price", precision = 19, scale = 2)
    private java.math.BigDecimal totalPrice;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private HotelBookingStatus status = HotelBookingStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id")
    private Room room;

    /**
     * Chủ sở hữu thật của booking — người đã đăng nhập lúc đặt.
     * Quyền huỷ / thanh toán xét theo trường này, KHÔNG xét guestEmail
     * (guestEmail do client tự khai nên có thể là email người khác khi đặt hộ).
     * Có thể null với dữ liệu cũ tạo trước khi thêm cột này.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private com.example.travelManager.domain.UserEntity user;

    public void calculateTotalNumOfGuests() {
        this.totalNumOfGuests = this.numOfAdults + this.numOfChildren;
    }

    public void setNumOfAdults(int numOfAdults) {
        this.numOfAdults = numOfAdults;
        calculateTotalNumOfGuests();
    }

    public void setNumOfChildren(int numOfChildren) {
        this.numOfChildren = numOfChildren;
        calculateTotalNumOfGuests();
    }

    public void setBookingConfirmationCode(String bookingConfirmationCode) {
        this.bookingConfirmationCode = bookingConfirmationCode;
    }

}
