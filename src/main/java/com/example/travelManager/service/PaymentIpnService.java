package com.example.travelManager.service;

import com.example.travelManager.domain.Payment;
import com.example.travelManager.repository.PaymentRepository;
import com.example.travelManager.repository.hotel.BookedRoomRepository;
import com.example.travelManager.repository.restaurant.RestaurantBookingRepository;
import com.example.travelManager.repository.tour.TourBookingRepository;
import com.example.travelManager.util.constant.hotel.HotelBookingStatus;
import com.example.travelManager.util.constant.restaurant.RestaurantBookingStatus;
import com.example.travelManager.util.constant.tour.BookingStatus;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

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
     *   "99" = xác nhận booking thất bại — đã rollback, cần VNPay gọi lại
     *   "98" = không tìm thấy payment
     */
    @Transactional
    public String processIpn(String txnRef, String responseCode,
                              String transactionNo, String bankCode, long vnpAmount) {
        Payment payment = paymentRepository.findByTxnRef(txnRef).orElse(null);
        if (payment == null) return "98";

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
                // Trước đây chỗ này chỉ log rồi vẫn trả "00" (= Confirm Success) cho VNPay.
                // Hậu quả: khách đã mất tiền nhưng booking kẹt PENDING, và vì báo thành công
                // nên VNPay không bao giờ gọi lại IPN → không có cơ hội tự phục hồi.
                // Giờ rollback transaction (payment quay lại PENDING) và trả "99" để VNPay retry.
                log.error("confirmBooking failed txnRef={} type={} id={}: {}",
                          txnRef, payment.getBookingType(), payment.getBookingId(), e.getMessage(), e);
                TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
                return "99";
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
                // Phòng và bàn đặt kèm trong gói tour cũng được xác nhận theo, vì chúng
                // được tạo ở trạng thái PENDING và thanh toán chung một giao dịch với tour.
                if (b.getBookedRoom() != null
                        && b.getBookedRoom().getStatus() == HotelBookingStatus.PENDING) {
                    b.getBookedRoom().setStatus(HotelBookingStatus.CONFIRMED);
                    bookedRoomRepository.save(b.getBookedRoom());
                }
                // Xác nhận MỌI bữa của đơn. Bản cũ chỉ xác nhận một bữa; nay đơn kèm
                // nhiều bữa nên bỏ sót là khách trả tiền rồi mà bàn vẫn treo PENDING.
                for (var bua : b.getRestaurantBookings()) {
                    if (bua.getStatus() == RestaurantBookingStatus.PENDING) {
                        bua.setStatus(RestaurantBookingStatus.CONFIRMED);
                        restaurantBookingRepository.save(bua);
                    }
                }
            });
            case "HOTEL" -> bookedRoomRepository.findById(bookingId).ifPresent(b -> {
                if (b.getStatus() == HotelBookingStatus.PENDING) {
                    b.setStatus(HotelBookingStatus.CONFIRMED);
                    bookedRoomRepository.save(b);
                }
            });
            case "RESTAURANT" -> restaurantBookingRepository.findById(bookingId).ifPresent(b -> {
                if (b.getStatus() == RestaurantBookingStatus.PENDING) {
                    b.setStatus(RestaurantBookingStatus.CONFIRMED);
                    restaurantBookingRepository.save(b);
                }
            });
        }
    }
}
