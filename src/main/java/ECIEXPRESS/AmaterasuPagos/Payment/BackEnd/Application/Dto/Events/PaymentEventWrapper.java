package ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Application.Dto.Events;

import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Application.Dto.PaymentDto;
import lombok.Data;

@Data
public class PaymentEventWrapper {
    private String eventId;
    private String eventType;
    private String timestamp;
    private String version = "1.0";
    private PaymentDto data;

    public PaymentEventWrapper() {
        this.eventId = java.util.UUID.randomUUID().toString();
        this.timestamp = java.time.Instant.now().toString();
    }

    public PaymentEventWrapper(String eventType, PaymentDto data) {
        this();
        this.eventType = eventType;
        this.data = data;
    }
}