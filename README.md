
# Notification Service

A Spring Boot application that provides a notification system to send email, SMS, and in-app notifications to users.

## Features

- RESTful API for sending and retrieving notifications
- Support for multiple notification types (Email, SMS, In-App)
- Asynchronous processing with RabbitMQ queue
- Automatic retry mechanism for failed notifications
- User preference settings for opting out of notification types
- Transactional processing to ensure data consistency

## Prerequisites

- Java 17 or higher
- Maven 3.6 or higher
- RabbitMQ server (for message queuing)

## Technology Stack

- Spring Boot 3.x
- Spring Data JPA
- Spring AMQP (for RabbitMQ integration)
- Hibernate ORM
- H2 Database (for development)

## Getting Started


### 1. Install RabbitMQ

You need to have RabbitMQ installed and running. Follow these steps:

#### 🐇 Step 1: Install Erlang (Required)

RabbitMQ is built on Erlang, so you **must install Erlang first**.

* Download the latest [Erlang/OTP Windows Installer](https://www.erlang.org/downloads).
* Install it and make sure to **add Erlang to your system PATH**.

#### 🐇 Step 2: Install RabbitMQ Server

* Download the RabbitMQ Windows installer from the [official RabbitMQ website](https://www.rabbitmq.com/download.html).
* Install it after Erlang is successfully installed.
* After installation, the RabbitMQ server is installed as a **Windows service**.

#### ✅ Step 3: Enable the RabbitMQ Management Plugin (Optional but recommended)

```bash
rabbitmq-plugins enable rabbitmq_management
```

Now you can access the RabbitMQ Dashboard at:
**[http://localhost:15672](http://localhost:15672)**
(Default username: `guest`, password: `guest`)

---

### 🟢 Start RabbitMQ

To start RabbitMQ as a service:

```bash
net start RabbitMQ
```

OR

```bash
rabbitmq-server
```

> If installed via installer, it should run automatically as a background service.

---

### 🔴 Stop RabbitMQ

To stop RabbitMQ:

```bash
net stop RabbitMQ
```

OR (if started manually):

Press `Ctrl + C` in the terminal where it’s running.

---

### 🛠 Verify RabbitMQ is Running

Open a browser and go to [http://localhost:15672](http://localhost:15672).
If the dashboard opens, you’re good to go!


### 2. Clone the repository

```bash
git clone https://github.com/ADJK321/Notification_Backend.git
cd Notification_Backend
```

### 3. Build the application

```bash
mvn clean package
```

### 4. Run the application

```bash
mvn spring-boot:run
```



The application will be available at `http://localhost:8080`  -> Error will show BUT USE POSTMAN Everything is fine .

## API Endpoints

### Send a Notification

```
POST /api/notifications
```

Request Body:
```json
{
  "userId": 1,
  "type": "EMAIL",
  "subject": "Welcome",
  "content": "Welcome to our platform!",
  "priority": "NORMAL"
}
```

### Get User Notifications

```
GET /api/users/1/notifications
```

## Postman Testing

You can use [Postman](https://www.postman.com/) to verify the endpoints:

- **POST**: `http://localhost:8080/api/notifications`
- **GET**: `http://localhost:8080/api/users/1/notifications`

## Architecture Overview

The system follows a layered architecture:

1. **Controller Layer**: Handles HTTP requests and responses
2. **Service Layer**: Contains business logic for notification processing
3. **Repository Layer**: Interfaces with the database
4. **Model Layer**: Defines the data entities

### Notification Workflow

1. Client sends a notification request
2. The request is validated and saved to the database
3. Notification ID is published to RabbitMQ queue
4. NotificationProcessor consumes from the queue and processes the notification
5. Based on the notification type and user preferences, the appropriate service is called
6. Status is updated to SENT or FAILED
7. Failed notifications are automatically retried based on the retry schedule

## Database Schema

### Users Table
- id (PK)
- name
- email
- phone_number
- email_enabled (boolean)
- sms_enabled (boolean)
- in_app_enabled (boolean)

### Notifications Table
- id (PK)
- user_id (FK)
- type (enum: EMAIL, SMS, IN_APP)
- subject
- content
- status (enum: PENDING, SENT, FAILED)
- priority (enum: LOW, NORMAL, HIGH)
- retry_count
- created_at
- updated_at
- sent_at

## Common Issues and Solutions

### LazyInitializationException

If you encounter a Hibernate LazyInitializationException when accessing User properties in the NotificationProcessor:

1. Add `@Transactional` to the processNotification method
2. This ensures the Hibernate session remains open during processing

```java
@RabbitListener(queues = "notification.queue")
@Transactional
public void processNotification(Long notificationId) {
    // Method implementation
}
```

## Assumptions Made

1. Users are pre-registered in the system
2. The notification service focuses solely on delivery, not content generation
3. Each notification type has its own handling service
4. Users can opt out of specific notification types
5. Failed notifications should be retried automatically
6. For demonstration purposes, actual delivery is simulated

## Future Improvements

1. Add authentication and authorization
2. Implement actual email and SMS provider integrations
3. Add notification templates with variable substitution
4. Implement read/unread status for in-app notifications
5. Add pagination for retrieving notifications
6. Implement more sophisticated retry mechanisms with exponential backoff
7. Add batch processing capabilities for sending notifications to multiple users
8. Implement notification analytics (delivery rates, open rates, etc.)
