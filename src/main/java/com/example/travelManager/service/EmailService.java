package com.example.travelManager.service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendWelcomeEmail(String toEmail, String name) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("phamquochuan9876@gmail.com"); // TRÙNG SMTP USERNAME
        message.setTo(toEmail);
        message.setSubject("Welcome to our Travel Manager App");
        message.setText(
                "Dear " + name +
                        ",\n\nWelcome to our Travel Manager App! We're excited to have you on board." +
                        "\n\nBest regards,\nTravel Manager Team");
        mailSender.send(message);
    }

    public void sendResetOtpEmail(String toEmail, String otp) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("phamquochuan9876@gmail.com");
        message.setTo(toEmail);
        message.setSubject("PassWord Reset OTP");
        message.setText("Your OTP for resetting your password is " + otp
                + ". Use this OTP to proceed with resetting your password");
        mailSender.send(message);
    }

    public void sendOtpEmail(String toEmail, String otp) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("phamquochuan9876@gmail.com");
        message.setTo(toEmail);
        message.setSubject("Account Verification OTP");
        message.setText("Your OTP is " + otp + ". Verify your account using this OTP.");
        mailSender.send(message);
    }

    public void sendHotelBookingConfirmation(String toEmail, String guestName,
            String hotelName, String roomNumber,
            String checkIn, String checkOut, String confirmationCode) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("phamquochuan9876@gmail.com");
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

    public void sendTourBookingConfirmation(String toEmail, String contactName,
            String tourName, String departureDate,
            int numAdults, int numChildren,
            java.math.BigDecimal finalPrice, String bookingId) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("phamquochuan9876@gmail.com");
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

    public void sendRestaurantBookingConfirmation(String toEmail, String contactName,
            String restaurantName, String bookingDate, String bookingTime,
            int guestCount, String confirmationCode) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("phamquochuan9876@gmail.com");
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
