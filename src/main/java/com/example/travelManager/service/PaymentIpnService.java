package com.example.travelManager.service;

import com.example.travelManager.domain.Payment;
import com.example.travelManager.repository.PaymentRepository;
import com.example.travelManager.repository.hotel.BookedRoomRepository;
import com.example.travelManager.repository.restaurant.RestaurantBookingRepository;
import com.example.travelManager.repository.tour.TourBookingRepository;
import com.example.travelManager.util.constant.restaurant.RestaurantBookingStatus;
import com.example.travelManager.util.constant.tour.BookingStatus;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentIpnService {

    private final PaymentRepository paymentRepository;
    private final TourBookingRepository tourBookingRepository;
    private final BookedRoomRepository bookedRoomRepository;
    private final RestaurantBookingRepository restaurantBookingRepository;

    /**
     * Xử lý IPN nguyên tử. Trả về:
     *   "00" = thành công
     *   "01" = đã xử lý rồi (status SUCCESS) — báo VNPay dừng retry
     *   "04" = sai số tiền
     *   "99" = không tìm thấy payment
     */
    @Transactional
    public String processIpn(String txnRef, String responseCode,
                              String transactionNo, String bankCode, long vnpAmount) {
        Payment payment = paymentRepository.findByTxnRef(txnRef).orElse(null);
        if (payment == null) return "99";

        // Amount validation
        if (payment.getAmount().longValue() != vnpAmount) return "04";

        boolean success = "00".equals(responseCode);
        Payment.PaymentStatus newStatus = success
                ? Payment.PaymentStatus.SUCCESS
                : Payment.PaymentStatus.FAILED;

        int affected = paymentRepository.compareAndSwap(
                txnRef, newStatus, responseCode, transactionNo, bankCode,
                Payment.PaymentStatus.PENDING);

        if (affected == 0) {
            Payment current = paymentRepository.findByTxnRef(txnRef).orElse(null);
            if (current != null && current.getStatus() == Payment.PaymentStatus.SUCCESS) {
                return "01";
            }
            log.warn("IPN ignored: txnRef={} status={} responseCode={}",
                     txnRef, current != null ? current.getStatus() : "null", responseCode);
            return "01";
        }

        if (success) {
            try {
                confirmBooking(payment.getBookingType(), payment.getBookingId());
            } catch (Exception e) {
                log.error("confirmBooking failed txnRef={} type={} id={}: {}",
                          txnRef, payment.getBookingType(), payment.getBookingId(), e.getMessage(), e);
            }
        }
        return "00";
    }

    private void confirmBooking(String bookingType, Long bookingId) {
        if (bookingId == null || bookingType == null) return;
        switch (bookingType) {
            case "TOUR" -> tourBookingRepository.findById(bookingId).ifPresent(b -> {
                if (b.getStatus() == BookingStatus.PENDING) {
                    b.setStatus(BookingStatus.CONFIRMED);
                    tourBookingRepository.save(b);
                }
            });
            case "HOTEL" -> bookedRoomRepository.findById(bookingId).ifPresent(b ->
                log.warn("Hotel booking {} confirmed — no status field. Manual review needed.", bookingId)
            );
            case "RESTAURANT" -> restaurantBookingRepository.findById(bookingId).ifPresent(b -> {
                if (b.getStatus() == RestaurantBookingStatus.PENDING) {
                    b.setStatus(RestaurantBookingStatus.CONFIRMED);
                    restaurantBookingRepository.save(b);
                }
            });
        }
    }
}
