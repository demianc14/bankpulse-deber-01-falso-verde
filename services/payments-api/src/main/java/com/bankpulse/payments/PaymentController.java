package com.bankpulse.payments;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class PaymentController {
    private final PaymentService service;
    private final PaymentRepository payments;
    private final OutboxRepository outbox;

    public PaymentController(PaymentService service, PaymentRepository payments, OutboxRepository outbox) {
        this.service = service;
        this.payments = payments;
        this.outbox = outbox;
    }

    @PostMapping("/payments")
    public ResponseEntity<Payment> create(
            @RequestHeader("X-Idempotency-Key") @NotBlank String idempotencyKey,
            @Valid @RequestBody PaymentRequest request) {
        return ResponseEntity.ok(service.create(idempotencyKey, request));
    }

    @GetMapping("/payments")
    public List<Payment> list() { return payments.findTop50ByOrderByCreatedAtDesc(); }

    @GetMapping("/outbox")
    public Map<String, Object> outbox() {
        List<OutboxEvent> pendingEvents = outbox.findTop50ByPublishedFalseOrderByCreatedAtAsc();
        return Map.of("pending", outbox.countByPublishedFalse(), "events", pendingEvents);
    }

    public record PaymentRequest(
            @NotBlank String account,
            @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
            @NotBlank @Pattern(regexp = "[A-Za-z]{3}") String currency) {}
}
