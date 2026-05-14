package com.ebook.repository;

import com.ebook.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    
    Optional<Order> findByUserIdAndBookId(Long userId, Long bookId);

    
    List<Order> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, Order.Status status);

    
    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);

    
    Optional<Order> findByTradeNo(String tradeNo);

    
    @Query("SELECT COUNT(o) > 0 FROM Order o WHERE o.user.id = :userId AND o.book.id = :bookId AND o.status = 'PAID'")
    boolean hasPurchased(Long userId, Long bookId);
    
    long countByStatus(Order.Status status);

    @Query("SELECT COALESCE(SUM(o.amount), 0) FROM Order o WHERE o.status = 'PAID'")
    BigDecimal sumTotalRevenue();

    @Query("SELECT COALESCE(SUM(o.amount), 0) FROM Order o WHERE o.status = 'PAID' AND o.paidAt >= :startOfMonth")
    BigDecimal sumMonthRevenue(@Param("startOfMonth") LocalDateTime startOfMonth);

    @Query("SELECT o FROM Order o JOIN FETCH o.user JOIN FETCH o.book ORDER BY o.createdAt DESC")
    List<Order> findAllForAdmin();
}