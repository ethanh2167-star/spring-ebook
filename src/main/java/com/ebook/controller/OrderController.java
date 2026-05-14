package com.ebook.controller;

import com.ebook.config.CurrentUser;
import com.ebook.dto.OrderDto;
import com.ebook.model.User;
import com.ebook.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/create")
    public ResponseEntity<OrderDto.CreateOrderResponse> create(
            @RequestParam Long bookId,
            @CurrentUser User user) {
        return ResponseEntity.ok(orderService.createOrder(user.getId(), bookId));
    }

    @PostMapping("/confirm")
    public ResponseEntity<OrderDto.ConfirmPaymentResponse> confirm(
            @RequestParam Long orderId,
            @CurrentUser User user) {
        return ResponseEntity.ok(orderService.confirmPayment(orderId, user.getId()));
    }

    @PostMapping("/cancel")
    public ResponseEntity<OrderDto.CancelOrderResponse> cancel(
            @RequestParam Long orderId,
            @CurrentUser User user) {
        return ResponseEntity.ok(orderService.cancelOrder(orderId, user.getId()));
    }

    @GetMapping("/history")
    public ResponseEntity<List<OrderDto.OrderHistoryItem>> history(@CurrentUser User user) {
        return ResponseEntity.ok(orderService.getOrderHistory(user.getId()));
    }
}