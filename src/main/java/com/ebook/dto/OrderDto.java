package com.ebook.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

public class OrderDto {

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CreateOrderResponse {
        private boolean success;
        private String message;
        private Long orderId;
        private String bookTitle;
        private BigDecimal amount;
        private String username;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ConfirmPaymentResponse {
        private boolean success;
        private String message;
        private String tradeNo;
        private Long bookId;
        private String bookTitle;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CancelOrderResponse {
        private boolean success;
        private String message;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class OrderHistoryItem {
        private Long id;
        private Long bookId;
        private String bookTitle;
        private String coverUrl;
        private BigDecimal amount;
        private String status;
        private String tradeNo;
        private String paidAt;
        private String createdAt;
    }


    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class AdminOrderStats {
        private long paidCount;
        private long pendingCount;
        private long cancelledCount;
        private BigDecimal totalRevenue;
        private BigDecimal monthRevenue;
    }


    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class AdminOrderItem {
        private Long id;
        private String userEmail;
        private String username;
        private Long bookId;
        private String bookTitle;
        private BigDecimal amount;
        private String status;
        private String tradeNo;
        private String paidAt;
        private String createdAt;
    }
}