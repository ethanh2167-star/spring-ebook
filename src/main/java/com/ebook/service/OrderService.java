package com.ebook.service;

import com.ebook.dto.OrderDto;
import com.ebook.model.Book;
import com.ebook.model.Order;
import com.ebook.model.User;
import com.ebook.repository.BookRepository;
import com.ebook.repository.OrderRepository;
import com.ebook.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository  userRepository;
    private final BookRepository  bookRepository;


    @Transactional
    public OrderDto.CreateOrderResponse createOrder(Long userId, Long bookId) {
        User user = userRepository.findById(userId).orElseThrow();
        Book book = bookRepository.findById(bookId).orElseThrow();

        if (book.getPrice() == null || book.getPrice().compareTo(BigDecimal.ZERO) == 0) {
            return new OrderDto.CreateOrderResponse(false, "此書籍為免費，無需付款",
                    null, null, null, null);
        }

        if (orderRepository.hasPurchased(userId, bookId)) {
            return new OrderDto.CreateOrderResponse(false, "你已購買此書籍",
                    null, null, null, null);
        }

        Optional<Order> existing = orderRepository.findByUserIdAndBookId(userId, bookId);
        Order order = existing.orElseGet(() -> {
            Order o = new Order();
            o.setUser(user);
            o.setBook(book);
            o.setAmount(book.getPrice());
            return orderRepository.save(o);
        });

        return new OrderDto.CreateOrderResponse(
                true, null,
                order.getId(), book.getTitle(), order.getAmount(), user.getUsername());
    }


    @Transactional
    public OrderDto.ConfirmPaymentResponse confirmPayment(Long orderId, Long userId) {
        Order order = orderRepository.findById(orderId).orElse(null);

        if (order == null) {
        	return new OrderDto.ConfirmPaymentResponse(false, "找不到訂單", null, null, null);
        }
        if (!order.getUser().getId().equals(userId)) {
        	return new OrderDto.ConfirmPaymentResponse(false, "無權操作此訂單", null, null, null);
        }
        if (order.isPaid()) {
        	return new OrderDto.ConfirmPaymentResponse(true, "此訂單已付款", order.getTradeNo(), order.getBook().getId(), order.getBook().getTitle());
        }

        String tradeNo = "MOCK"
                + DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(LocalDateTime.now())
                + String.format("%04d", orderId);
        order.setStatus(Order.Status.PAID);
        order.setTradeNo(tradeNo);
        order.setPaidAt(LocalDateTime.now());
        orderRepository.save(order);

        return new OrderDto.ConfirmPaymentResponse(true, "付款成功", tradeNo, order.getBook().getId(), order.getBook().getTitle());
    }


    @Transactional
    public OrderDto.CancelOrderResponse cancelOrder(Long orderId, Long userId) {
        Order order = orderRepository.findById(orderId).orElse(null);

        if (order == null || !order.getUser().getId().equals(userId)) {
            return new OrderDto.CancelOrderResponse(false, "找不到訂單");
        }
        if (order.isPaid()) {
            return new OrderDto.CancelOrderResponse(false, "已付款訂單無法取消");
        }

        order.setStatus(Order.Status.FAILED);
        orderRepository.save(order);
        return new OrderDto.CancelOrderResponse(true, "訂單已取消");
    }


    public boolean hasPurchased(Long userId, Long bookId) {
        return orderRepository.hasPurchased(userId, bookId);
    }


    public List<OrderDto.OrderHistoryItem> getOrderHistory(Long userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream().map(o -> new OrderDto.OrderHistoryItem(
                        o.getId(),
                        o.getBook().getId(),
                        o.getBook().getTitle(),
                        o.getBook().getCoverUrl(),
                        o.getAmount(),
                        o.getStatus().name(),
                        o.getTradeNo(),
                        o.getPaidAt() != null ? o.getPaidAt().toString() : null,
                        o.getCreatedAt().toString()
                )).toList();
    }
    public OrderDto.AdminOrderStats getAdminStats() {
        long paid      = orderRepository.countByStatus(Order.Status.PAID);
        long pending   = orderRepository.countByStatus(Order.Status.PENDING);
        long cancelled = orderRepository.countByStatus(Order.Status.FAILED);
        BigDecimal total = orderRepository.sumTotalRevenue();
        BigDecimal month = orderRepository.sumMonthRevenue(
                LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0));
        return new OrderDto.AdminOrderStats(paid, pending, cancelled, total, month);
    }

    public List<OrderDto.AdminOrderItem> getAllOrdersForAdmin() {
        return orderRepository.findAllForAdmin().stream().map(o ->
            new OrderDto.AdminOrderItem(
                o.getId(),
                o.getUser().getEmail(),
                o.getUser().getUsername(),
                o.getBook().getId(),
                o.getBook().getTitle(),
                o.getAmount(),
                o.getStatus().name(),
                o.getTradeNo(),
                o.getPaidAt()   != null ? o.getPaidAt().toString()   : null,
                o.getCreatedAt() != null ? o.getCreatedAt().toString() : null
            )
        ).toList();
    }
}