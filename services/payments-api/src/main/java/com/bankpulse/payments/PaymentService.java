package com.bankpulse.payments;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentService {
    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

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
        // Correccion (Deber 1, PR #2): la Idempotency-Key del cliente se usa TAL CUAL
        // para buscar y persistir. El intento anterior le agregaba un sufijo de
        // correlacion antes de usarla, lo que rompia la deduplicacion (dos
        // solicitudes con la misma clave terminaban comparando/guardando claves
        // distintas). La traza de correlacion ahora es solo para logging y no
        // participa en la clave de negocio.
        String traceId = UUID.randomUUID().toString();
        log.info("payment.create idempotencyKey={} traceId={}", idempotencyKey, traceId);
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
