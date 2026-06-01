package com.example.travelManager.repository;

import com.example.travelManager.domain.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByTxnRef(String txnRef);

    List<Payment> findByUserEmail(String userEmail);

    List<Payment> findByBookingTypeAndBookingId(String bookingType, Long bookingId);

    List<Payment> findAllByOrderByCreatedAtDesc();

    List<Payment> findByStatusAndCreatedAtBefore(Payment.PaymentStatus status, Instant cutoff);

    @Modifying
    @Query("UPDATE Payment p SET p.status = :newStatus, p.responseCode = :responseCode, " +
           "p.transactionNo = :transactionNo, p.bankCode = :bankCode " +
           "WHERE p.txnRef = :txnRef AND p.status = :oldStatus")
    int compareAndSwap(@Param("txnRef") String txnRef,
                       @Param("newStatus") Payment.PaymentStatus newStatus,
                       @Param("responseCode") String responseCode,
                       @Param("transactionNo") String transactionNo,
                       @Param("bankCode") String bankCode,
                       @Param("oldStatus") Payment.PaymentStatus oldStatus);
}
