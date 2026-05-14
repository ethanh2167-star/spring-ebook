package com.ebook.service;

import com.ebook.dto.BookDto;
import com.ebook.model.Book;
import com.ebook.model.ReadingProgress;
import com.ebook.model.User;
import com.ebook.repository.BookRepository;
import com.ebook.repository.ReadingProgressRepository;
import com.ebook.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class BookService {

    private final BookRepository bookRepository;
    private final UserRepository userRepository;
    private final ReadingProgressRepository progressRepository;

    @Value("${app.upload-dir:uploads}")
    private String uploadDir;

    private final ConcurrentHashMap<String, Long> viewCache = new ConcurrentHashMap<>();
    private static final long VIEW_COOLDOWN_SECONDS = 3600;

    public Page<BookDto.BookResponse> getBooks(String category, String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Book> books;
        if (keyword != null && !keyword.isBlank()) {
            if (category != null && !category.isBlank()) {
                books = bookRepository.searchByCategoryAndKeyword(category, keyword, pageable);
            } else {
                books = bookRepository.searchByKeyword(keyword, pageable);
            }
        } else if (category != null && !category.isBlank()) {
            books = bookRepository.findByCategoryAndPublishedTrueOrderByCreatedAtDesc(category, pageable);
        } else {
            books = bookRepository.findByPublishedTrueOrderByCreatedAtDesc(pageable);
        }
        return books.map(BookDto.BookResponse::from);
    }

    public List<BookDto.BookResponse> getPopular() {
        return bookRepository.findTop5ByPublishedTrueOrderByViewsDesc()
            .stream().map(BookDto.BookResponse::from).toList();
    }

    public List<String> getCategories() {
        return bookRepository.findCategoriesByPublishedTrue();
    }

    @Transactional
    public Optional<BookDto.BookResponse> getBookById(Long id, String ip) {
        return bookRepository.findByIdAndPublishedTrue(id).map(book -> {
            String cacheKey = ip + ":" + id;
            long now = Instant.now().getEpochSecond();
            Long lastViewed = viewCache.get(cacheKey);
            if (lastViewed == null || (now - lastViewed) > VIEW_COOLDOWN_SECONDS) {
                book.setViews(book.getViews() + 1);
                bookRepository.save(book);
                viewCache.put(cacheKey, now);
            }
            return BookDto.BookResponse.from(book);
        });
    }

    @Scheduled(fixedRate = 3600000)
    public void cleanViewCache() {
        long now = Instant.now().getEpochSecond();
        viewCache.entrySet().removeIf(entry ->
            (now - entry.getValue()) > VIEW_COOLDOWN_SECONDS);
    }


    @Transactional
    public boolean addToShelf(Long userId, Long bookId) {
        User user = userRepository.findByIdWithBookshelf(userId).orElseThrow();
        Book book = bookRepository.findById(bookId).orElseThrow();
        boolean added = user.getBookshelf().add(book);
        userRepository.save(user);
        return added;
    }

    @Transactional
    public boolean removeFromShelf(Long userId, Long bookId) {
        User user = userRepository.findByIdWithBookshelf(userId).orElseThrow();
        Book book = bookRepository.findById(bookId).orElseThrow();
        boolean removed = user.getBookshelf().remove(book);
        userRepository.save(user);
        return removed;
    }

    @Transactional(readOnly = true)
    public boolean isInShelf(Long userId, Long bookId) {
        return userRepository.existsBookInShelf(userId, bookId);
    }

    @Transactional(readOnly = true)
    public List<BookDto.BookResponse> getShelf(Long userId) {
        return userRepository.findByIdWithBookshelf(userId)
            .map(User::getBookshelf)
            .orElse(Collections.emptySet())
            .stream().map(BookDto.BookResponse::from).toList();
    }


    @Transactional
    public void updateProgress(Long userId, Long bookId, int page) {
        ReadingProgress progress = progressRepository
            .findByUserIdAndBookId(userId, bookId)
            .orElseGet(() -> {
                ReadingProgress p = new ReadingProgress();
                p.setUser(userRepository.getReferenceById(userId));
                p.setBook(bookRepository.getReferenceById(bookId));
                return p;
            });
        progress.setCurrentPage(page);
        progressRepository.save(progress);
    }

    @Transactional(readOnly = true)
    public int getProgress(Long userId, Long bookId) {
        return progressRepository.findByUserIdAndBookId(userId, bookId)
            .map(ReadingProgress::getCurrentPage)
            .orElse(1);
    }


    public List<BookDto.BookResponse> getAllBooksForAdmin() {
        return bookRepository.findAll()
            .stream().map(BookDto.BookResponse::from).toList();
    }

    @Transactional
    public BookDto.BookResponse createBook(BookDto.BookRequest req) {
        Book book = new Book();
        book.setTitle(req.getTitle());
        book.setAuthor(req.getAuthor());
        book.setDescription(req.getDescription());
        book.setCoverUrl(req.getCoverUrl());
        book.setFileUrl(req.getFileUrl());
        book.setCategory(req.getCategory());
        book.setTotalPages(req.getTotalPages());
        book.setPrice(req.getPrice() != null ? req.getPrice() : BigDecimal.ZERO);
        book.setPublished(req.isPublished());
        return BookDto.BookResponse.from(bookRepository.save(book));
    }

    @Transactional
    public BookDto.BookResponse updateBook(Long bookId, BookDto.BookRequest req) {
        Book book = bookRepository.findById(bookId)
            .orElseThrow(() -> new RuntimeException("書籍不存在"));
        book.setTitle(req.getTitle());
        book.setAuthor(req.getAuthor());
        book.setDescription(req.getDescription());
        if (req.getCoverUrl() != null && !req.getCoverUrl().isBlank()) {
            book.setCoverUrl(req.getCoverUrl());
        }
        if (req.getFileUrl() != null && !req.getFileUrl().isBlank()) {
            book.setFileUrl(req.getFileUrl());
        }
        book.setCategory(req.getCategory());
        book.setTotalPages(req.getTotalPages());
        book.setPrice(req.getPrice() != null ? req.getPrice() : BigDecimal.ZERO);
        book.setPublished(req.isPublished());
        return BookDto.BookResponse.from(bookRepository.save(book));
    }

    @Transactional
    public void deleteBook(Long bookId) {
        if (!bookRepository.existsById(bookId)) {
            throw new RuntimeException("書籍不存在");
        }
        bookRepository.deleteById(bookId);
    }

    @Transactional
    public BookDto.BookResponse publishBook(Long bookId) {
        Book book = bookRepository.findById(bookId)
            .orElseThrow(() -> new RuntimeException("書籍不存在"));
        book.setPublished(true);
        return BookDto.BookResponse.from(bookRepository.save(book));
    }

    @Transactional
    public BookDto.BookResponse unpublishBook(Long bookId) {
        Book book = bookRepository.findById(bookId)
            .orElseThrow(() -> new RuntimeException("書籍不存在"));
        book.setPublished(false);
        return BookDto.BookResponse.from(bookRepository.save(book));
    }

    public String uploadCover(Long bookId, MultipartFile file) {
        try {
            String filename = "cover_" + bookId + "_" + System.currentTimeMillis()
                + getExtension(file.getOriginalFilename());
            Path uploadPath = Paths.get(uploadDir);
            Files.createDirectories(uploadPath);
            file.transferTo(uploadPath.resolve(filename).toFile());
            String url = "/uploads/" + filename;
            bookRepository.findById(bookId).ifPresent(book -> {
                book.setCoverUrl(url);
                bookRepository.save(book);
            });
            return url;
        } catch (Exception e) {
            throw new RuntimeException("封面上傳失敗：" + e.getMessage());
        }
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return ".jpg";
        return filename.substring(filename.lastIndexOf("."));
    }
}