package com.example.notification.service;

import com.example.notification.model.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class SmsService {

    private static final Logger log = LoggerFactory.getLogger(SmsService.class);

    public boolean sendSms(Notification notification) {
        try {
            // In a real application, this would use an SMS API provider
            // like Twilio or AWS SNS
            log.info("Sending SMS to {}: {}",
                    notification.getUser().getPhoneNumber(),
                    notification.getContent());

            // Simulate SMS sending with random success/failure
            boolean success = Math.random() > 0.2; // 80% success rate

            if (success) {
                log.info("SMS sent successfully");
            } else {
                log.error("Failed to send SMS");
            }

            return success;
        } catch (Exception e) {
            log.error("Error sending SMS: {}", e.getMessage(), e);
            return false;
        }
    }
}
