package com.ebook.controller;

import com.ebook.dto.BookDto;
import com.ebook.service.BookService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/books")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminBookController {

    private final BookService bookService;

    @GetMapping
    public ResponseEntity<List<BookDto.BookResponse>> getAllBooks() {
        return ResponseEntity.ok(bookService.getAllBooksForAdmin());
    }

    @PostMapping
    public ResponseEntity<BookDto.BookResponse> createBook(
            @Valid @RequestBody BookDto.BookRequest req) {
        return ResponseEntity.ok(bookService.createBook(req));
    }

    @PutMapping("/{id}")
    public ResponseEntity<BookDto.BookResponse> updateBook(
            @PathVariable Long id,
            @Valid @RequestBody BookDto.BookRequest req) {
        return ResponseEntity.ok(bookService.updateBook(id, req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteBook(@PathVariable Long id) {
        bookService.deleteBook(id);
        return ResponseEntity.ok(Map.of("success", true, "message", "書籍已刪除"));
    }

    @PostMapping("/{id}/cover")
    public ResponseEntity<Map<String, Object>> uploadCover(
            @PathVariable Long id,
            @RequestParam MultipartFile file) {
        String url = bookService.uploadCover(id, file);
        return ResponseEntity.ok(Map.of("success", true, "coverUrl", url));
    }

    @PatchMapping("/{id}/publish")
    public ResponseEntity<BookDto.BookResponse> publishBook(@PathVariable Long id) {
        return ResponseEntity.ok(bookService.publishBook(id));
    }

    @PatchMapping("/{id}/unpublish")
    public ResponseEntity<BookDto.BookResponse> unpublishBook(@PathVariable Long id) {
        return ResponseEntity.ok(bookService.unpublishBook(id));
    }
}