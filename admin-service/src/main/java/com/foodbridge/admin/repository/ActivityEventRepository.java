package com.foodbridge.admin.repository;

import com.foodbridge.admin.document.ActivityEvent;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ActivityEventRepository extends MongoRepository<ActivityEvent, String> {

    List<ActivityEvent> findAllByOrderByOccurredAtDesc(Pageable pageable);

    long countByEventType(String eventType);
}
