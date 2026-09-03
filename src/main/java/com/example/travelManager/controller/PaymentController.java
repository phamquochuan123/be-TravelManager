package com.example.travelManager.controller;

import com.example.travelManager.domain.Payment;
import com.example.travelManager.domain.hotel.BookedRoom;
import com.example.travelManager.domain.restaurant.RestaurantBooking;
import com.example.travelManager.domain.tour.TourBooking;
import com.example.travelManager.exception.ResourceNotFoundException;
import com.example.travelManager.repository.PaymentRepository;
import com.example.travelManager.repository.UserRepository;
import com.example.travelManager.repository.hotel.BookedRoomRepository;
import com.example.travelManager.repository.restaurant.RestaurantBookingRepository;
import com.example.travelManager.repository.tour.TourBookingRepository;
import com.example.travelManager.service.PaymentIpnService;
import com.example.travelManager.util.SecurityUtil;
import com.example.travelManager.util.VNPayUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import com.example.travelManager.service.hotel.BookedRoomServiceImpl;
import java.util.*;

@RestController
@RequestMapping("/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final VNPayUtil vnPayUtil;
    private final PaymentIpnService paymentIpnService;
    private final TourBookingRepository tourBookingRepository;
    private final BookedRoomRepository bookedRoomRepository;
    private final RestaurantBookingRepository restaurantBookingRepository;

    // Nhà hàng chưa có khái niệm giá trong domain model — tiền cọc cố định,
    // phải khớp với DEPOSIT_AMOUNT ở react travel manager/src/pages/restaurants/BookRestaurant.jsx
    private static final BigDecimal RESTAURANT_DEPOSIT_AMOUNT = BigDecimal.valueOf(100_000);

    // FE gửi "TOUR_BOOKING" → BE lưu "TOUR"
    private static final Map<String, String> FE_TO_BE_TYPE = Map.of(
            "TOUR_BOOKING",       "TOUR",
            "HOTEL_BOOKING",      "HOTEL",
            "RESTAURANT_BOOKING", "RESTAURANT"
    );
    // BE lưu "TOUR" → FE hiển thị "TOUR_BOOKING"
    private static final Map<String, String> BE_TO_FE_TYPE = Map.of(
            "TOUR",       "TOUR_BOOKING",
            "HOTEL",      "HOTEL_BOOKING",
            "RESTAURANT", "RESTAURANT_BOOKING"
    );

    /**
     * POST /payment/create
     */
    @PostMapping("/create")
    public ResponseEntity<CreatePaymentResponse> createPayment(
            @RequestBody CreatePaymentRequest request,
            HttpServletRequest httpRequest) throws Exception {

        String email  = SecurityUtil.getCurrentUserLogin().orElse("anonymous");
        String txnRef = UUID.randomUUID().toString().replace("-", "").substring(0, 20);
        String ipAddr = getClientIp(httpRequest);

        // Không tin số tiền client gửi — luôn tự tra lại giá thực từ booking trong DB
        BigDecimal amount = resolveTrustedAmount(request.getBookingType(), request.getBookingId(), email);

        Payment payment = new Payment();
        payment.setBookingType(request.getBookingType());
        payment.setBookingId(request.getBookingId());
        payment.setUserEmail(email);
        payment.setAmount(amount);
        payment.setTxnRef(txnRef);
        payment.setOrderInfo(request.getOrderInfo() != null
                ? request.getOrderInfo()
                : request.getBookingType() + " booking #" + request.getBookingId());
        payment.setStatus(Payment.PaymentStatus.PENDING);
        paymentRepository.save(payment);

        String payUrl = vnPayUtil.createPaymentUrl(
                txnRef,
                amount.longValue(),
                payment.getOrderInfo(),
                ipAddr);

        CreatePaymentResponse res = new CreatePaymentResponse();
        res.setTxnRef(txnRef);
        res.setPayUrl(payUrl);
        return ResponseEntity.ok(res);
    }

    /**
     * GET /payment/ipn — VNPay server-to-server callback
     */
    @GetMapping("/ipn")
    public ResponseEntity<Map<String, String>> ipn(@RequestParam Map<String, String> params) {
        Map<String, String> result = new HashMap<>();
        if (!vnPayUtil.verifySignature(params)) {
            result.put("RspCode", "97"); result.put("Message", "Invalid signature");
            return ResponseEntity.ok(result);
        }

        String txnRef        = params.get("vnp_TxnRef");
        String responseCode  = params.get("vnp_ResponseCode");
        String transactionNo = params.get("vnp_TransactionNo");
        String bankCode      = params.get("vnp_BankCode");

        if (txnRef == null || txnRef.isBlank()) {
            result.put("RspCode", "01"); result.put("Message", "Missing TxnRef");
            return ResponseEntity.ok(result);
        }

        long vnpAmount;
        try {
            vnpAmount = Long.parseLong(params.get("vnp_Amount")) / 100;
        } catch (NumberFormatException e) {
            result.put("RspCode", "04"); result.put("Message", "Invalid Amount");
            return ResponseEntity.ok(result);
        }

        String code = paymentIpnService.processIpn(txnRef, responseCode,
                                                    transactionNo, bankCode, vnpAmount);
        switch (code) {
            case "04" -> { result.put("RspCode", "04"); result.put("Message", "Invalid Amount"); }
            case "01" -> { result.put("RspCode", "02"); result.put("Message", "Order Already Confirmed"); }
            case "98" -> { result.put("RspCode", "01"); result.put("Message", "Order Not Found"); }
            // Xác nhận booking thất bại, giao dịch đã rollback — báo lỗi để VNPay gọi lại IPN.
            case "99" -> { result.put("RspCode", "99"); result.put("Message", "Unknown error"); }
            default   -> { result.put("RspCode", "00"); result.put("Message", "Confirm Success"); }
        }
        return ResponseEntity.ok(result);
    }

    /**
     * GET /payment/result — FE redirect sau thanh toán
     */
    @GetMapping("/result")
    public ResponseEntity<PaymentResultResponse> result(@RequestParam Map<String, String> params) {
        boolean valid       = vnPayUtil.verifySignature(params);
        String txnRef       = params.get("vnp_TxnRef");
        String responseCode = params.get("vnp_ResponseCode");
        boolean success     = valid && "00".equals(responseCode);

        PaymentResultResponse res = new PaymentResultResponse();
        res.setTxnRef(txnRef);
        res.setTransactionCode(txnRef);   // alias cho FE
        res.setSuccess(success);
        res.setResponseCode(responseCode);
        res.setMessage(success ? "Thanh toán thành công" : "Thanh toán thất bại hoặc bị huỷ");
        paymentRepository.findByTxnRef(txnRef).ifPresent(p -> {
            res.setBookingType(p.getBookingType());
            res.setBookingId(p.getBookingId());
            res.setAmount(p.getAmount());
        });
        return ResponseEntity.ok(res);
    }

    /**
     * GET /payment/my?page=0&size=10 — Lịch sử thanh toán của user (paginated)
     */
    @GetMapping("/my")
    public ResponseEntity<Page<PaymentDto>> myPayments(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size) {

        String email = SecurityUtil.getCurrentUserLogin().orElse("");
        // Phân trang + sắp xếp ở DB. Cách cũ load TOÀN BỘ payment của user vào RAM
        // rồi mới sort và cắt trang — không chịu nổi khi dữ liệu lớn dần.
        Page<Payment> pageResult = paymentRepository
                .findByUserEmailOrderByCreatedAtDesc(email, PageRequest.of(page, size));

        return ResponseEntity.ok(pageResult.map(p -> toDto(p, null)));
    }

    /**
     * GET /payment/admin/payments?page=0&size=15&search=...&status=...&orderType=...
     * ADMIN xem tất cả giao dịch (paginated + filter)
     */
    @GetMapping("/admin/payments")
    public ResponseEntity<Page<PaymentDto>> allPayments(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "15") int size,
            @RequestParam(name = "search", required = false)    String search,
            @RequestParam(name = "status", required = false)    String status,
            @RequestParam(name = "orderType", required = false)    String orderType) {

        // Lọc + phân trang ở DB. Cách cũ đọc TOÀN BỘ bảng payment rồi lọc bằng Stream
        // trong bộ nhớ — mỗi lần admin mở trang là quét sạch bảng.
        Payment.PaymentStatus statusFilter = null;
        if (status != null && !status.isBlank()) {
            try {
                statusFilter = Payment.PaymentStatus.valueOf(status);
            } catch (IllegalArgumentException e) {
                // Status lạ do client gửi → coi như không có kết quả nào khớp
                return ResponseEntity.ok(Page.empty(PageRequest.of(page, size)));
            }
        }

        // FE gửi "TOUR_BOOKING" → map về "TOUR" mà BE lưu
        String typeFilter = (orderType != null && !orderType.isBlank())
                ? FE_TO_BE_TYPE.getOrDefault(orderType, orderType)
                : null;

        String searchFilter = (search != null && !search.isBlank()) ? search : null;

        Page<Payment> pageResult = paymentRepository.searchForAdmin(
                statusFilter, typeFilter, searchFilter, PageRequest.of(page, size));

        return ResponseEntity.ok(
                pageResult.map(p -> toDto(p, resolveCustomerName(p.getUserEmail()))));
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    /**
     * Tự tra lại giá thực từ booking trong DB, không tin amount client gửi lên,
     * đồng thời xác minh booking thuộc về user đang gọi API.
     */
    private BigDecimal resolveTrustedAmount(String bookingType, Long bookingId, String currentUserEmail) {
        if (bookingType == null || bookingId == null) {
            throw new IllegalArgumentException("Thiếu thông tin booking để tạo thanh toán");
        }
        String beType = FE_TO_BE_TYPE.getOrDefault(bookingType, bookingType);
        return switch (beType) {
            case "TOUR" -> {
                TourBooking booking = tourBookingRepository.findById(bookingId)
                        .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + bookingId));
                if (booking.getUser() == null || !currentUserEmail.equalsIgnoreCase(booking.getUser().getEmail())) {
                    throw new AccessDeniedException("Bạn không có quyền thanh toán cho đơn đặt tour này");
                }
                yield booking.getFinalPrice();
            }
            case "HOTEL" -> {
                BookedRoom booking = bookedRoomRepository.findById(bookingId)
                        .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + bookingId));
                // Ưu tiên chủ sở hữu thật (user_id); guestEmail chỉ là fallback cho booking cũ
                // vì đó là chuỗi do client tự khai lúc đặt.
                boolean owned = booking.getUser() != null
                        ? currentUserEmail.equalsIgnoreCase(booking.getUser().getEmail())
                        : (booking.getGuestEmail() != null
                                && currentUserEmail.equalsIgnoreCase(booking.getGuestEmail()));
                if (!owned) {
                    throw new AccessDeniedException("Bạn không có quyền thanh toán cho đơn đặt phòng này");
                }
                // Dùng giá đã chốt lúc đặt. Tính lại tại đây sẽ áp giá phòng HIỆN TẠI —
                // admin sửa giá giữa lúc đặt và lúc trả tiền là khách bị tính sai.
                if (booking.getTotalPrice() != null) {
                    yield booking.getTotalPrice();
                }
                // Booking cũ tạo trước khi có cột total_price: đành tính lại như trước.
                BigDecimal fallback = BookedRoomServiceImpl.calculateTotalPrice(
                        booking.getRoom(), booking.getCheckInDate(), booking.getCheckOutDate());
                if (fallback == null) {
                    throw new IllegalStateException("Không xác định được số tiền của đơn đặt phòng này");
                }
                yield fallback;
            }
            case "RESTAURANT" -> {
                RestaurantBooking booking = restaurantBookingRepository.findById(bookingId)
                        .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + bookingId));
                if (booking.getUser() == null || !currentUserEmail.equalsIgnoreCase(booking.getUser().getEmail())) {
                    throw new AccessDeniedException("Bạn không có quyền thanh toán cho đơn đặt bàn này");
                }
                yield RESTAURANT_DEPOSIT_AMOUNT;
            }
            default -> throw new IllegalArgumentException("Loại booking không hợp lệ: " + bookingType);
        };
    }

    private PaymentDto toDto(Payment p, String customerName) {
        PaymentDto dto = new PaymentDto();
        dto.setId(p.getId());
        dto.setTransactionCode(p.getTxnRef());
        dto.setAmount(p.getAmount());
        dto.setStatus(p.getStatus() != null ? p.getStatus().name() : "PENDING");
        dto.setMethod("VNPay");
        dto.setCreatedAt(p.getCreatedAt());
        dto.setOrderType(BE_TO_FE_TYPE.getOrDefault(p.getBookingType(), p.getBookingType()));
        dto.setOrderId(p.getBookingId());
        dto.setOrderDesc(p.getOrderInfo());
        dto.setCustomerEmail(p.getUserEmail());
        dto.setCustomerName(customerName);
        return dto;
    }

    private String resolveCustomerName(String email) {
        if (email == null) return null;
        return userRepository.findByEmail(email)
                .map(u -> u.getName())
                .orElse(null);
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank()) ip = request.getRemoteAddr();
        return ip;
    }

    // ── DTOs ─────────────────────────────────────────────────────────────────

    @Data
    public static class CreatePaymentRequest {
        private String bookingType;
        private Long bookingId;
        private BigDecimal amount;
        private String orderInfo;
    }

    @Data
    public static class CreatePaymentResponse {
        private String txnRef;
        private String payUrl;
    }

    @Data
    public static class PaymentResultResponse {
        private String txnRef;
        private String transactionCode; // alias txnRef cho FE
        private boolean success;
        private String responseCode;
        private String message;
        private String bookingType;
        private Long bookingId;
        private BigDecimal amount;
    }

    @Data
    public static class PaymentDto {
        private Long id;
        private String transactionCode;  // txnRef
        private BigDecimal amount;
        private String status;
        private String method;           // "VNPay"
        private Instant createdAt;
        private String orderType;        // TOUR_BOOKING / HOTEL_BOOKING / RESTAURANT_BOOKING
        private Long orderId;            // bookingId
        private String orderDesc;        // orderInfo
        private String customerName;
        private String customerEmail;    // userEmail
    }
}
