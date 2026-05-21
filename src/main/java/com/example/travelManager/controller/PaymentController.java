package com.example.travelManager.controller;

import com.example.travelManager.domain.Payment;
import com.example.travelManager.repository.PaymentRepository;
import com.example.travelManager.util.SecurityUtil;
import com.example.travelManager.util.VNPayUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentRepository paymentRepository;
    private final VNPayUtil vnPayUtil;

    /**
     * POST /payment/create
     * Tạo URL thanh toán VNPay cho một booking.
     * Body: { bookingType: "TOUR"|"HOTEL"|"RESTAURANT", bookingId: 1, amount: 1500000, orderInfo: "..." }
     */
    @PostMapping("/create")
    public ResponseEntity<CreatePaymentResponse> createPayment(
            @RequestBody CreatePaymentRequest request,
            HttpServletRequest httpRequest) throws Exception {

        String email = SecurityUtil.getCurrentUserLogin().orElse("anonymous");
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
     * GET /payment/ipn — VNPay gọi callback sau khi thanh toán xong (server-to-server).
     * Trả về JSON {"RspCode":"00","Message":"Confirm Success"}
     */
    @GetMapping("/ipn")
    public ResponseEntity<Map<String, String>> ipn(@RequestParam Map<String, String> params) {
        Map<String, String> result = new HashMap<>();
        if (!vnPayUtil.verifySignature(params)) {
            result.put("RspCode", "97");
            result.put("Message", "Invalid signature");
            return ResponseEntity.ok(result);
        }

        String txnRef = params.get("vnp_TxnRef");
        String responseCode = params.get("vnp_ResponseCode");
        String transactionNo = params.get("vnp_TransactionNo");
        String bankCode = params.get("vnp_BankCode");

        paymentRepository.findByTxnRef(txnRef).ifPresent(payment -> {
            if (payment.getStatus() == Payment.PaymentStatus.PENDING) {
                payment.setResponseCode(responseCode);
                payment.setTransactionNo(transactionNo);
                payment.setBankCode(bankCode);
                payment.setStatus("00".equals(responseCode)
                        ? Payment.PaymentStatus.SUCCESS
                        : Payment.PaymentStatus.FAILED);
                paymentRepository.save(payment);
            }
        });

        result.put("RspCode", "00");
        result.put("Message", "Confirm Success");
        return ResponseEntity.ok(result);
    }

    /**
     * GET /payment/result — Frontend redirect sau thanh toán, lấy kết quả.
     */
    @GetMapping("/result")
    public ResponseEntity<PaymentResultResponse> result(@RequestParam Map<String, String> params) {
        boolean valid = vnPayUtil.verifySignature(params);
        String txnRef = params.get("vnp_TxnRef");
        String responseCode = params.get("vnp_ResponseCode");
        boolean success = valid && "00".equals(responseCode);

        PaymentResultResponse res = new PaymentResultResponse();
        res.setTxnRef(txnRef);
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
     * GET /payment/my — Lịch sử thanh toán của user hiện tại.
     */
    @GetMapping("/my")
    public ResponseEntity<List<Payment>> myPayments() {
        String email = SecurityUtil.getCurrentUserLogin().orElse("");
        return ResponseEntity.ok(paymentRepository.findByUserEmail(email));
    }

    /**
     * GET /admin/payments — ADMIN xem tất cả giao dịch.
     */
    @GetMapping("/admin/payments")
    public ResponseEntity<List<Payment>> allPayments() {
        return ResponseEntity.ok(paymentRepository.findAllByOrderByCreatedAtDesc());
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank()) ip = request.getRemoteAddr();
        return ip;
    }

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
        private boolean success;
        private String responseCode;
        private String message;
        private String bookingType;
        private Long bookingId;
        private BigDecimal amount;
    }
}
