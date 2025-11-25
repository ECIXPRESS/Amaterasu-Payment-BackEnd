package ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Application.Services.Event;

import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Application.Dto.Events.PaymentEventWrapper;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Application.Dto.PaymentDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentEventPublisher {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String PAYMENT_EVENTS_CHANNEL = "payment.events";

    public void publishPaymentEvent(String eventType, PaymentDto paymentDto) {
        try {
            PaymentEventWrapper eventWrapper = new PaymentEventWrapper(eventType, paymentDto);
            String eventMessage = objectMapper.writeValueAsString(eventWrapper);

            redisTemplate.convertAndSend(PAYMENT_EVENTS_CHANNEL, eventMessage);

            log.info("Evento de pago publicado - Tipo: {}, OrderId: {}, Status: {}",
                    eventType, paymentDto.orderId(), paymentDto.paymentStatus());

        } catch (Exception e) {
            log.error("Error publicando evento de pago: {}", e.getMessage(), e);
        }
    }

    public void publishPaymentCreated(PaymentDto paymentDto) {
        publishPaymentEvent("payment.created", paymentDto);
    }

    public void publishPaymentProcessed(PaymentDto paymentDto) {
        publishPaymentEvent("payment.processed", paymentDto);
    }

    public void publishPaymentCompleted(PaymentDto paymentDto) {
        publishPaymentEvent("payment.completed", paymentDto);
    }

    public void publishPaymentFailed(PaymentDto paymentDto) {
        publishPaymentEvent("payment.failed", paymentDto);
    }
}