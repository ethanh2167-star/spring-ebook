package com.ebook.controller;

import com.ebook.dto.OrderDto;
import com.ebook.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/orders")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminOrderController {

    private final OrderService orderService;

    @GetMapping("/stats")
    public ResponseEntity<OrderDto.AdminOrderStats> getStats() {
        return ResponseEntity.ok(orderService.getAdminStats());
    }

    @GetMapping
    public ResponseEntity<List<OrderDto.AdminOrderItem>> getAllOrders() {
        return ResponseEntity.ok(orderService.getAllOrdersForAdmin());
    }
}