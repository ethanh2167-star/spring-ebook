-- Flyway Migration V1
-- 建立 ebook_db 初始資料表結構

-- ── users ────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS `users` (
  `id`              BIGINT(20)    NOT NULL AUTO_INCREMENT,
  `username`        VARCHAR(50)   NOT NULL,
  `email`           VARCHAR(100)  NOT NULL,
  `password`        VARCHAR(255)  NOT NULL,
  `role`            ENUM('ADMIN','USER') DEFAULT NULL,
  `avatar_url`      VARCHAR(255)  DEFAULT NULL,
  `created_at`      DATETIME(6)   DEFAULT NULL,
  `failed_attempts` INT(11)       NOT NULL DEFAULT 0,
  `account_locked`  TINYINT(1)    NOT NULL DEFAULT 0,
  `lock_time`       DATETIME(6)   DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_username` (`username`),
  UNIQUE KEY `UK_email`    (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci;

-- ── books ─────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS `books` (
  `id`          BIGINT(20)    NOT NULL AUTO_INCREMENT,
  `title`       VARCHAR(255)  NOT NULL,
  `author`      VARCHAR(100)  NOT NULL,
  `description` TEXT          DEFAULT NULL,
  `category`    VARCHAR(50)   DEFAULT NULL,
  `cover_url`   VARCHAR(255)  DEFAULT NULL,
  `file_url`    VARCHAR(255)  DEFAULT NULL,
  `price`       DECIMAL(10,2) NOT NULL,
  `total_pages` INT(11)       DEFAULT NULL,
  `views`       INT(11)       NOT NULL DEFAULT 0,
  `published`   TINYINT(1)    NOT NULL DEFAULT 1,
  `created_at`  DATETIME(6)   DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci;

-- ── bookshelf ─────────────────────────────────────────
CREATE TABLE IF NOT EXISTS `bookshelf` (
  `user_id` BIGINT(20) NOT NULL,
  `book_id` BIGINT(20) NOT NULL,
  PRIMARY KEY (`book_id`, `user_id`),
  CONSTRAINT `FK_bookshelf_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FK_bookshelf_book` FOREIGN KEY (`book_id`) REFERENCES `books` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci;

-- ── orders ────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS `orders` (
  `id`         BIGINT(20)    NOT NULL AUTO_INCREMENT,
  `user_id`    BIGINT(20)    NOT NULL,
  `book_id`    BIGINT(20)    NOT NULL,
  `amount`     DECIMAL(10,2) NOT NULL,
  `status`     ENUM('FAILED','PAID','PENDING','REFUNDED') NOT NULL,
  `trade_no`   VARCHAR(255)  DEFAULT NULL,
  `created_at` DATETIME(6)   DEFAULT NULL,
  `paid_at`    DATETIME(6)   DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_orders_user_book` (`user_id`, `book_id`),
  UNIQUE KEY `UK_orders_trade_no`  (`trade_no`),
  CONSTRAINT `FK_orders_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FK_orders_book` FOREIGN KEY (`book_id`) REFERENCES `books` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci;

-- ── password_reset_tokens ─────────────────────────────
CREATE TABLE IF NOT EXISTS `password_reset_tokens` (
  `id`          BIGINT(20)   NOT NULL AUTO_INCREMENT,
  `user_id`     BIGINT(20)   NOT NULL,
  `token`       VARCHAR(255) NOT NULL,
  `expiry_date` DATETIME(6)  NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_reset_token` (`token`),
  CONSTRAINT `FK_reset_token_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci;

-- ── reading_progress ──────────────────────────────────
CREATE TABLE IF NOT EXISTS `reading_progress` (
  `id`           BIGINT(20) NOT NULL AUTO_INCREMENT,
  `user_id`      BIGINT(20) NOT NULL,
  `book_id`      BIGINT(20) NOT NULL,
  `current_page` INT(11)    DEFAULT NULL,
  `updated_at`   DATETIME(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_progress_user_book` (`user_id`, `book_id`),
  CONSTRAINT `FK_progress_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FK_progress_book` FOREIGN KEY (`book_id`) REFERENCES `books` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci;

-- ── user_sessions ─────────────────────────────────────
CREATE TABLE IF NOT EXISTS `user_sessions` (
  `id`          BIGINT(20)  NOT NULL AUTO_INCREMENT,
  `user_id`     BIGINT(20)  NOT NULL,
  `token`       VARCHAR(36) NOT NULL,
  `device_type` VARCHAR(10) NOT NULL,
  `created_at`  DATETIME    NOT NULL,
  `expires_at`  DATETIME    NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_session_token` (`token`),
  CONSTRAINT `FK_session_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci;
