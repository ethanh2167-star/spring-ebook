package com.ebook.controller;

import com.ebook.config.CurrentUser;
import com.ebook.dto.BookDto;
import com.ebook.model.User;
import com.ebook.service.BookService;
import com.ebook.service.OrderService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
public class BookController {

    private final BookService bookService;
    private final OrderService orderService;

    @GetMapping
    public ResponseEntity<Page<BookDto.BookResponse>> getBooks(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(bookService.getBooks(category, keyword, page, size));
    }

    @GetMapping("/popular")
    public ResponseEntity<List<BookDto.BookResponse>> getPopular() {
        return ResponseEntity.ok(bookService.getPopular());
    }

    @GetMapping("/categories")
    public ResponseEntity<List<String>> getCategories() {
        return ResponseEntity.ok(bookService.getCategories());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getBook(
            @PathVariable Long id,
            @CurrentUser(required = false) User user,
            HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank()) ip = request.getRemoteAddr();

        return bookService.getBookById(id, ip).map(book -> {
            Map<String, Object> result = new HashMap<>();
            result.put("book", book);
            Long userId = user != null ? user.getId() : null;
            result.put("inShelf",  userId != null && bookService.isInShelf(userId, id));
            result.put("progress", userId != null ? bookService.getProgress(userId, id) : null);
            boolean isFree = book.getPrice() != null
                && book.getPrice().compareTo(BigDecimal.ZERO) == 0;
            result.put("hasPurchased",
                userId != null && (isFree || orderService.hasPurchased(userId, id)));
            return ResponseEntity.ok(result);
        }).orElse(ResponseEntity.notFound().build());
    }
}