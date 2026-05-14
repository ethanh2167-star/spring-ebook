package com.ebook.controller;

import com.ebook.config.CurrentUser;
import com.ebook.dto.ApiResponse;
import com.ebook.model.User;
import com.ebook.service.BookService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/progress")
@RequiredArgsConstructor
public class ProgressController {

    private final BookService bookService;

    @PostMapping
    public ResponseEntity<ApiResponse> updateProgress(
            @RequestParam Long bookId,
            @RequestParam int page,
            @CurrentUser User user) {
        bookService.updateProgress(user.getId(), bookId, page);
        return ResponseEntity.ok(new ApiResponse(true, "進度已儲存"));
    }
}