package com.ebook.service;
import com.ebook.model.User;
import com.ebook.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class LoginAttemptService {
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private final UserRepository userRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleFailedLogin(User user) {
        User freshUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new RuntimeException("使用者不存在"));
        int attempts = freshUser.getFailedAttempts() + 1;
        freshUser.setFailedAttempts(attempts);
        if (attempts >= MAX_FAILED_ATTEMPTS) {
            freshUser.setAccountLocked(true);
            freshUser.setLockTime(LocalDateTime.now());
        }
        userRepository.save(freshUser);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void resetFailedAttempts(User user) {
        User freshUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new RuntimeException("使用者不存在"));
        freshUser.setFailedAttempts(0);
        freshUser.setAccountLocked(false);
        freshUser.setLockTime(null);
        userRepository.save(freshUser);
    }
}