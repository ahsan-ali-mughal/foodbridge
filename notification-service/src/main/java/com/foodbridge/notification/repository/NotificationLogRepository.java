package com.foodbridge.notification.repository;

import com.foodbridge.notification.document.NotificationLog;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface NotificationLogRepository extends MongoRepository<NotificationLog, String> {
}
