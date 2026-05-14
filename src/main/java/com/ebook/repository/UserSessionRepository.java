package com.ebook.repository;

import com.ebook.model.User;
import com.ebook.model.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.Optional;

public interface UserSessionRepository extends JpaRepository<UserSession, Long> {

    Optional<UserSession> findByToken(String token);

    Optional<UserSession> findByUserAndDeviceType(User user, String deviceType);

    void deleteByExpiresAtBefore(LocalDateTime now);

    void deleteByUserAndDeviceType(User user, String deviceType);

    @Query("SELECT s FROM UserSession s JOIN FETCH s.user WHERE s.token = :token")
    Optional<UserSession> findByTokenWithUser(@Param("token") String token);

    @Query("SELECT s FROM UserSession s JOIN FETCH s.user WHERE s.refreshToken = :refreshToken")
    Optional<UserSession> findByRefreshTokenWithUser(@Param("refreshToken") String refreshToken);

	void deleteByRefreshExpiresAtBefore(LocalDateTime now);
}