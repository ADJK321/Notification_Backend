package com.example.notification.service;

import com.example.notification.model.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class InAppService {

    private static final Logger log = LoggerFactory.getLogger(InAppService.class);

    public boolean sendInAppNotification(Notification notification) {
        try {
            // In a real application, this might use WebSockets or a push notification service
            // to deliver real-time notifications to the user's application
            log.info("Sending in-app notification to user {}: {}",
                    notification.getUser().getId(),
                    notification.getContent());

            // Simulate in-app notification with high success rate
            boolean success = Math.random() > 0.05; // 95% success rate

            if (success) {
                log.info("In-app notification sent successfully");
            } else {
                log.error("Failed to send in-app notification");
            }

            return success;
        } catch (Exception e) {
            log.error("Error sending in-app notification: {}", e.getMessage(), e);
            return false;
        }
    }
}
