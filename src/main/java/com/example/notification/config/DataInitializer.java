package com.example.notification.config;

import com.example.notification.model.User;
import com.example.notification.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
public class DataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;

    // Manual constructor instead of @RequiredArgsConstructor
    public DataInitializer(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Bean
    @Profile("!test") // Don't run in test profile
    public CommandLineRunner initData() {
        return args -> {
            // Only add sample data if DB is empty
            if (userRepository.count() == 0) {
                log.info("Initializing sample data");

                User user1 = new User();
                user1.setName("John Doe");
                user1.setEmail("john.doe@example.com");
                user1.setPhoneNumber("+1234567890");
                user1.setEmailEnabled(true);
                user1.setSmsEnabled(true);
                user1.setInAppEnabled(true);

                User user2 = new User();
                user2.setName("Jane Smith");
                user2.setEmail("jane.smith@example.com");
                user2.setPhoneNumber("+0987654321");
                user2.setEmailEnabled(true);
                user2.setSmsEnabled(false);
                user2.setInAppEnabled(true);

                userRepository.save(user1);
                userRepository.save(user2);

                log.info("Sample data initialized with {} users", userRepository.count());
            }
        };
    }
}
