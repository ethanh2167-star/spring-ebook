package com.ebook.service;

import com.ebook.dto.OrderDto;
import com.ebook.model.Book;
import com.ebook.model.Order;
import com.ebook.model.User;
import com.ebook.repository.BookRepository;
import com.ebook.repository.OrderRepository;
import com.ebook.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private UserRepository  userRepository;
    @Mock private BookRepository  bookRepository;

    @InjectMocks
    private OrderService orderService;

    @Test
    @DisplayName("createOrder - 成功：建立新訂單並回傳訂單資訊")
    void createOrder_success() {
        User user = buildUser(1L, "bobby");
        Book book = buildBook(10L, "Spring Boot 入門", new BigDecimal("299"));
        Order saved = buildOrder(100L, user, book, Order.Status.PENDING);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(bookRepository.findById(10L)).thenReturn(Optional.of(book));
        when(orderRepository.hasPurchased(1L, 10L)).thenReturn(false);
        when(orderRepository.findByUserIdAndBookId(1L, 10L)).thenReturn(Optional.empty());
        when(orderRepository.save(any(Order.class))).thenReturn(saved);

        OrderDto.CreateOrderResponse resp = orderService.createOrder(1L, 10L);

        assertThat(resp.isSuccess()).isTrue();
        assertThat(resp.getOrderId()).isEqualTo(100L);
        assertThat(resp.getAmount()).isEqualByComparingTo("299");
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    @DisplayName("createOrder - 失敗：書籍為免費（price = 0）")
    void createOrder_freeBook() {
        User user = buildUser(1L, "bobby");
        Book book = buildBook(10L, "免費書", BigDecimal.ZERO);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(bookRepository.findById(10L)).thenReturn(Optional.of(book));

        OrderDto.CreateOrderResponse resp = orderService.createOrder(1L, 10L);

        assertThat(resp.isSuccess()).isFalse();
        assertThat(resp.getMessage()).isEqualTo("此書籍為免費，無需付款");
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("createOrder - 失敗：書籍 price 為 null（視為免費）")
    void createOrder_nullPrice() {
        User user = buildUser(1L, "bobby");
        Book book = buildBook(10L, "無定價書", null);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(bookRepository.findById(10L)).thenReturn(Optional.of(book));

        OrderDto.CreateOrderResponse resp = orderService.createOrder(1L, 10L);

        assertThat(resp.isSuccess()).isFalse();
        assertThat(resp.getMessage()).isEqualTo("此書籍為免費，無需付款");
    }

    @Test
    @DisplayName("createOrder - 失敗：使用者已購買此書")
    void createOrder_alreadyPurchased() {
        User user = buildUser(1L, "bobby");
        Book book = buildBook(10L, "Spring Boot 入門", new BigDecimal("299"));

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(bookRepository.findById(10L)).thenReturn(Optional.of(book));
        when(orderRepository.hasPurchased(1L, 10L)).thenReturn(true);

        OrderDto.CreateOrderResponse resp = orderService.createOrder(1L, 10L);

        assertThat(resp.isSuccess()).isFalse();
        assertThat(resp.getMessage()).isEqualTo("你已購買此書籍");
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("createOrder - 成功：已有 PENDING 訂單，直接回傳既有訂單")
    void createOrder_existingPendingOrder() {
        User user = buildUser(1L, "bobby");
        Book book = buildBook(10L, "Spring Boot 入門", new BigDecimal("299"));
        Order existing = buildOrder(99L, user, book, Order.Status.PENDING);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(bookRepository.findById(10L)).thenReturn(Optional.of(book));
        when(orderRepository.hasPurchased(1L, 10L)).thenReturn(false);
        when(orderRepository.findByUserIdAndBookId(1L, 10L)).thenReturn(Optional.of(existing));

        OrderDto.CreateOrderResponse resp = orderService.createOrder(1L, 10L);

        assertThat(resp.isSuccess()).isTrue();
        assertThat(resp.getOrderId()).isEqualTo(99L);
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("confirmPayment - 成功：訂單標記為已付款")
    void confirmPayment_success() {
        User user = buildUser(1L, "bobby");
        Book book = buildBook(10L, "Spring Boot 入門", new BigDecimal("299"));
        Order order = buildOrder(100L, user, book, Order.Status.PENDING);

        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));

        OrderDto.ConfirmPaymentResponse resp = orderService.confirmPayment(100L, 1L);

        assertThat(resp.isSuccess()).isTrue();
        assertThat(resp.getMessage()).isEqualTo("付款成功");
        assertThat(resp.getTradeNo()).startsWith("MOCK");
        assertThat(order.getStatus()).isEqualTo(Order.Status.PAID);
        verify(orderRepository).save(order);
    }

    @Test
    @DisplayName("confirmPayment - 失敗：訂單不存在")
    void confirmPayment_orderNotFound() {
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        OrderDto.ConfirmPaymentResponse resp = orderService.confirmPayment(999L, 1L);

        assertThat(resp.isSuccess()).isFalse();
        assertThat(resp.getMessage()).isEqualTo("找不到訂單");
    }

    @Test
    @DisplayName("confirmPayment - 失敗：操作者非訂單所有人")
    void confirmPayment_wrongUser() {
        User owner = buildUser(1L, "bobby");
        Book book  = buildBook(10L, "Spring Boot 入門", new BigDecimal("299"));
        Order order = buildOrder(100L, owner, book, Order.Status.PENDING);

        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));

        
        OrderDto.ConfirmPaymentResponse resp = orderService.confirmPayment(100L, 2L);

        assertThat(resp.isSuccess()).isFalse();
        assertThat(resp.getMessage()).isEqualTo("無權操作此訂單");
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("confirmPayment - 已付款：直接回傳成功，不重複付款")
    void confirmPayment_alreadyPaid() {
        User user = buildUser(1L, "bobby");
        Book book = buildBook(10L, "Spring Boot 入門", new BigDecimal("299"));
        Order order = buildOrder(100L, user, book, Order.Status.PAID);
        order.setTradeNo("MOCK20240101120000100");

        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));

        OrderDto.ConfirmPaymentResponse resp = orderService.confirmPayment(100L, 1L);

        assertThat(resp.isSuccess()).isTrue();
        assertThat(resp.getMessage()).isEqualTo("此訂單已付款");
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("cancelOrder - 成功：訂單狀態改為 FAILED")
    void cancelOrder_success() {
        User user = buildUser(1L, "bobby");
        Book book = buildBook(10L, "Spring Boot 入門", new BigDecimal("299"));
        Order order = buildOrder(100L, user, book, Order.Status.PENDING);

        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));

        OrderDto.CancelOrderResponse resp = orderService.cancelOrder(100L, 1L);

        assertThat(resp.isSuccess()).isTrue();
        assertThat(resp.getMessage()).isEqualTo("訂單已取消");
        assertThat(order.getStatus()).isEqualTo(Order.Status.FAILED);
        verify(orderRepository).save(order);
    }

    @Test
    @DisplayName("cancelOrder - 失敗：訂單不存在")
    void cancelOrder_orderNotFound() {
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        OrderDto.CancelOrderResponse resp = orderService.cancelOrder(999L, 1L);

        assertThat(resp.isSuccess()).isFalse();
        assertThat(resp.getMessage()).isEqualTo("找不到訂單");
    }

    @Test
    @DisplayName("cancelOrder - 失敗：操作者非訂單所有人")
    void cancelOrder_wrongUser() {
        User owner = buildUser(1L, "bobby");
        Book book  = buildBook(10L, "Spring Boot 入門", new BigDecimal("299"));
        Order order = buildOrder(100L, owner, book, Order.Status.PENDING);

        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));

        OrderDto.CancelOrderResponse resp = orderService.cancelOrder(100L, 2L);

        assertThat(resp.isSuccess()).isFalse();
        assertThat(resp.getMessage()).isEqualTo("找不到訂單");
    }

    @Test
    @DisplayName("cancelOrder - 失敗：已付款訂單無法取消")
    void cancelOrder_alreadyPaid() {
        User user = buildUser(1L, "bobby");
        Book book = buildBook(10L, "Spring Boot 入門", new BigDecimal("299"));
        Order order = buildOrder(100L, user, book, Order.Status.PAID);

        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));

        OrderDto.CancelOrderResponse resp = orderService.cancelOrder(100L, 1L);

        assertThat(resp.isSuccess()).isFalse();
        assertThat(resp.getMessage()).isEqualTo("已付款訂單無法取消");
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("hasPurchased - 已購買，回傳 true")
    void hasPurchased_true() {
        when(orderRepository.hasPurchased(1L, 10L)).thenReturn(true);
        assertThat(orderService.hasPurchased(1L, 10L)).isTrue();
    }

    @Test
    @DisplayName("hasPurchased - 未購買，回傳 false")
    void hasPurchased_false() {
        when(orderRepository.hasPurchased(1L, 10L)).thenReturn(false);
        assertThat(orderService.hasPurchased(1L, 10L)).isFalse();
    }

    @Test
    @DisplayName("getOrderHistory - 回傳正確筆數與欄位")
    void getOrderHistory_returnsList() {
        User user = buildUser(1L, "bobby");
        Book book = buildBook(10L, "Spring Boot 入門", new BigDecimal("299"));
        Order order = buildOrder(100L, user, book, Order.Status.PAID);
        order.setTradeNo("MOCK001");
        order.setPaidAt(LocalDateTime.now());
        order.setCreatedAt(LocalDateTime.now());

        when(orderRepository.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(order));

        List<OrderDto.OrderHistoryItem> result = orderService.getOrderHistory(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getBookTitle()).isEqualTo("Spring Boot 入門");
        assertThat(result.get(0).getStatus()).isEqualTo("PAID");
        assertThat(result.get(0).getTradeNo()).isEqualTo("MOCK001");
    }

    @Test
    @DisplayName("getOrderHistory - 無訂單，回傳空列表")
    void getOrderHistory_empty() {
        when(orderRepository.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of());

        List<OrderDto.OrderHistoryItem> result = orderService.getOrderHistory(1L);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("getAdminStats - 正確彙總各狀態數量與金額")
    void getAdminStats_correctAggregation() {
        when(orderRepository.countByStatus(Order.Status.PAID)).thenReturn(5L);
        when(orderRepository.countByStatus(Order.Status.PENDING)).thenReturn(3L);
        when(orderRepository.countByStatus(Order.Status.FAILED)).thenReturn(2L);
        when(orderRepository.sumTotalRevenue()).thenReturn(new BigDecimal("1500"));
        when(orderRepository.sumMonthRevenue(any(LocalDateTime.class)))
            .thenReturn(new BigDecimal("500"));

        OrderDto.AdminOrderStats stats = orderService.getAdminStats();

        assertThat(stats.getPaidCount()).isEqualTo(5L);
        assertThat(stats.getPendingCount()).isEqualTo(3L);
        assertThat(stats.getCancelledCount()).isEqualTo(2L);
        assertThat(stats.getTotalRevenue()).isEqualByComparingTo("1500");
        assertThat(stats.getMonthRevenue()).isEqualByComparingTo("500");
    }

    private User buildUser(Long id, String username) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setEmail(username + "@test.com");
        user.setRole(User.Role.USER);
        return user;
    }

    private Book buildBook(Long id, String title, BigDecimal price) {
        Book book = new Book();
        book.setId(id);
        book.setTitle(title);
        book.setPrice(price);
        return book;
    }

    private Order buildOrder(Long id, User user, Book book, Order.Status status) {
        Order order = new Order();
        order.setId(id);
        order.setUser(user);
        order.setBook(book);
        order.setAmount(book.getPrice());
        order.setStatus(status);
        order.setCreatedAt(LocalDateTime.now());
        return order;
    }
}