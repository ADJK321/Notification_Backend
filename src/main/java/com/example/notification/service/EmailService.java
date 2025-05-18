package com.example.notification.service;

import com.example.notification.model.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    public boolean sendEmail(Notification notification) {
        try {
            // In a real application, this would use an email client library
            // like JavaMail to send actual emails
            log.info("Sending email to user {}: Subject: '{}', Content: '{}'",
                    notification.getUser() != null ? notification.getUser().getEmail() : "Unknown",
                    notification.getSubject(),
                    notification.getContent());

            // Simulate email sending with random success/failure
            boolean success = Math.random() > 0.1; // 90% success rate

            if (success) {
                log.info("Email sent successfully");
            } else {
                log.error("Failed to send email");
            }

            return success;
        } catch (Exception e) {
            log.error("Error sending email: {}", e.getMessage(), e);
            return false;
        }
    }
}
