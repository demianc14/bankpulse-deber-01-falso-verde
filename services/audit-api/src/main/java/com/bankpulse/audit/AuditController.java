package com.bankpulse.audit;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
public class AuditController {
    private final AuditRepository events;

    public AuditController(AuditRepository events) { this.events = events; }

    @PostMapping("/internal/events")
    public ResponseEntity<Map<String, Object>> receive(
            @RequestHeader("X-Event-Id") String eventId,
            @RequestBody Map<String, Object> payload) {
        if (!eventId.equals(String.valueOf(payload.get("eventId")))) {
            return ResponseEntity.badRequest().body(Map.of("accepted", false, "reason", "event id mismatch"));
        }
        if (!events.existsByEventId(eventId)) {
            try {
                events.save(new AuditEvent(
                        eventId,
                        String.valueOf(payload.get("eventType")),
                        String.valueOf(payload.get("aggregateId")),
                        Instant.parse(String.valueOf(payload.get("occurredAt"))),
                        Instant.now(),
                        payload));
            } catch (DuplicateKeyException ignored) {
                // A concurrent retry already persisted the same event.
            }
        }
        return ResponseEntity.accepted().body(Map.of("accepted", true, "eventId", eventId));
    }

    @GetMapping("/api/audit")
    public List<AuditEvent> list() { return events.findTop50ByOrderByReceivedAtDesc(); }
}
