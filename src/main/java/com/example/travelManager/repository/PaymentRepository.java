package com.example.travelManager.repository;

import com.example.travelManager.domain.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByTxnRef(String txnRef);

    List<Payment> findByUserEmail(String userEmail);

    List<Payment> findByBookingTypeAndBookingId(String bookingType, Long bookingId);

    List<Payment> findAllByOrderByCreatedAtDesc();
}
