package com.ebook.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

public class AuthDto {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RegisterRequest {
        @jakarta.validation.constraints.NotBlank(message = "使用者名稱不能為空")
        @jakarta.validation.constraints.Size(min = 2, max = 50, message = "使用者名稱長度須在 2~50 字之間")
        private String username;

        @jakarta.validation.constraints.NotBlank(message = "Email 不能為空")
        @jakarta.validation.constraints.Email(message = "Email 格式不正確")
        private String email;

        @jakarta.validation.constraints.NotBlank(message = "密碼不能為空")
        @jakarta.validation.constraints.Size(min = 6, max = 100, message = "密碼長度須在 6~100 字之間")
        private String password;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoginRequest {
        @jakarta.validation.constraints.NotBlank(message = "Email 不能為空")
        @jakarta.validation.constraints.Email(message = "Email 格式不正確")
        private String email;

        @jakarta.validation.constraints.NotBlank(message = "密碼不能為空")
        private String password;

        private String deviceType;
    }

    @Data
    @AllArgsConstructor
    public static class LoginResponse {
        private boolean success;
        private String message;
        private UserResponse user;
        private String token;
        private String refreshToken;
    }

    @Data
    @AllArgsConstructor
    public static class RefreshResponse {
        private boolean success;
        private String token;
    }

    @Data
    @AllArgsConstructor
    public static class UserResponse {
        private Long id;
        private String username;
        private String email;
        private String role;
    }

    @Data
    @AllArgsConstructor
    public static class ApiResponse {
        private boolean success;
        private String message;
        private Object data;

        public ApiResponse(boolean success, String message) {
            this.success = success;
            this.message = message;
            this.data = null;
        }
    }
}