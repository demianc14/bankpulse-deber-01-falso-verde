package com.bankpulse.payments;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

@Component
public class OutboxPublisher {
    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);
    private final OutboxRepository outbox;
    private final RestClient auditClient;

    public OutboxPublisher(OutboxRepository outbox, RestClient.Builder builder, @Value("${audit.base-url}") String auditBaseUrl) {
        this.outbox = outbox;
        this.auditClient = builder.baseUrl(auditBaseUrl).build();
    }

    @Scheduled(fixedDelayString = "${outbox.fixed-delay-ms}")
    @Transactional
    public void publishPending() {
        for (OutboxEvent event : outbox.findTop50ByPublishedFalseOrderByCreatedAtAsc()) {
            try {
                auditClient.post()
                        .uri("/internal/events")
                        .header("X-Event-Id", event.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(event.getPayload())
                        .retrieve()
                        .toBodilessEntity();
                event.markPublished();
                log.info("Published eventId={} aggregateId={}", event.getId(), event.getAggregateId());
            } catch (Exception ex) {
                event.markFailed(ex.getMessage());
                log.warn("Audit unavailable; eventId={} remains in outbox, attempt={}", event.getId(), event.getAttempts());
            }
        }
    }
}
