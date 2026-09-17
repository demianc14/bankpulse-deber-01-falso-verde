package com.bankpulse.payments;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentService {
    private final PaymentRepository payments;
    private final OutboxRepository outbox;
    private final ObjectMapper objectMapper;

    public PaymentService(PaymentRepository payments, OutboxRepository outbox, ObjectMapper objectMapper) {
        this.payments = payments;
        this.outbox = outbox;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public Payment create(String idempotencyKey, PaymentController.PaymentRequest request) {
        return payments.findByIdempotencyKey(idempotencyKey).orElseGet(() -> persist(idempotencyKey, request));
    }

    private Payment persist(String idempotencyKey, PaymentController.PaymentRequest request) {
        Instant now = Instant.now();
        Payment payment = new Payment(UUID.randomUUID().toString(), idempotencyKey, request.account(), request.amount(), request.currency().toUpperCase(), "ACCEPTED", now);
        payments.save(payment);

        String eventId = UUID.randomUUID().toString();
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("eventId", eventId);
        event.put("eventType", "PAYMENT_CREATED");
        event.put("aggregateId", payment.getId());
        event.put("occurredAt", now);
        event.put("account", payment.getAccount());
        event.put("amount", payment.getAmount());
        event.put("currency", payment.getCurrency());
        try {
            outbox.save(new OutboxEvent(eventId, payment.getId(), "PAYMENT_CREATED", objectMapper.writeValueAsString(event), now));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not serialize payment event", e);
        }
        return payment;
    }
}
