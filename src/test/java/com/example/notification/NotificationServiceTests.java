package com.example.notification.service;

import com.example.notification.dto.NotificationRequest;
import com.example.notification.model.Notification;
import com.example.notification.model.NotificationStatus;
import com.example.notification.model.NotificationType;
import com.example.notification.model.User;
import com.example.notification.repository.NotificationRepository;
import com.example.notification.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
public class NotificationServiceTests {

	@Mock
	private UserRepository userRepository;

	@Mock
	private NotificationRepository notificationRepository;

	@Mock
	private RabbitTemplate rabbitTemplate;

	@InjectMocks
	private NotificationService notificationService;

	private User testUser;
	private Notification testNotification;

	@BeforeEach
	void setUp() {
		testUser = new User();
		testUser.setId(1L);
		testUser.setName("Test User");
		testUser.setEmail("test@example.com");
		testUser.setPhoneNumber("+1234567890");

		testNotification = Notification.builder()
				.id(1L)
				.user(testUser)
				.type(NotificationType.EMAIL)
				.content("Test notification content")
				.subject("Test Subject")
				.status(NotificationStatus.PENDING)
				.retryCount(0)
				.build();

		when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
		when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);
		doNothing().when(rabbitTemplate).convertAndSend(anyString(), anyLong());
	}

	@Test
	void testCreateNotification() {
		NotificationRequest request = new NotificationRequest();
		request.setUserId(1L);
		request.setType(NotificationType.EMAIL);
		request.setContent("Test notification content");
		request.setSubject("Test Subject");

		Notification result = notificationService.createNotification(request);

		assertNotNull(result);
		assertEquals(NotificationStatus.PENDING, result.getStatus());
		verify(notificationRepository, times(1)).save(any(Notification.class));
		verify(rabbitTemplate, times(1)).convertAndSend(anyString(), anyLong());
	}

	@Test
	void testGetUserNotifications() {
		Notification notification1 = Notification.builder()
				.id(1L)
				.user(testUser)
				.content("Notification 1")
				.build();

		Notification notification2 = Notification.builder()
				.id(2L)
				.user(testUser)
				.content("Notification 2")
				.build();

		List<Notification> notificationList = Arrays.asList(notification1, notification2);

		when(userRepository.existsById(1L)).thenReturn(true);
		when(notificationRepository.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(notificationList);

		List<Notification> result = notificationService.getUserNotifications(1L);

		assertEquals(2, result.size());
		verify(notificationRepository, times(1)).findByUserIdOrderByCreatedAtDesc(1L);
	}

	@Test
	void testMarkNotificationAsSent() {
		when(notificationRepository.findById(1L)).thenReturn(Optional.of(testNotification));

		notificationService.markNotificationAsSent(1L);

		assertEquals(NotificationStatus.SENT, testNotification.getStatus());
		assertNotNull(testNotification.getSentAt());
		verify(notificationRepository, times(1)).save(testNotification);
	}

	@Test
	void testMarkNotificationAsFailedWithRetry() {
		when(notificationRepository.findById(1L)).thenReturn(Optional.of(testNotification));

		notificationService.markNotificationAsFailedWithRetry(1L);

		assertEquals(NotificationStatus.RETRY, testNotification.getStatus());
		assertEquals(1, testNotification.getRetryCount());
		verify(notificationRepository, times(1)).save(testNotification);
	}

	@Test
	void testRetryFailedNotifications() {
		Notification notification1 = Notification.builder()
				.id(1L)
				.status(NotificationStatus.RETRY)
				.build();

		Notification notification2 = Notification.builder()
				.id(2L)
				.status(NotificationStatus.RETRY)
				.build();

		List<Notification> retryList = Arrays.asList(notification1, notification2);

		when(notificationRepository.findByStatus(NotificationStatus.RETRY)).thenReturn(retryList);

		notificationService.retryFailedNotifications();

		verify(rabbitTemplate, times(2)).convertAndSend(anyString(), anyLong());
	}
}