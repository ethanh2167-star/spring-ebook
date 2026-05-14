package com.ebook.repository;

import com.ebook.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.bookshelf WHERE u.id = :id")
    Optional<User> findByIdWithBookshelf(@Param("id") Long id);

    @Query("SELECT COUNT(b) > 0 FROM User u JOIN u.bookshelf b WHERE u.id = :userId AND b.id = :bookId")
    boolean existsBookInShelf(@Param("userId") Long userId, @Param("bookId") Long bookId);
}