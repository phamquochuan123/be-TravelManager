package com.example.travelManager.controller;

import com.example.travelManager.domain.Payment;
import com.example.travelManager.repository.PaymentRepository;
import com.example.travelManager.repository.UserRepository;
import com.example.travelManager.service.PaymentIpnService;
import com.example.travelManager.util.SecurityUtil;
import com.example.travelManager.util.VNPayUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
@RequestMapping("/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final VNPayUtil vnPayUtil;
    private final PaymentIpnService paymentIpnService;

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

        Payment payment = new Payment();
        payment.setBookingType(request.getBookingType());
        payment.setBookingId(request.getBookingId());
        payment.setUserEmail(email);
        payment.setAmount(request.getAmount());
        payment.setTxnRef(txnRef);
        payment.setOrderInfo(request.getOrderInfo() != null
                ? request.getOrderInfo()
                : request.getBookingType() + " booking #" + request.getBookingId());
        payment.setStatus(Payment.PaymentStatus.PENDING);
        paymentRepository.save(payment);

        String payUrl = vnPayUtil.createPaymentUrl(
                txnRef,
                request.getAmount().longValue(),
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
            case "99" -> { result.put("RspCode", "01"); result.put("Message", "Order Not Found"); }
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
        List<Payment> all = paymentRepository.findByUserEmail(email);
        // Sort mới nhất trước
        all.sort(Comparator.comparing(Payment::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())));

        return ResponseEntity.ok(toPage(all, page, size, false));
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

        List<Payment> all = paymentRepository.findAllByOrderByCreatedAtDesc();

        Stream<Payment> stream = all.stream();

        if (status != null && !status.isBlank()) {
            stream = stream.filter(p -> p.getStatus() != null && p.getStatus().name().equals(status));
        }
        if (orderType != null && !orderType.isBlank()) {
            // FE gửi "TOUR_BOOKING" → map về "TOUR" mà BE lưu
            String beType = FE_TO_BE_TYPE.getOrDefault(orderType, orderType);
            stream = stream.filter(p -> beType.equals(p.getBookingType()));
        }
        if (search != null && !search.isBlank()) {
            String q = search.toLowerCase();
            stream = stream.filter(p ->
                    (p.getTxnRef()    != null && p.getTxnRef().toLowerCase().contains(q)) ||
                    (p.getUserEmail() != null && p.getUserEmail().toLowerCase().contains(q)));
        }

        List<Payment> filtered = stream.collect(Collectors.toList());
        return ResponseEntity.ok(toPage(filtered, page, size, true));
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private Page<PaymentDto> toPage(List<Payment> list, int page, int size, boolean includeCustomer) {
        int total  = list.size();
        int from   = Math.min(page * size, total);
        int to     = Math.min(from + size, total);
        List<PaymentDto> content = list.subList(from, to).stream()
                .map(p -> toDto(p, includeCustomer ? resolveCustomerName(p.getUserEmail()) : null))
                .collect(Collectors.toList());
        return new PageImpl<>(content, PageRequest.of(page, size), total);
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
