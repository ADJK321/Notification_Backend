package com.example.notification.service;

import com.example.notification.model.Notification;
import com.example.notification.repository.NotificationRepository;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationProcessor {

    private static final Logger log = LoggerFactory.getLogger(NotificationProcessor.class);

    private final NotificationRepository notificationRepository;
    private final NotificationService notificationService;
    private final EmailService emailService;
    private final SmsService smsService;
    private final InAppService inAppService;

    public NotificationProcessor(NotificationRepository notificationRepository, NotificationService notificationService, EmailService emailService, SmsService smsService, InAppService inAppService) {
        this.notificationRepository = notificationRepository;
        this.notificationService = notificationService;
        this.emailService = emailService;
        this.smsService = smsService;
        this.inAppService = inAppService;
    }

    @RabbitListener(queues = "notification.queue")
    @Transactional
    public void processNotification(Long notificationId) {
        log.info("Processing notification: {}", notificationId);

        try {
            Notification notification = notificationRepository.findById(notificationId)
                    .orElseThrow(() -> new EntityNotFoundException("Notification not found with ID: " + notificationId));

            boolean success = false;

            // Process based on notification type
            switch (notification.getType()) {
                case EMAIL:
                    if (notification.getUser().isEmailEnabled()) {
                        success = emailService.sendEmail(notification);
                    } else {
                        log.info("Email notifications disabled for user {}", notification.getUser().getId());
                        success = true; // Mark as success since user has opted out
                    }
                    break;

                case SMS:
                    if (notification.getUser().isSmsEnabled()) {
                        success = smsService.sendSms(notification);
                    } else {
                        log.info("SMS notifications disabled for user {}", notification.getUser().getId());
                        success = true; // Mark as success since user has opted out
                    }
                    break;

                case IN_APP:
                    if (notification.getUser().isInAppEnabled()) {
                        success = inAppService.sendInAppNotification(notification);
                    } else {
                        log.info("In-app notifications disabled for user {}", notification.getUser().getId());
                        success = true; // Mark as success since user has opted out
                    }
                    break;

                default:
                    log.error("Unknown notification type: {}", notification.getType());
                    success = false;
            }

            // Update notification status based on result
            if (success) {
                notificationService.markNotificationAsSent(notificationId);
            } else {
                notificationService.markNotificationAsFailedWithRetry(notificationId);
            }

        } catch (Exception e) {
            log.error("Error processing notification {}: {}", notificationId, e.getMessage(), e);
        }
    }
}