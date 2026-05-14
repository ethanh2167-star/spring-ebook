package com.ebook.controller;

import com.ebook.config.CurrentUser;
import com.ebook.dto.AuthDto;
import com.ebook.dto.AuthDto.*;
import com.ebook.dto.PasswordResetDto;
import com.ebook.model.User;
import com.ebook.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse> register(@Valid @RequestBody RegisterRequest req) {
        return ResponseEntity.ok(authService.register(req));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest req) {
        return ResponseEntity.ok(authService.login(req));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse> logout(
            @RequestHeader("Authorization") String bearerToken) {
        String token = bearerToken.replace("Bearer ", "");
        authService.logout(token);
        return ResponseEntity.ok(new ApiResponse(true, "已登出"));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse> forgotPassword(
            @Valid @RequestBody PasswordResetDto.ForgotPasswordRequest req) {
        authService.forgotPassword(req);
        return ResponseEntity.ok(new ApiResponse(true, "若此 Email 已註冊，重設連結已寄出，請查收信箱"));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse> resetPassword(
            @Valid @RequestBody PasswordResetDto.ResetPasswordRequest req) {
        authService.resetPassword(req);
        return ResponseEntity.ok(new ApiResponse(true, "密碼已成功重設，請重新登入"));
    }

    @GetMapping("/me")
    ResponseEntity<ApiResponse> me(@CurrentUser(required = false) User user) {
        if (user == null) {
            return ResponseEntity.status(401).body(new ApiResponse(false, "未登入"));
        }
        UserResponse userResp = new UserResponse(
                user.getId(), user.getUsername(), user.getEmail(), user.getRole().name());
        return ResponseEntity.ok(new ApiResponse(true, "ok", userResp));
    }
    @PostMapping("/refresh")
    public ResponseEntity<AuthDto.RefreshResponse> refresh(
            @RequestHeader("X-Refresh-Token") String refreshToken) {
        return ResponseEntity.ok(authService.refreshToken(refreshToken));
    }
}