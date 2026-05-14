# Spring Ebook Platform

基於 Spring Boot 4 開發的電子書閱讀平台，實作使用者認證、書籍管理、書架、閱讀進度追蹤與訂單金流等完整功能。

---

## 功能特色

- **使用者認證**：註冊 / 登入 / 登出，UUID Session Token + Refresh Token 機制
- **書籍瀏覽**：分類搜尋、分頁查詢、瀏覽次數追蹤（IP 去重）
- **個人書架**：加入 / 移除書架，DB 層級 EXISTS 查詢
- **閱讀進度**：記錄當前頁數與閱讀百分比
- **訂單系統**：建立訂單、ECPay 金流串接（展示用）
- **後台管理**：書籍 CRUD、封面圖片上傳（RBAC 權限控管）
- **密碼重設**：Gmail SMTP 寄送重設信件
- **API 文件**：Swagger UI（Springdoc OpenAPI 3.0）

---

## 技術棧

| 類別 | 技術 |
|------|------|
| 語言 | Java 21 |
| 框架 | Spring Boot 4.0.5 |
| 安全 | Spring Security、UUID Session Token |
| 資料層 | Spring Data JPA、Hibernate、Flyway |
| 資料庫 | MariaDB |
| 工具 | Lombok、Springdoc OpenAPI 3.0 |
| 前端 | 原生 JavaScript（SPA 架構） |

---

## 系統架構

### 分層架構

```
Controller → Service → Repository → Entity
               ↕
             DTO / Exception / Config / Security
```

### UML 完整圖

> 包含類別圖、套件圖、ER Diagram、時序圖、活動圖、排程圖

![電子書系統 UML 完整圖](uml.png)

---

## 啟動方式

### 前置需求

- Java 21+
- MariaDB（預設 port 3307）
- Maven 3.8+

### 步驟

**1. Clone 專案**

```bash
git clone https://github.com/ethanh2167-star/spring-ebook.git
cd spring-ebook
```

**2. 複製設定檔**

```bash
cp src/main/resources/application-example.yml src/main/resources/application.yml
```

依實際環境填入以下設定：

| 欄位 | 說明 |
|------|------|
| `datasource.url` | MariaDB 連線字串 |
| `datasource.username` | 資料庫帳號 |
| `datasource.password` | 資料庫密碼 |
| `mail.username` | Gmail 帳號 |
| `mail.password` | Gmail 應用程式密碼 |

**3. 設定環境變數（PowerShell）**

```powershell
$env:DB_URL="jdbc:mariadb://localhost:3307/ebook_db?characterEncoding=UTF-8&useSSL=false"
$env:ADMIN_EMAIL="admin@ebook.com"
$env:ADMIN_PASSWORD="yourpassword"
$env:MAIL_USERNAME="your@gmail.com"
$env:MAIL_PASSWORD="yourpassword"
```

**4. 啟動**

```bash
mvn spring-boot:run
```

開啟瀏覽器：[http://localhost:8080](http://localhost:8080)

---

## API 文件

啟動後前往：[http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

---

## 設計亮點

### Session Token vs JWT

本專案採用 UUID Session Token 儲存於資料庫，而非 JWT，主要考量：

- **可撤銷性**：登出、帳號封鎖時可立即失效，JWT 無法做到
- **簡單性**：不需管理 Token 黑名單或輪替金鑰
- **可接受的效能取捨**：每次請求多一次 DB 查詢，但對此規模的應用影響極小

### @Transactional 與 Rollback

訂單建立使用 `@Transactional`，確保訂單與明細同步寫入，任一失敗自動 Rollback。

### @Retryable 處理樂觀鎖

登入時有 Optimistic Locking 衝突風險，使用 `@Retryable` 自動重試，避免手動 try-catch 污染業務邏輯。

---

## 專案結構

```
src/main/java/com/ebook/
├── config/          # SecurityConfig、WebConfig、DataInitializer
├── controller/      # AuthController、BookController、OrderController
│                    # ShelfController、ProgressController、AdminBookController
├── service/         # 各業務邏輯服務
├── repository/      # Spring Data JPA Repository
├── model/           # Entity（User、Book、Order、UserSession...）
├── dto/             # BookDto、OrderDto、AuthDto、UserDto
├── exception/       # GlobalExceptionHandler、CustomException
└── security/        # TokenAuthFilter、CurrentUserArgumentResolver
```

---

## 未來優化方向

- 雲端檔案儲存（書籍封面 / PDF）
- Docker 容器化
- CI/CD 自動部署

---

## 聲明

本專案為學校練習用途。部分書籍封面圖片來源自 [博客來](https://www.books.com.tw/)，版權歸原著作權人所有，僅供展示使用，無任何商業目的。

---

## 作者

**Ethan** — [@ethanh2167-star](https://github.com/ethanh2167-star)