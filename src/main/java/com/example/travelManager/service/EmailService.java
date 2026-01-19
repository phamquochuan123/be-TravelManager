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

}
