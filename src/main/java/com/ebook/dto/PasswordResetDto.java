package com.ebook.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

public class PasswordResetDto {

    @Data
    public static class ForgotPasswordRequest {
        @NotBlank(message = "Email 不能為空")
        @Email(message = "Email 格式不正確")
        private String email;
    }

    @Data
    public static class ResetPasswordRequest {
        @NotBlank(message = "重設 Token 不能為空")
        private String token;

        @NotBlank(message = "新密碼不能為空")
        @Size(min = 6, max = 100, message = "密碼長度須在 6~100 字之間")
        private String newPassword;
    }
}