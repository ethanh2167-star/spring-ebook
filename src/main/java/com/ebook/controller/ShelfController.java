package com.ebook.controller;

import com.ebook.config.CurrentUser;
import com.ebook.dto.ApiResponse;
import com.ebook.dto.BookDto;
import com.ebook.model.User;
import com.ebook.service.BookService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/shelf")
@RequiredArgsConstructor
public class ShelfController {

    private final BookService bookService;

    @GetMapping
    public ResponseEntity<List<BookDto.BookResponse>> getShelf(@CurrentUser User user) {
        return ResponseEntity.ok(bookService.getShelf(user.getId()));
    }

    @PostMapping("/add")
    public ResponseEntity<ApiResponse> addToShelf(
            @RequestParam Long bookId,
            @CurrentUser User user) {
        boolean ok = bookService.addToShelf(user.getId(), bookId);
        return ResponseEntity.ok(new ApiResponse(ok, ok ? "已加入書架" : "已在書架中"));
    }

    @PostMapping("/remove")
    public ResponseEntity<ApiResponse> removeFromShelf(
            @RequestParam Long bookId,
            @CurrentUser User user) {
        boolean ok = bookService.removeFromShelf(user.getId(), bookId);
        return ResponseEntity.ok(new ApiResponse(ok, ok ? "已從書架移除" : "操作失敗"));
    }
}