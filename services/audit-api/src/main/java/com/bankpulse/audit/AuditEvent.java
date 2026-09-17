package com.bankpulse.audit;

import java.time.Instant;
import java.util.Map;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("audit_events")
public class AuditEvent {
    @Id
    private String id;
    @Indexed(unique = true)
    private String eventId;
    private String eventType;
    private String aggregateId;
    private Instant occurredAt;
    private Instant receivedAt;
    private Map<String, Object> payload;

    public AuditEvent(String eventId, String eventType, String aggregateId, Instant occurredAt, Instant receivedAt, Map<String, Object> payload) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.aggregateId = aggregateId;
        this.occurredAt = occurredAt;
        this.receivedAt = receivedAt;
        this.payload = payload;
    }

    public String getId() { return id; }
    public String getEventId() { return eventId; }
    public String getEventType() { return eventType; }
    public String getAggregateId() { return aggregateId; }
    public Instant getOccurredAt() { return occurredAt; }
    public Instant getReceivedAt() { return receivedAt; }
    public Map<String, Object> getPayload() { return payload; }
}
