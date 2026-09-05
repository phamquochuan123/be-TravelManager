package com.example.travelManager.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String mailFrom;

    @Value("${app.admin.email:admin@travelmanager.com}")
    private String adminEmail;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /** Tên hiển thị của người gửi. Mail không có tên người gửi rất dễ bị Gmail xếp vào Spam. */
    private static final String SENDER_NAME = "Travel Manager";

    /**
     * Gửi mail có tên người gửi và nội dung HTML.
     *
     * Vì sao không dùng SimpleMailMessage nữa: nó chỉ đặt được địa chỉ trần, không đặt được
     * tên hiển thị. Mail chỉ có mỗi dòng "Your OTP is 123456" gửi từ một địa chỉ không tên
     * là đúng khuôn mẫu spam — Gmail lọc thẳng vào Spam của người nhận, nên người dùng
     * đăng ký xong ngồi chờ mã mãi không thấy.
     */
    private void sendHtml(String toEmail, String subject, String htmlBody, String plainFallback) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(new InternetAddress(mailFrom, SENDER_NAME, "UTF-8"));
            helper.setReplyTo(mailFrom);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(plainFallback, htmlBody);
            mailSender.send(message);
        } catch (MessagingException | java.io.UnsupportedEncodingException e) {
            // Bọc lại để phía gọi (ProfileServiceImpl) xử lý như trước
            throw new IllegalStateException("Không gửi được email tới " + toEmail, e);
        }
    }

    /** Khung HTML chung: tiếng Việt, có ngữ cảnh rõ ràng, không dùng chữ hoa/giục gấp. */
    private String otpHtml(String tieuDe, String moTa, String otp, String hanSuDung) {
        return """
                <div style="font-family:Arial,Helvetica,sans-serif;max-width:520px;margin:0 auto;color:#222">
                  <h2 style="color:#0a1628;margin-bottom:4px">%s</h2>
                  <p style="color:#555;line-height:1.6">%s</p>
                  <div style="background:#f4f7fb;border:1px solid #dbe4f0;border-radius:6px;
                              padding:18px;text-align:center;margin:20px 0">
                    <div style="font-size:30px;letter-spacing:6px;font-weight:bold;color:#0a1628">%s</div>
                  </div>
                  <p style="color:#555;line-height:1.6">Mã có hiệu lực trong %s.</p>
                  <p style="color:#888;font-size:13px;line-height:1.6">
                    Nếu bạn không thực hiện yêu cầu này, hãy bỏ qua email.
                  </p>
                  <hr style="border:none;border-top:1px solid #eee;margin:22px 0">
                  <p style="color:#999;font-size:12px">Travel Manager — hệ thống đặt tour, khách sạn và nhà hàng.</p>
                </div>
                """.formatted(tieuDe, moTa, otp, hanSuDung);
    }

    /**
     * Không để @Async ảnh hưởng luồng đăng ký thì đăng ký mất ~8s vì phải chờ 2 lần gửi mail.
     * Mail chào mừng không quan trọng bằng OTP nên cho chạy nền.
     */
    @Async
    public void sendWelcomeEmail(String toEmail, String name) {
        String html = """
                <div style="font-family:Arial,Helvetica,sans-serif;max-width:520px;margin:0 auto;color:#222">
                  <h2 style="color:#0a1628">Chào %s,</h2>
                  <p style="color:#555;line-height:1.6">
                    Tài khoản Travel Manager của bạn đã được tạo. Bạn có thể tìm và đặt tour,
                    phòng khách sạn và bàn nhà hàng ngay bây giờ.
                  </p>
                  <hr style="border:none;border-top:1px solid #eee;margin:22px 0">
                  <p style="color:#999;font-size:12px">Travel Manager — hệ thống đặt tour, khách sạn và nhà hàng.</p>
                </div>
                """.formatted(name);
        sendHtml(toEmail, "Chào mừng bạn đến với Travel Manager",
                html, "Chào " + name + ", tài khoản Travel Manager của bạn đã được tạo.");
    }

    public void sendResetOtpEmail(String toEmail, String otp) {
        sendHtml(toEmail,
                "Mã đặt lại mật khẩu Travel Manager",
                otpHtml("Đặt lại mật khẩu",
                        "Bạn vừa yêu cầu đặt lại mật khẩu cho tài khoản Travel Manager. "
                                + "Nhập mã bên dưới để tiếp tục.",
                        otp, "15 phút"),
                "Ma dat lai mat khau Travel Manager cua ban la " + otp + ", co hieu luc trong 15 phut.");
    }

    public void sendOtpEmail(String toEmail, String otp) {
        sendHtml(toEmail,
                "Mã xác thực tài khoản Travel Manager",
                otpHtml("Xác thực địa chỉ email",
                        "Cảm ơn bạn đã đăng ký Travel Manager. Nhập mã bên dưới để xác thực email.",
                        otp, "24 giờ"),
                "Ma xac thuc tai khoan Travel Manager cua ban la " + otp + ", co hieu luc trong 24 gio.");
    }

    @Async
    public void sendHotelBookingConfirmation(String toEmail, String guestName,
            String hotelName, String roomNumber,
            String checkIn, String checkOut, String confirmationCode) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailFrom);
        message.setTo(toEmail);
        message.setSubject("Xác nhận đặt phòng - " + confirmationCode);
        message.setText(
                "Xin chào " + guestName + ",\n\n" +
                "Đặt phòng của bạn đã được xác nhận!\n\n" +
                "Khách sạn   : " + hotelName + "\n" +
                "Phòng       : " + roomNumber + "\n" +
                "Check-in    : " + checkIn + "\n" +
                "Check-out   : " + checkOut + "\n" +
                "Mã xác nhận : " + confirmationCode + "\n\n" +
                "Cảm ơn bạn đã sử dụng dịch vụ của chúng tôi!\n" +
                "Travel Manager Team");
        mailSender.send(message);
    }

    @Async
    public void sendTourBookingConfirmation(String toEmail, String contactName,
            String tourName, String departureDate,
            int numAdults, int numChildren,
            java.math.BigDecimal finalPrice, String bookingId) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailFrom);
        message.setTo(toEmail);
        message.setSubject("Xác nhận đặt tour - " + tourName);
        message.setText(
                "Xin chào " + contactName + ",\n\n" +
                "Đặt tour của bạn đã được ghi nhận!\n\n" +
                "Tour        : " + tourName + "\n" +
                "Ngày khởi hành: " + departureDate + "\n" +
                "Người lớn   : " + numAdults + "\n" +
                "Trẻ em      : " + numChildren + "\n" +
                "Tổng tiền   : " + finalPrice + " VNĐ\n" +
                "Mã booking  : " + bookingId + "\n\n" +
                "Chúng tôi sẽ liên hệ để xác nhận trong thời gian sớm nhất.\n" +
                "Travel Manager Team");
        mailSender.send(message);
    }

    @Async
    public void sendIncidentReportNotification(String staffName, String tourName,
            String departureDate, String incidentType, String description) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailFrom);
        message.setTo(adminEmail);
        message.setSubject("[CẢNH BÁO] Sự cố Tour: " + tourName);
        message.setText(
                "Staff " + staffName + " đã báo cáo sự cố!\n\n" +
                "Tour        : " + tourName + "\n" +
                "Ngày khởi hành: " + departureDate + "\n" +
                "Loại sự cố  : " + incidentType + "\n\n" +
                "Mô tả:\n" + description + "\n\n" +
                "Vui lòng đăng nhập hệ thống để xử lý.\n" +
                "Travel Manager System");
        mailSender.send(message);
    }

    @Async
    public void sendRestaurantBookingConfirmation(String toEmail, String contactName,
            String restaurantName, String bookingDate, String bookingTime,
            int guestCount, String confirmationCode) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailFrom);
        message.setTo(toEmail);
        message.setSubject("Xác nhận đặt bàn - " + confirmationCode);
        message.setText(
                "Xin chào " + contactName + ",\n\n" +
                "Đặt bàn của bạn đã được xác nhận!\n\n" +
                "Nhà hàng    : " + restaurantName + "\n" +
                "Ngày        : " + bookingDate + "\n" +
                "Giờ         : " + bookingTime + "\n" +
                "Số khách    : " + guestCount + "\n" +
                "Mã xác nhận : " + confirmationCode + "\n\n" +
                "Hẹn gặp bạn!\n" +
                "Travel Manager Team");
        mailSender.send(message);
    }

}
