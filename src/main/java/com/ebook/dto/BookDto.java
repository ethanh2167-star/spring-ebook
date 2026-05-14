package com.ebook.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class BookDto {
	private BookDto() {
		throw new IllegalStateException("Utility class");
	}

    @Data
    public static class BookRequest {
        @NotBlank(message = "書名不能為空")
        private String title;

        @NotBlank(message = "作者不能為空")
        private String author;

        private String description;
        private String coverUrl;
        private String fileUrl;
        private String category;
        private int totalPages;

        @PositiveOrZero(message = "價格不能為負數")
        private BigDecimal price = BigDecimal.ZERO;

        private boolean published = true;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class BookResponse {
        private Long id;
        private String title;
        private String author;
        private String description;
        private String coverUrl;
        private String fileUrl;
        private String category;
        private int totalPages;
        private int views;
        private BigDecimal price;
        private boolean published;
        private LocalDateTime createdAt;

        public static BookResponse from(com.ebook.model.Book book) {
            return new BookResponse(
                book.getId(),
                book.getTitle(),
                book.getAuthor(),
                book.getDescription(),
                book.getCoverUrl(),
                book.getFileUrl(),
                book.getCategory(),
                book.getTotalPages(),
                book.getViews(),
                book.getPrice(),
                book.isPublished(),
                book.getCreatedAt()
            );
        }
    }
}