package com.ebook.service;

import com.ebook.dto.AuthDto;
import com.ebook.dto.PasswordResetDto;
import com.ebook.model.PasswordResetToken;
import com.ebook.model.User;
import com.ebook.model.UserSession;
import com.ebook.repository.PasswordResetTokenRepository;
import com.ebook.repository.UserRepository;
import com.ebook.repository.UserSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final UserSessionRepository userSessionRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final LoginAttemptService loginAttemptService;

    @Value("${app.frontend-url}")
    private String frontendUrl;


    @Transactional
    public AuthDto.ApiResponse register(AuthDto.RegisterRequest req) {
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new RuntimeException("此 Email 已被註冊");
        }
        if (userRepository.existsByUsername(req.getUsername())) {
            throw new RuntimeException("此使用者名稱已被註冊");
        }
        User user = new User();
        user.setEmail(req.getEmail());
        user.setUsername(req.getUsername());
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setRole(User.Role.USER);
        userRepository.save(user);

        AuthDto.UserResponse userResp = new AuthDto.UserResponse(
            user.getId(), user.getUsername(), user.getEmail(), user.getRole().name());
        return new AuthDto.ApiResponse(true, "註冊成功", userResp);
    }


    @Retryable(
        retryFor = { ObjectOptimisticLockingFailureException.class },
        maxAttempts = 3,
        backoff = @Backoff(delay = 100)
    )
    @Transactional
    public AuthDto.LoginResponse login(AuthDto.LoginRequest req) {
        User user = userRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new RuntimeException("帳號或密碼錯誤"));

        
        if (user.isAccountLocked()) {
            LocalDateTime unlockTime = user.getLockTime().plusMinutes(30);
            if (LocalDateTime.now().isBefore(unlockTime)) {
                long remaining = java.time.Duration.between(
                    LocalDateTime.now(), unlockTime).toMinutes();
                throw new RuntimeException("帳號已暫時鎖定，請於 " + remaining + " 分鐘後再試");
            } else {
                loginAttemptService.resetFailedAttempts(user);
            }
        }

       
        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            loginAttemptService.handleFailedLogin(user);
            throw new RuntimeException("帳號或密碼錯誤");
        }

       
        loginAttemptService.resetFailedAttempts(user);
        UserSession session = createSession(user, req.getDeviceType());

        var auth = new UsernamePasswordAuthenticationToken(
            user.getEmail(), null,
            List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())));
        SecurityContextHolder.getContext().setAuthentication(auth);

        AuthDto.UserResponse userResp = new AuthDto.UserResponse(
            user.getId(), user.getUsername(), user.getEmail(), user.getRole().name());
        return new AuthDto.LoginResponse(true, "登入成功", userResp,
            session.getToken(), session.getRefreshToken());
    }


    private UserSession createSession(User user, String deviceType) {
        userSessionRepository.deleteByUserAndDeviceType(user, deviceType);

        UserSession newSession = new UserSession();
        newSession.setUser(user);
        newSession.setToken(UUID.randomUUID().toString());
        newSession.setRefreshToken(UUID.randomUUID().toString());
        newSession.setDeviceType(deviceType);
        newSession.setCreatedAt(LocalDateTime.now());
        newSession.setExpiresAt(LocalDateTime.now().plusHours(1));
        newSession.setRefreshExpiresAt(LocalDateTime.now().plusDays(30));

        return userSessionRepository.save(newSession);
    }

    @Transactional
    public void logout(String token) {
        userSessionRepository.findByToken(token)
                .ifPresent(userSessionRepository::delete);
        SecurityContextHolder.clearContext();
    }

    public User validateToken(String token) {
        return userSessionRepository.findByTokenWithUser(token)
                .filter(s -> s.getExpiresAt().isAfter(LocalDateTime.now()))
                .map(UserSession::getUser)
                .orElseThrow(() -> new RuntimeException("Token 無效或已過期"));
    }

    @Transactional
    public AuthDto.RefreshResponse refreshToken(String refreshToken) {
        UserSession session = userSessionRepository
                .findByRefreshTokenWithUser(refreshToken)
                .orElseThrow(() -> new RuntimeException("Refresh Token 無效"));

        if (session.getRefreshExpiresAt().isBefore(LocalDateTime.now())) {
            userSessionRepository.delete(session);
            throw new RuntimeException("Refresh Token 已過期，請重新登入");
        }

        session.setToken(UUID.randomUUID().toString());
        session.setExpiresAt(LocalDateTime.now().plusHours(1));
        userSessionRepository.save(session);

        return new AuthDto.RefreshResponse(true, session.getToken());
    }

    @Transactional
    public void forgotPassword(PasswordResetDto.ForgotPasswordRequest req) {
        userRepository.findByEmail(req.getEmail()).ifPresent(user -> {
            passwordResetTokenRepository.deleteByUser(user);
            PasswordResetToken resetToken = new PasswordResetToken(user);
            passwordResetTokenRepository.save(resetToken);
            emailService.sendPasswordResetEmail(
                user.getEmail(), resetToken.getToken(), frontendUrl);
        });
    }

    @Transactional
    public void resetPassword(PasswordResetDto.ResetPasswordRequest req) {
        PasswordResetToken resetToken = passwordResetTokenRepository
                .findByToken(req.getToken())
                .orElseThrow(() -> new RuntimeException("無效或已使用的重設連結"));

        if (resetToken.isExpired()) {
            passwordResetTokenRepository.delete(resetToken);
            throw new RuntimeException("重設連結已過期，請重新申請");
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(req.getNewPassword()));
        userRepository.save(user);

        loginAttemptService.resetFailedAttempts(user);
        passwordResetTokenRepository.delete(resetToken);
    }
}