package com.ebook.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import com.ebook.dto.BookDto;
import com.ebook.model.Book;
import com.ebook.model.ReadingProgress;
import com.ebook.model.User;
import com.ebook.repository.BookRepository;
import com.ebook.repository.ReadingProgressRepository;
import com.ebook.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class BookServiceTest {

    @Mock private BookRepository bookRepository;
    @Mock private UserRepository userRepository;
    @Mock private ReadingProgressRepository progressRepository;

    @InjectMocks
    private BookService bookService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(bookService, "uploadDir", "uploads");
    }

    @Test
    @DisplayName("getBooks - keyword + category：呼叫 searchByCategoryAndKeyword")
    void getBooks_keywordAndCategory() {
        Page<Book> mockPage = new PageImpl<>(List.of(buildBook(1L, "Spring Boot", "技術")));
        when(bookRepository.searchByCategoryAndKeyword(eq("技術"), eq("Spring"), any(Pageable.class)))
            .thenReturn(mockPage);

        Page<BookDto.BookResponse> result = bookService.getBooks("技術", "Spring", 0, 10);

        assertThat(result.getTotalElements()).isEqualTo(1);
        verify(bookRepository).searchByCategoryAndKeyword(eq("技術"), eq("Spring"), any());
    }

    @Test
    @DisplayName("getBooks - 只有 keyword：呼叫 searchByKeyword")
    void getBooks_keywordOnly() {
        Page<Book> mockPage = new PageImpl<>(List.of(buildBook(1L, "Java 入門", "技術")));
        when(bookRepository.searchByKeyword(eq("Java"), any(Pageable.class)))
            .thenReturn(mockPage);

        Page<BookDto.BookResponse> result = bookService.getBooks(null, "Java", 0, 10);

        assertThat(result.getContent()).hasSize(1);
        verify(bookRepository).searchByKeyword(eq("Java"), any());
    }

    @Test
    @DisplayName("getBooks - 只有 category：呼叫 findByCategoryAndPublishedTrue")
    void getBooks_categoryOnly() {
        Page<Book> mockPage = new PageImpl<>(List.of(buildBook(1L, "設計模式", "技術")));
        when(bookRepository.findByCategoryAndPublishedTrueOrderByCreatedAtDesc(eq("技術"), any(Pageable.class)))
            .thenReturn(mockPage);

        Page<BookDto.BookResponse> result = bookService.getBooks("技術", null, 0, 10);

        assertThat(result.getContent()).hasSize(1);
        verify(bookRepository).findByCategoryAndPublishedTrueOrderByCreatedAtDesc(eq("技術"), any());
    }

    @Test
    @DisplayName("getBooks - 無條件：呼叫 findByPublishedTrue（全部書籍）")
    void getBooks_noFilter() {
        Page<Book> mockPage = new PageImpl<>(List.of(
            buildBook(1L, "書A", "技術"),
            buildBook(2L, "書B", "文學")
        ));
        when(bookRepository.findByPublishedTrueOrderByCreatedAtDesc(any(Pageable.class)))
            .thenReturn(mockPage);

        Page<BookDto.BookResponse> result = bookService.getBooks(null, null, 0, 10);

        assertThat(result.getContent()).hasSize(2);
        verify(bookRepository).findByPublishedTrueOrderByCreatedAtDesc(any());
    }

    @Test
    @DisplayName("getPopular - 回傳瀏覽數最高的書籍列表")
    void getPopular_returnsList() {
        Book b = buildBook(1L, "熱門書", "技術");
        b.setViews(999);
        when(bookRepository.findTop5ByPublishedTrueOrderByViewsDesc()).thenReturn(List.of(b));

        List<BookDto.BookResponse> result = bookService.getPopular();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("熱門書");
    }

    @Test
    @DisplayName("getCategories - 回傳所有分類")
    void getCategories_returnsList() {
        when(bookRepository.findCategoriesByPublishedTrue())
            .thenReturn(List.of("技術", "文學", "商業"));

        List<String> result = bookService.getCategories();

        assertThat(result).containsExactly("技術", "文學", "商業");
    }

    @Test
    @DisplayName("getBookById - 成功：第一次瀏覽，views + 1")
    void getBookById_firstVisit_incrementsViews() {
        Book book = buildBook(1L, "Spring Boot", "技術");
        book.setViews(10);
        when(bookRepository.findByIdAndPublishedTrue(1L)).thenReturn(Optional.of(book));
        when(bookRepository.save(any(Book.class))).thenReturn(book);

        Optional<BookDto.BookResponse> result = bookService.getBookById(1L, "127.0.0.1");

        assertThat(result).isPresent();
        assertThat(book.getViews()).isEqualTo(11);
        verify(bookRepository).save(book);
    }

    @Test
    @DisplayName("getBookById - 同 IP 一小時內重複瀏覽，views 不累加")
    void getBookById_sameIpWithinCooldown_noIncrement() {
        Book book = buildBook(1L, "Spring Boot", "技術");
        book.setViews(10);
        when(bookRepository.findByIdAndPublishedTrue(1L)).thenReturn(Optional.of(book));
        when(bookRepository.save(any(Book.class))).thenReturn(book);
        
        bookService.getBookById(1L, "127.0.0.1");
        int viewsAfterFirst = book.getViews();
        
        bookService.getBookById(1L, "127.0.0.1");
        assertThat(book.getViews()).isEqualTo(viewsAfterFirst); 
        verify(bookRepository, times(1)).save(book);            
    }

    @Test
    @DisplayName("getBookById - 書籍不存在，回傳 Optional.empty()")
    void getBookById_notFound() {
        when(bookRepository.findByIdAndPublishedTrue(999L)).thenReturn(Optional.empty());

        Optional<BookDto.BookResponse> result = bookService.getBookById(999L, "127.0.0.1");

        assertThat(result).isEmpty();
        verify(bookRepository, never()).save(any());
    }

    @Test
    @DisplayName("addToShelf - 成功：書籍加入書架，回傳 true")
    void addToShelf_success() {
        User user = buildUserWithShelf(1L);
        Book book = buildBook(10L, "Spring Boot", "技術");

        when(userRepository.findByIdWithBookshelf(1L)).thenReturn(Optional.of(user));
        when(bookRepository.findById(10L)).thenReturn(Optional.of(book));

        boolean result = bookService.addToShelf(1L, 10L);

        assertThat(result).isTrue();
        assertThat(user.getBookshelf()).contains(book);
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("addToShelf - 書籍已在書架，回傳 false（Set 不重複）")
    void addToShelf_alreadyInShelf() {
        Book book = buildBook(10L, "Spring Boot", "技術");
        User user = buildUserWithShelf(1L);
        user.getBookshelf().add(book); // 預先加入

        when(userRepository.findByIdWithBookshelf(1L)).thenReturn(Optional.of(user));
        when(bookRepository.findById(10L)).thenReturn(Optional.of(book));

        boolean result = bookService.addToShelf(1L, 10L);

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("removeFromShelf - 成功：書籍從書架移除，回傳 true")
    void removeFromShelf_success() {
        Book book = buildBook(10L, "Spring Boot", "技術");
        User user = buildUserWithShelf(1L);
        user.getBookshelf().add(book);

        when(userRepository.findByIdWithBookshelf(1L)).thenReturn(Optional.of(user));
        when(bookRepository.findById(10L)).thenReturn(Optional.of(book));

        boolean result = bookService.removeFromShelf(1L, 10L);

        assertThat(result).isTrue();
        assertThat(user.getBookshelf()).doesNotContain(book);
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("removeFromShelf - 書籍不在書架，回傳 false")
    void removeFromShelf_notInShelf() {
        User user = buildUserWithShelf(1L);
        Book book = buildBook(10L, "Spring Boot", "技術");

        when(userRepository.findByIdWithBookshelf(1L)).thenReturn(Optional.of(user));
        when(bookRepository.findById(10L)).thenReturn(Optional.of(book));

        boolean result = bookService.removeFromShelf(1L, 10L);

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("isInShelf - 委派給 repository，回傳正確結果")
    void isInShelf_delegatesToRepository() {
        when(userRepository.existsBookInShelf(1L, 10L)).thenReturn(true);
        assertThat(bookService.isInShelf(1L, 10L)).isTrue();

        when(userRepository.existsBookInShelf(1L, 99L)).thenReturn(false);
        assertThat(bookService.isInShelf(1L, 99L)).isFalse();
    }

    @Test
    @DisplayName("getShelf - 回傳使用者書架上的書籍清單")
    void getShelf_returnsBooksInShelf() {
        Book book = buildBook(10L, "Spring Boot", "技術");
        User user = buildUserWithShelf(1L);
        user.getBookshelf().add(book);

        when(userRepository.findByIdWithBookshelf(1L)).thenReturn(Optional.of(user));

        List<BookDto.BookResponse> result = bookService.getShelf(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Spring Boot");
    }

    @Test
    @DisplayName("getShelf - 使用者不存在，回傳空列表")
    void getShelf_userNotFound_returnsEmpty() {
        when(userRepository.findByIdWithBookshelf(999L)).thenReturn(Optional.empty());

        List<BookDto.BookResponse> result = bookService.getShelf(999L);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("updateProgress - 已有進度，更新頁數")
    void updateProgress_existingProgress() {
        ReadingProgress progress = new ReadingProgress();
        progress.setCurrentPage(5);

        when(progressRepository.findByUserIdAndBookId(1L, 10L)).thenReturn(Optional.of(progress));

        bookService.updateProgress(1L, 10L, 42);

        assertThat(progress.getCurrentPage()).isEqualTo(42);
        verify(progressRepository).save(progress);
    }

    @Test
    @DisplayName("updateProgress - 無進度紀錄，建立新進度")
    void updateProgress_newProgress() {
        when(progressRepository.findByUserIdAndBookId(1L, 10L)).thenReturn(Optional.empty());
        when(userRepository.getReferenceById(1L)).thenReturn(buildUserWithShelf(1L));
        when(bookRepository.getReferenceById(10L)).thenReturn(buildBook(10L, "Spring Boot", "技術"));

        bookService.updateProgress(1L, 10L, 1);

        verify(progressRepository).save(argThat(p -> p.getCurrentPage() == 1));
    }

    @Test
    @DisplayName("getProgress - 有進度紀錄，回傳當前頁數")
    void getProgress_found() {
        ReadingProgress progress = new ReadingProgress();
        progress.setCurrentPage(55);
        when(progressRepository.findByUserIdAndBookId(1L, 10L)).thenReturn(Optional.of(progress));

        assertThat(bookService.getProgress(1L, 10L)).isEqualTo(55);
    }

    @Test
    @DisplayName("getProgress - 無進度紀錄，預設回傳第 1 頁")
    void getProgress_notFound_returnsDefault() {
        when(progressRepository.findByUserIdAndBookId(1L, 10L)).thenReturn(Optional.empty());

        assertThat(bookService.getProgress(1L, 10L)).isEqualTo(1);
    }

    @Test
    @DisplayName("createBook - 成功：price 正常設定")
    void createBook_success() {
        BookDto.BookRequest req = buildBookRequest("新書", new BigDecimal("199"));
        Book saved = buildBook(1L, "新書", "技術");
        saved.setPrice(new BigDecimal("199"));
        when(bookRepository.save(any(Book.class))).thenReturn(saved);

        BookDto.BookResponse result = bookService.createBook(req);

        assertThat(result.getTitle()).isEqualTo("新書");
        verify(bookRepository).save(any(Book.class));
    }

    @Test
    @DisplayName("createBook - price 為 null 時，預設設為 0")
    void createBook_nullPrice_defaultsToZero() {
        BookDto.BookRequest req = buildBookRequest("免費書", null);
        Book saved = buildBook(1L, "免費書", "技術");
        saved.setPrice(BigDecimal.ZERO);
        when(bookRepository.save(any(Book.class))).thenReturn(saved);

        bookService.createBook(req);

        verify(bookRepository).save(argThat(b -> b.getPrice().compareTo(BigDecimal.ZERO) == 0));
    }

    @Test
    @DisplayName("updateBook - 成功：所有欄位更新")
    void updateBook_success() {
        Book existing = buildBook(1L, "舊書名", "技術");
        BookDto.BookRequest req = buildBookRequest("新書名", new BigDecimal("350"));
        req.setCoverUrl("/uploads/new_cover.jpg");
        req.setFileUrl("/uploads/new_file.pdf");

        when(bookRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(bookRepository.save(any(Book.class))).thenReturn(existing);

        bookService.updateBook(1L, req);

        assertThat(existing.getTitle()).isEqualTo("新書名");
        assertThat(existing.getCoverUrl()).isEqualTo("/uploads/new_cover.jpg");
        verify(bookRepository).save(existing);
    }

    @Test
    @DisplayName("updateBook - coverUrl/fileUrl 為空白，不覆蓋原有值")
    void updateBook_blankUrls_notOverwritten() {
        Book existing = buildBook(1L, "書名", "技術");
        existing.setCoverUrl("/uploads/old_cover.jpg");
        existing.setFileUrl("/uploads/old_file.pdf");

        BookDto.BookRequest req = buildBookRequest("書名", new BigDecimal("299"));
        req.setCoverUrl("");
        req.setFileUrl("  ");

        when(bookRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(bookRepository.save(any(Book.class))).thenReturn(existing);

        bookService.updateBook(1L, req);

        assertThat(existing.getCoverUrl()).isEqualTo("/uploads/old_cover.jpg");
        assertThat(existing.getFileUrl()).isEqualTo("/uploads/old_file.pdf");
    }

    @Test
    @DisplayName("updateBook - 書籍不存在，拋出 RuntimeException")
    void updateBook_notFound() {
        when(bookRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookService.updateBook(999L, buildBookRequest("X", null)))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("書籍不存在");
    }

    @Test
    @DisplayName("deleteBook - 成功：呼叫 deleteById")
    void deleteBook_success() {
        when(bookRepository.existsById(1L)).thenReturn(true);

        bookService.deleteBook(1L);

        verify(bookRepository).deleteById(1L);
    }

    @Test
    @DisplayName("deleteBook - 書籍不存在，拋出 RuntimeException")
    void deleteBook_notFound() {
        when(bookRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> bookService.deleteBook(999L))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("書籍不存在");

        verify(bookRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("publishBook - 成功：published 設為 true")
    void publishBook_success() {
        Book book = buildBook(1L, "草稿書", "技術");
        book.setPublished(false);
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(bookRepository.save(any(Book.class))).thenReturn(book);

        bookService.publishBook(1L);

        assertThat(book.isPublished()).isTrue();
        verify(bookRepository).save(book);
    }

    @Test
    @DisplayName("publishBook - 書籍不存在，拋出 RuntimeException")
    void publishBook_notFound() {
        when(bookRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookService.publishBook(999L))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("書籍不存在");
    }

    @Test
    @DisplayName("unpublishBook - 成功：published 設為 false")
    void unpublishBook_success() {
        Book book = buildBook(1L, "已發布書", "技術");
        book.setPublished(true);
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(bookRepository.save(any(Book.class))).thenReturn(book);

        bookService.unpublishBook(1L);

        assertThat(book.isPublished()).isFalse();
        verify(bookRepository).save(book);
    }

    @Test
    @DisplayName("unpublishBook - 書籍不存在，拋出 RuntimeException")
    void unpublishBook_notFound() {
        when(bookRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookService.unpublishBook(999L))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("書籍不存在");
    }

    private Book buildBook(Long id, String title, String category) {
        Book book = new Book();
        book.setId(id);
        book.setTitle(title);
        book.setAuthor("作者");
        book.setCategory(category);
        book.setPrice(new BigDecimal("299"));
        book.setPublished(true);
        book.setViews(0);
        book.setCreatedAt(LocalDateTime.now());
        return book;
    }

    private User buildUserWithShelf(Long id) {
        User user = new User();
        user.setId(id);
        user.setUsername("bobby");
        user.setEmail("bobby@test.com");
        user.setRole(User.Role.USER);
        user.setBookshelf(new HashSet<>());
        return user;
    }

    private BookDto.BookRequest buildBookRequest(String title, BigDecimal price) {
        BookDto.BookRequest req = new BookDto.BookRequest();
        req.setTitle(title);
        req.setAuthor("作者");
        req.setDescription("描述");
        req.setCategory("技術");
        req.setTotalPages(200);
        req.setPrice(price);
        req.setPublished(true);
        return req;
    }
}