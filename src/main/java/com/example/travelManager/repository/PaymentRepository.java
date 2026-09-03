package com.example.travelManager.repository;

import com.example.travelManager.domain.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    /** Lịch sử thanh toán của một user — phân trang ở DB, không load hết rồi cắt trong RAM. */
    Page<Payment> findByUserEmailOrderByCreatedAtDesc(String userEmail, Pageable pageable);

    /**
     * Danh sách giao dịch cho admin, lọc + phân trang ngay tại DB.
     * Mỗi điều kiện đều cho phép null = "không lọc theo tiêu chí này".
     */
    @Query("""
            SELECT p FROM Payment p
            WHERE (:status IS NULL OR p.status = :status)
              AND (:bookingType IS NULL OR p.bookingType = :bookingType)
              AND (:search IS NULL
                   OR LOWER(p.txnRef) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(p.userEmail) LIKE LOWER(CONCAT('%', :search, '%')))
            ORDER BY p.createdAt DESC
            """)
    Page<Payment> searchForAdmin(@Param("status") Payment.PaymentStatus status,
                                 @Param("bookingType") String bookingType,
                                 @Param("search") String search,
                                 Pageable pageable);

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
