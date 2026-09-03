package com.example.travelManager.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.example.travelManager.domain.Payment;
import com.example.travelManager.domain.tour.TourBooking;
import com.example.travelManager.repository.PaymentRepository;
import com.example.travelManager.repository.hotel.BookedRoomRepository;
import com.example.travelManager.repository.restaurant.RestaurantBookingRepository;
import com.example.travelManager.repository.tour.TourBookingRepository;
import com.example.travelManager.util.constant.tour.BookingStatus;

/**
 * Test cho xử lý IPN của VNPay — luồng liên quan trực tiếp tới tiền.
 * Bảo vệ 3 tính chất: đúng số tiền, idempotent (VNPay retry không cộng 2 lần),
 * và không báo thành công khi chưa xác nhận được booking.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PaymentIpnServiceTest {

    private static final String TXN_REF = "TXN123";

    @Mock private PaymentRepository paymentRepository;
    @Mock private TourBookingRepository tourBookingRepository;
    @Mock private BookedRoomRepository bookedRoomRepository;
    @Mock private RestaurantBookingRepository restaurantBookingRepository;

    @InjectMocks private PaymentIpnService service;

    private Payment payment;
    private TourBooking booking;

    @BeforeEach
    void setUp() {
        payment = new Payment();
        payment.setTxnRef(TXN_REF);
        payment.setAmount(BigDecimal.valueOf(1_000_000));
        payment.setBookingType("TOUR");
        payment.setBookingId(55L);
        payment.setStatus(Payment.PaymentStatus.PENDING);

        booking = new TourBooking();
        booking.setStatus(BookingStatus.PENDING);

        when(paymentRepository.findByTxnRef(TXN_REF)).thenReturn(Optional.of(payment));
        when(tourBookingRepository.findById(55L)).thenReturn(Optional.of(booking));
    }

    @Test
    @DisplayName("Sai số tiền thì trả 04 và không đụng tới booking")
    void processIpn_saiSoTien() {
        String code = service.processIpn(TXN_REF, "00", "TN1", "NCB", 999_999L);

        assertThat(code).isEqualTo("04");
        verify(paymentRepository, never()).compareAndSwap(anyString(), any(), any(), any(), any(), any());
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.PENDING);
    }

    @Test
    @DisplayName("Không tìm thấy payment thì trả 98 (Order Not Found), không phải 99")
    void processIpn_khongTimThayPayment() {
        when(paymentRepository.findByTxnRef("KHONG-CO")).thenReturn(Optional.empty());

        assertThat(service.processIpn("KHONG-CO", "00", "TN1", "NCB", 1_000_000L))
                .isEqualTo("98");
    }

    @Test
    @DisplayName("Thanh toán thành công thì xác nhận booking và trả 00")
    void processIpn_thanhCong() {
        when(paymentRepository.compareAndSwap(eq(TXN_REF), eq(Payment.PaymentStatus.SUCCESS),
                any(), any(), any(), any())).thenReturn(1);

        String code = service.processIpn(TXN_REF, "00", "TN1", "NCB", 1_000_000L);

        assertThat(code).isEqualTo("00");
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        verify(tourBookingRepository).save(booking);
    }

    @Test
    @DisplayName("VNPay gọi lại IPN lần 2 thì không xác nhận thêm lần nữa (idempotent)")
    void processIpn_goiLaiLanHai() {
        // compareAndSwap trả 0 = trạng thái đã khác PENDING, tức đã xử lý trước đó
        when(paymentRepository.compareAndSwap(anyString(), any(), any(), any(), any(), any()))
                .thenReturn(0);
        payment.setStatus(Payment.PaymentStatus.SUCCESS);

        String code = service.processIpn(TXN_REF, "00", "TN1", "NCB", 1_000_000L);

        assertThat(code).isEqualTo("01");
        verify(tourBookingRepository, never()).save(any());
    }

    @Test
    @DisplayName("Giao dịch thất bại phía VNPay thì đánh dấu FAILED, không xác nhận booking")
    void processIpn_giaoDichThatBai() {
        when(paymentRepository.compareAndSwap(eq(TXN_REF), eq(Payment.PaymentStatus.FAILED),
                any(), any(), any(), any())).thenReturn(1);

        String code = service.processIpn(TXN_REF, "24", "TN1", "NCB", 1_000_000L);

        assertThat(code).isEqualTo("00");
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.PENDING);
        verify(tourBookingRepository, never()).save(any());
    }
}
