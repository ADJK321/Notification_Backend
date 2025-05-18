package com.example.notification.service;

import com.example.notification.dto.NotificationRequest;
import com.example.notification.model.Notification;
import com.example.notification.model.NotificationStatus;
import com.example.notification.repository.NotificationRepository;
import com.example.notification.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    private static final String NOTIFICATION_QUEUE = "notification.queue";
    private static final int MAX_RETRY_COUNT = 3;

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final RabbitTemplate rabbitTemplate;

    public NotificationService(NotificationRepository notificationRepository,
                               UserRepository userRepository,
                               RabbitTemplate rabbitTemplate) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.rabbitTemplate = rabbitTemplate;
    }

    @Transactional
    public Notification createNotification(NotificationRequest request) {
        log.info("Creating notification for user: {}", request.getUserId());

        // Verify user exists
        var user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + request.getUserId()));

        // Create notification
        var notification = Notification.builder()
                .user(user)
                .type(request.getType())
                .content(request.getContent())
                .subject(request.getSubject())
                .build();

        // Save notification
        notification = notificationRepository.save(notification);
        log.info("Notification created with ID: {}", notification.getId());

        try {
            // Send to queue for processing
            rabbitTemplate.convertAndSend(NOTIFICATION_QUEUE, notification.getId());
            log.info("Notification ID {} sent to queue for processing", notification.getId());
        } catch (Exception e) {
            log.error("Failed to send notification to queue: {}", e.getMessage(), e);
            // Optionally process immediately as fallback
            // processNotificationDirectly(notification);
        }

        return notification;
    }

    @Transactional(readOnly = true)
    public List<Notification> getUserNotifications(Long userId) {
        log.info("Fetching notifications for user: {}", userId);

        // Verify user exists
        if (!userRepository.existsById(userId)) {
            throw new EntityNotFoundException("User not found with ID: " + userId);
        }

        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional
    public void markNotificationAsSent(Long notificationId) {
        log.info("Marking notification {} as sent", notificationId);

        var notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new EntityNotFoundException("Notification not found with ID: " + notificationId));

        notification.setStatus(NotificationStatus.SENT);
        notification.setSentAt(LocalDateTime.now());

        notificationRepository.save(notification);
        log.info("Notification {} marked as SENT", notificationId);
    }

    @Transactional
    public void markNotificationAsFailedWithRetry(Long notificationId) {
        log.info("Marking notification {} as failed for retry", notificationId);

        var notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new EntityNotFoundException("Notification not found with ID: " + notificationId));

        notification.setRetryCount(notification.getRetryCount() + 1);

        if (notification.getRetryCount() >= MAX_RETRY_COUNT) {
            log.warn("Notification {} has reached max retry count ({}). Marking as FAILED",
                    notificationId, MAX_RETRY_COUNT);
            notification.setStatus(NotificationStatus.FAILED);
        } else {
            log.info("Scheduling notification {} for retry. Retry count: {}",
                    notificationId, notification.getRetryCount());
            notification.setStatus(NotificationStatus.RETRY);
        }

        notificationRepository.save(notification);
    }

    @Transactional
    public void retryFailedNotifications() {
        log.info("Looking for notifications to retry");

        List<Notification> retryNotifications = notificationRepository.findByStatus(NotificationStatus.RETRY);

        if (retryNotifications.isEmpty()) {
            log.info("No notifications found for retry");
            return;
        }

        log.info("Found {} notifications to retry", retryNotifications.size());

        for (Notification notification : retryNotifications) {
            log.info("Requeueing notification {} for retry attempt {}",
                    notification.getId(), notification.getRetryCount());

            // Reset to pending state before requeueing
            notification.setStatus(NotificationStatus.PENDING);
            notificationRepository.save(notification);

            try {
                // Requeue for processing
                rabbitTemplate.convertAndSend(NOTIFICATION_QUEUE, notification.getId());
            } catch (Exception e) {
                log.error("Failed to requeue notification {}: {}", notification.getId(), e.getMessage(), e);
                notification.setStatus(NotificationStatus.RETRY); // Reset back to retry
                notificationRepository.save(notification);
            }
        }
    }
}