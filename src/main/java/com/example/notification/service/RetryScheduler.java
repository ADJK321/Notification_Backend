package com.example.notification.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class RetryScheduler {

    private static final Logger log = LoggerFactory.getLogger(RetryScheduler.class);

    private final NotificationService notificationService;

    public RetryScheduler(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    // Run every 5 minutes
    @Scheduled(fixedRate = 300000)
    public void retryFailedNotifications() {
        log.info("Scheduled task: Retrying failed notifications");
        notificationService.retryFailedNotifications();
    }
}
