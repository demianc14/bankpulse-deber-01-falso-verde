package com.bankpulse.audit;

import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface AuditRepository extends MongoRepository<AuditEvent, String> {
    boolean existsByEventId(String eventId);
    List<AuditEvent> findTop50ByOrderByReceivedAtDesc();
}
