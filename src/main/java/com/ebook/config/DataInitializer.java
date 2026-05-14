package com.ebook.config;

import com.ebook.model.Book;
import com.ebook.model.User;
import com.ebook.repository.BookRepository;
import com.ebook.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Profile("dev")
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final BookRepository bookRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${ADMIN_EMAIL:admin@ebook.com}")
    private String adminEmail;

    @Value("${ADMIN_PASSWORD:changeme}")
    private String adminPassword;

    @Override
    public void run(String... args) {
        if (!userRepository.existsByEmail(adminEmail)) {
            User admin = new User("admin", adminEmail, passwordEncoder.encode(adminPassword));
            admin.setRole(User.Role.ADMIN);
            userRepository.save(admin);
        }

        if (bookRepository.count() == 0) {
            bookRepository.save(createBook("深入淺出Java", "作者一",
                "Java 程式設計入門到進階，適合初學者。",
                "https://picsum.photos/seed/java/300/400", "程式設計", 320,
                "/upload/head-first-java.pdf"));
            bookRepository.save(createBook("Python 資料科學", "作者二",
                "用 Python 進行資料分析與機器學習。",
                "https://picsum.photos/seed/python/300/400", "資料科學", 280,
                "/upload/Pythondatascience.pdf"));
            bookRepository.save(createBook("網頁設計實務", "作者三",
                "HTML、CSS、JavaScript 完整教學。",
                "https://picsum.photos/seed/web/300/400", "程式設計", 250,
                "/upload/WebDesignPractice.pdf"));
            bookRepository.save(createBook("資料庫系統概論", "作者四",
                "SQL 與資料庫設計完全指南。",
                "https://picsum.photos/seed/db/300/400", "資料庫", 300,
                "/upload/IntroductiontoDatabaseSystems.pdf"));
            bookRepository.save(createBook("演算法導論", "作者五",
                "從基礎到進階的演算法解說。",
                "https://picsum.photos/seed/algo/300/400", "電腦科學", 420,
                "/upload/IntroductiontoAlgorithms.pdf"));
            bookRepository.save(createBook("系統分析與設計", "作者六",
                "軟體工程與系統分析完整教材。",
                "https://picsum.photos/seed/sa/300/400", "軟體工程", 360,
                "/upload/SystemAnalysisandDesign.pdf"));
        }
    }
    private Book createBook(String title, String author, String desc,
            String cover, String cat, int pages, String filePath) {
        Book b = new Book();
        b.setTitle(title);
        b.setAuthor(author);
        b.setDescription(desc);
        b.setCoverUrl(cover);
        b.setCategory(cat);
        b.setTotalPages(pages);
        b.setFileUrl(filePath);
        b.setPublished(true);
        return b;
    }
}