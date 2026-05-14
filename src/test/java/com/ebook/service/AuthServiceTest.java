package com.ebook.service;

import com.ebook.dto.AuthDto;
import com.ebook.dto.PasswordResetDto;
import com.ebook.model.PasswordResetToken;
import com.ebook.model.User;
import com.ebook.model.UserSession;
import com.ebook.repository.PasswordResetTokenRepository;
import com.ebook.repository.UserRepository;
import com.ebook.repository.UserSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordResetTokenRepository passwordResetTokenRepository;
    @Mock private UserSessionRepository userSessionRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private EmailService emailService;
    @Mock private LoginAttemptService loginAttemptService;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void setUp() {

        ReflectionTestUtils.setField(authService, "frontendUrl", "http://localhost:5173");
    }

    @Test
    @DisplayName("register - 成功：回傳 ApiResponse 且 success = true")
    void register_success() {
        AuthDto.RegisterRequest req = new AuthDto.RegisterRequest();
        req.setEmail("bobby@test.com");
        req.setUsername("bobby");
        req.setPassword("password123");

        when(userRepository.existsByEmail(req.getEmail())).thenReturn(false);
        when(userRepository.existsByUsername(req.getUsername())).thenReturn(false);
        when(passwordEncoder.encode(req.getPassword())).thenReturn("hashed_pw");

        User savedUser = new User();
        savedUser.setId(1L);
        savedUser.setEmail(req.getEmail());
        savedUser.setUsername(req.getUsername());
        savedUser.setPassword("hashed_pw");
        savedUser.setRole(User.Role.USER);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        AuthDto.ApiResponse resp = authService.register(req);

        assertThat(resp.isSuccess()).isTrue();
        assertThat(resp.getMessage()).isEqualTo("註冊成功");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("register - 失敗：Email 已被使用")
    void register_emailAlreadyExists() {
        AuthDto.RegisterRequest req = new AuthDto.RegisterRequest();
        req.setEmail("existing@test.com");
        req.setUsername("bobby");
        req.setPassword("password123");

        when(userRepository.existsByEmail(req.getEmail())).thenReturn(true);

        assertThatThrownBy(() -> authService.register(req))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("此 Email 已被註冊");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("register - 失敗：使用者名稱已被使用")
    void register_usernameAlreadyExists() {
        AuthDto.RegisterRequest req = new AuthDto.RegisterRequest();
        req.setEmail("new@test.com");
        req.setUsername("existingUser");
        req.setPassword("password123");

        when(userRepository.existsByEmail(req.getEmail())).thenReturn(false);
        when(userRepository.existsByUsername(req.getUsername())).thenReturn(true);

        assertThatThrownBy(() -> authService.register(req))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("此使用者名稱已被註冊");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("login - 成功：回傳 token 與 refreshToken")
    void login_success() {
        AuthDto.LoginRequest req = new AuthDto.LoginRequest();
        req.setEmail("bobby@test.com");
        req.setPassword("password123");
        req.setDeviceType("web");

        User user = buildUnlockedUser();
        when(userRepository.findByEmail(req.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(req.getPassword(), user.getPassword())).thenReturn(true);

        UserSession session = buildSession(user);
        when(userSessionRepository.save(any(UserSession.class))).thenReturn(session);

        AuthDto.LoginResponse resp = authService.login(req);

        assertThat(resp.isSuccess()).isTrue();
        assertThat(resp.getToken()).isNotBlank();
        assertThat(resp.getRefreshToken()).isNotBlank();
        verify(loginAttemptService).resetFailedAttempts(user);
    }

    @Test
    @DisplayName("login - 失敗：Email 不存在")
    void login_emailNotFound() {
        AuthDto.LoginRequest req = new AuthDto.LoginRequest();
        req.setEmail("nobody@test.com");
        req.setPassword("password123");

        when(userRepository.findByEmail(req.getEmail())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(req))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("帳號或密碼錯誤");
    }

    @Test
    @DisplayName("login - 失敗：密碼錯誤，觸發 handleFailedLogin")
    void login_wrongPassword() {
        AuthDto.LoginRequest req = new AuthDto.LoginRequest();
        req.setEmail("bobby@test.com");
        req.setPassword("wrongPass");
        req.setDeviceType("web");

        User user = buildUnlockedUser();
        when(userRepository.findByEmail(req.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(req.getPassword(), user.getPassword())).thenReturn(false);

        assertThatThrownBy(() -> authService.login(req))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("帳號或密碼錯誤");

        verify(loginAttemptService).handleFailedLogin(user);
        verify(loginAttemptService, never()).resetFailedAttempts(any());
    }

    @Test
    @DisplayName("login - 失敗：帳號仍在鎖定期內")
    void login_accountStillLocked() {
        AuthDto.LoginRequest req = new AuthDto.LoginRequest();
        req.setEmail("bobby@test.com");
        req.setPassword("password123");

        User user = buildUnlockedUser();
        user.setAccountLocked(true);
        user.setLockTime(LocalDateTime.now().minusMinutes(10));

        when(userRepository.findByEmail(req.getEmail())).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(req))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("帳號已暫時鎖定");

        verify(loginAttemptService, never()).resetFailedAttempts(any());
    }

    @Test
    @DisplayName("login - 成功：帳號鎖定已到期，自動解鎖並登入")
    void login_lockExpired_autoUnlock() {
        AuthDto.LoginRequest req = new AuthDto.LoginRequest();
        req.setEmail("bobby@test.com");
        req.setPassword("password123");
        req.setDeviceType("web");

        User user = buildUnlockedUser();
        user.setAccountLocked(true);

        user.setLockTime(LocalDateTime.now().minusMinutes(31));

        when(userRepository.findByEmail(req.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(req.getPassword(), user.getPassword())).thenReturn(true);

        UserSession session = buildSession(user);
        when(userSessionRepository.save(any(UserSession.class))).thenReturn(session);

        AuthDto.LoginResponse resp = authService.login(req);

        assertThat(resp.isSuccess()).isTrue();

        verify(loginAttemptService, times(2)).resetFailedAttempts(user);
    }

    @Test
    @DisplayName("logout - 成功：找到 session 並刪除")
    void logout_sessionFound() {
        String token = UUID.randomUUID().toString();
        UserSession session = new UserSession();
        when(userSessionRepository.findByToken(token)).thenReturn(Optional.of(session));

        authService.logout(token);

        verify(userSessionRepository).delete(session);
    }

    @Test
    @DisplayName("logout - 無動作：Token 不存在，不拋出例外")
    void logout_sessionNotFound() {
        String token = "invalid-token";
        when(userSessionRepository.findByToken(token)).thenReturn(Optional.empty());

        assertThatCode(() -> authService.logout(token)).doesNotThrowAnyException();
        verify(userSessionRepository, never()).delete(any());
    }

    @Test
    @DisplayName("validateToken - 成功：回傳對應的 User")
    void validateToken_success() {
        User user = buildUnlockedUser();
        UserSession session = buildSession(user);

        when(userSessionRepository.findByTokenWithUser(session.getToken()))
            .thenReturn(Optional.of(session));

        User result = authService.validateToken(session.getToken());

        assertThat(result.getEmail()).isEqualTo(user.getEmail());
    }

    @Test
    @DisplayName("validateToken - 失敗：Token 已過期")
    void validateToken_expired() {
        User user = buildUnlockedUser();
        UserSession session = buildSession(user);
        session.setExpiresAt(LocalDateTime.now().minusHours(1));

        when(userSessionRepository.findByTokenWithUser(session.getToken()))
            .thenReturn(Optional.of(session));

        assertThatThrownBy(() -> authService.validateToken(session.getToken()))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Token 無效或已過期");
    }

    @Test
    @DisplayName("validateToken - 失敗：Token 不存在")
    void validateToken_notFound() {
        when(userSessionRepository.findByTokenWithUser("ghost-token"))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.validateToken("ghost-token"))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Token 無效或已過期");
    }

    @Test
    @DisplayName("refreshToken - 成功：產生新的 access token")
    void refreshToken_success() {
        User user = buildUnlockedUser();
        UserSession session = buildSession(user);
        String refreshToken = session.getRefreshToken();

        when(userSessionRepository.findByRefreshTokenWithUser(refreshToken))
            .thenReturn(Optional.of(session));
        when(userSessionRepository.save(any(UserSession.class))).thenReturn(session);

        AuthDto.RefreshResponse resp = authService.refreshToken(refreshToken);

        assertThat(resp.isSuccess()).isTrue();
        assertThat(resp.getToken()).isNotBlank();
    }

    @Test
    @DisplayName("refreshToken - 失敗：Refresh Token 不存在")
    void refreshToken_notFound() {
        when(userSessionRepository.findByRefreshTokenWithUser("bad-refresh"))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refreshToken("bad-refresh"))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Refresh Token 無效");
    }

    @Test
    @DisplayName("refreshToken - 失敗：Refresh Token 已過期，session 被刪除")
    void refreshToken_expired() {
        User user = buildUnlockedUser();
        UserSession session = buildSession(user);
        session.setRefreshExpiresAt(LocalDateTime.now().minusDays(1));
        String refreshToken = session.getRefreshToken();

        when(userSessionRepository.findByRefreshTokenWithUser(refreshToken))
            .thenReturn(Optional.of(session));

        assertThatThrownBy(() -> authService.refreshToken(refreshToken))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Refresh Token 已過期，請重新登入");

        verify(userSessionRepository).delete(session);
    }

    @Test
    @DisplayName("forgotPassword - 成功：找到 user，寄送重設信")
    void forgotPassword_userExists() {
        PasswordResetDto.ForgotPasswordRequest req = new PasswordResetDto.ForgotPasswordRequest();
        req.setEmail("bobby@test.com");

        User user = buildUnlockedUser();
        when(userRepository.findByEmail(req.getEmail())).thenReturn(Optional.of(user));
        when(passwordResetTokenRepository.save(any(PasswordResetToken.class)))
            .thenAnswer(inv -> inv.getArgument(0));

        authService.forgotPassword(req);

        verify(passwordResetTokenRepository).deleteByUser(user);
        verify(passwordResetTokenRepository).save(any(PasswordResetToken.class));
        verify(emailService).sendPasswordResetEmail(eq(user.getEmail()), anyString(), anyString());
    }

    @Test
    @DisplayName("forgotPassword - 靜默成功：Email 不存在，不寄信（防列舉攻擊）")
    void forgotPassword_userNotFound_silentSuccess() {
        PasswordResetDto.ForgotPasswordRequest req = new PasswordResetDto.ForgotPasswordRequest();
        req.setEmail("nobody@test.com");

        when(userRepository.findByEmail(req.getEmail())).thenReturn(Optional.empty());

        assertThatCode(() -> authService.forgotPassword(req)).doesNotThrowAnyException();
        verify(emailService, never()).sendPasswordResetEmail(any(), any(), any());
    }

    @Test
    @DisplayName("resetPassword - 成功：密碼更新，token 刪除")
    void resetPassword_success() {
        PasswordResetDto.ResetPasswordRequest req = new PasswordResetDto.ResetPasswordRequest();
        req.setToken("valid-token");
        req.setNewPassword("newSecurePass");

        User user = buildUnlockedUser();
        PasswordResetToken resetToken = mock(PasswordResetToken.class);
        when(resetToken.isExpired()).thenReturn(false);
        when(resetToken.getUser()).thenReturn(user);

        when(passwordResetTokenRepository.findByToken("valid-token"))
            .thenReturn(Optional.of(resetToken));
        when(passwordEncoder.encode("newSecurePass")).thenReturn("hashed_new");

        authService.resetPassword(req);

        assertThat(user.getPassword()).isEqualTo("hashed_new");
        verify(userRepository).save(user);
        verify(passwordResetTokenRepository).delete(resetToken);
        verify(loginAttemptService).resetFailedAttempts(user);
    }

    @Test
    @DisplayName("resetPassword - 失敗：Token 不存在")
    void resetPassword_tokenNotFound() {
        PasswordResetDto.ResetPasswordRequest req = new PasswordResetDto.ResetPasswordRequest();
        req.setToken("ghost-token");
        req.setNewPassword("newPass");

        when(passwordResetTokenRepository.findByToken("ghost-token"))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.resetPassword(req))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("無效或已使用的重設連結");
    }

    @Test
    @DisplayName("resetPassword - 失敗：Token 已過期，刪除 token 並拋例外")
    void resetPassword_tokenExpired() {
        PasswordResetDto.ResetPasswordRequest req = new PasswordResetDto.ResetPasswordRequest();
        req.setToken("expired-token");
        req.setNewPassword("newPass");

        PasswordResetToken resetToken = mock(PasswordResetToken.class);
        when(resetToken.isExpired()).thenReturn(true);

        when(passwordResetTokenRepository.findByToken("expired-token"))
            .thenReturn(Optional.of(resetToken));

        assertThatThrownBy(() -> authService.resetPassword(req))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("重設連結已過期，請重新申請");

        verify(passwordResetTokenRepository).delete(resetToken);
        verify(userRepository, never()).save(any());
    }

    private User buildUnlockedUser() {
        User user = new User();
        user.setId(1L);
        user.setEmail("bobby@test.com");
        user.setUsername("bobby");
        user.setPassword("hashed_pw");
        user.setRole(User.Role.USER);
        user.setAccountLocked(false);
        return user;
    }

    private UserSession buildSession(User user) {
        UserSession session = new UserSession();
        session.setUser(user);
        session.setToken(UUID.randomUUID().toString());
        session.setRefreshToken(UUID.randomUUID().toString());
        session.setDeviceType("web");
        session.setCreatedAt(LocalDateTime.now());
        session.setExpiresAt(LocalDateTime.now().plusHours(1));
        session.setRefreshExpiresAt(LocalDateTime.now().plusDays(30));
        return session;
    }
}