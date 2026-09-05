package com.example.travelManager.util;

import com.example.travelManager.config.VNPayConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Component
@RequiredArgsConstructor
public class VNPayUtil {

    /** VNPay quy ước mọi mốc thời gian trong request theo giờ Việt Nam (GMT+7). */
    private static final ZoneId MUI_GIO_VNPAY = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter DINH_DANG_NGAY =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    /** Khớp với PaymentExpiryScheduler — chỗ tự đánh FAILED payment treo quá 15 phút. */
    private static final int SO_PHUT_CHO_THANH_TOAN = 15;

    private final VNPayConfig config;

    public String createPaymentUrl(String txnRef, long amountVnd,
                                    String orderInfo, String ipAddr) throws Exception {
        String vnpVersion = "2.1.0";
        String vnpCommand = "pay";
        String vnpLocale = "vn";
        String vnpCurrCode = "VND";
        String vnpOrderType = "other";

        // VNPay đối chiếu 2 mốc này với đồng hồ GMT+7 của họ, nên PHẢI quy về
        // giờ Việt Nam. Dùng giờ mặc định của JVM là hỏng: container chạy UTC
        // thì vnp_ExpireDate sinh ra sớm hơn giờ VNPay 7 tiếng, và mọi giao dịch
        // bị từ chối ngay bằng mã 15 "quá thời gian chờ thanh toán" — dù khách
        // vừa bấm xong. Cắm cứng zone thay vì trông vào biến TZ của môi trường.
        ZonedDateTime bayGio = ZonedDateTime.now(MUI_GIO_VNPAY);
        String createDate = bayGio.format(DINH_DANG_NGAY);
        String expireDate = bayGio.plusMinutes(SO_PHUT_CHO_THANH_TOAN).format(DINH_DANG_NGAY);

        Map<String, String> params = new TreeMap<>();
        params.put("vnp_Version", vnpVersion);
        params.put("vnp_Command", vnpCommand);
        params.put("vnp_TmnCode", config.getTmnCode());
        params.put("vnp_Amount", String.valueOf(amountVnd * 100)); // VNPay yêu cầu nhân 100
        params.put("vnp_CurrCode", vnpCurrCode);
        params.put("vnp_TxnRef", txnRef);
        params.put("vnp_OrderInfo", orderInfo);
        params.put("vnp_OrderType", vnpOrderType);
        params.put("vnp_Locale", vnpLocale);
        params.put("vnp_ReturnUrl", config.getReturnUrl());
        params.put("vnp_IpAddr", ipAddr);
        params.put("vnp_CreateDate", createDate);
        params.put("vnp_ExpireDate", expireDate);

        // Build query string để sign
        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            hashData.append(entry.getKey()).append('=')
                    .append(URLEncoder.encode(entry.getValue(), StandardCharsets.US_ASCII));
            query.append(URLEncoder.encode(entry.getKey(), StandardCharsets.US_ASCII))
                    .append('=')
                    .append(URLEncoder.encode(entry.getValue(), StandardCharsets.US_ASCII));
            hashData.append('&');
            query.append('&');
        }
        // Remove trailing '&'
        hashData.deleteCharAt(hashData.length() - 1);
        query.deleteCharAt(query.length() - 1);

        String secureHash = hmacSHA512(config.getHashSecret(), hashData.toString());
        return config.getUrl() + "?" + query + "&vnp_SecureHash=" + secureHash;
    }

    public boolean verifySignature(Map<String, String> params) {
        String receivedHash = params.get("vnp_SecureHash");
        if (receivedHash == null) return false;

        Map<String, String> sorted = new TreeMap<>(params);
        sorted.remove("vnp_SecureHash");
        sorted.remove("vnp_SecureHashType");

        StringBuilder hashData = new StringBuilder();
        for (Map.Entry<String, String> entry : sorted.entrySet()) {
            hashData.append(entry.getKey()).append('=')
                    .append(URLEncoder.encode(entry.getValue(), StandardCharsets.US_ASCII))
                    .append('&');
        }
        if (hashData.length() > 0) hashData.deleteCharAt(hashData.length() - 1);

        String expected = hmacSHA512(config.getHashSecret(), hashData.toString());
        return expected.equalsIgnoreCase(receivedHash);
    }

    private String hmacSHA512(String key, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
            mac.init(secretKey);
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("HMAC-SHA512 error", e);
        }
    }
}
