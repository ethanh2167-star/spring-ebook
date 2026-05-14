package com.ebook.repository;

import com.ebook.model.ReadingProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ReadingProgressRepository extends JpaRepository<ReadingProgress, Long> {
    Optional<ReadingProgress> findByUserIdAndBookId(Long userId, Long bookId);
}
