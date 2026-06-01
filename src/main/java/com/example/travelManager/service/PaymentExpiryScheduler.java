package com.example.travelManager.service;

import com.example.travelManager.domain.Payment;
import com.example.travelManager.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentExpiryScheduler {

    private final PaymentRepository paymentRepository;

    // Chạy mỗi 2 phút, đánh dấu FAILED các payment PENDING quá 15 phút
    @Scheduled(fixedDelay = 2 * 60 * 1000)
    @Transactional
    public void expireStalePayments() {
        Instant cutoff = Instant.now().minusSeconds(15 * 60);
        List<Payment> stale = paymentRepository
                .findByStatusAndCreatedAtBefore(Payment.PaymentStatus.PENDING, cutoff);
        if (stale.isEmpty()) return;
        stale.forEach(p -> p.setStatus(Payment.PaymentStatus.FAILED));
        paymentRepository.saveAll(stale);
        log.info("Expired {} stale PENDING payments", stale.size());
    }
}
