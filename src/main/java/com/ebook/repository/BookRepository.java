package com.ebook.repository;

import com.ebook.model.Book;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface BookRepository extends JpaRepository<Book, Long> {

    
    Page<Book> findByPublishedTrueOrderByCreatedAtDesc(Pageable pageable);
    List<Book> findTop5ByPublishedTrueOrderByViewsDesc();
    Optional<Book> findByIdAndPublishedTrue(Long id);
    Page<Book> findByCategoryAndPublishedTrueOrderByCreatedAtDesc(String category, Pageable pageable);

    @Query("SELECT b FROM Book b WHERE b.published = true " +
           "AND (b.title LIKE %:keyword% OR b.author LIKE %:keyword%) " +
           "ORDER BY b.createdAt DESC")
    Page<Book> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT b FROM Book b WHERE b.published = true " +
           "AND b.category = :category " +
           "AND (b.title LIKE %:keyword% OR b.author LIKE %:keyword%) " +
           "ORDER BY b.createdAt DESC")
    Page<Book> searchByCategoryAndKeyword(@Param("category") String category,
                                          @Param("keyword") String keyword,
                                          Pageable pageable);

    @Query("SELECT DISTINCT b.category FROM Book b WHERE b.published = true " +
           "AND b.category IS NOT NULL ORDER BY b.category")
    List<String> findCategoriesByPublishedTrue();

    @Query("SELECT COUNT(b) > 0 FROM User u JOIN u.bookshelf b " +
           "WHERE u.id = :userId AND b.id = :bookId")
    boolean isBookInShelf(@Param("userId") Long userId,
                          @Param("bookId") Long bookId);
}