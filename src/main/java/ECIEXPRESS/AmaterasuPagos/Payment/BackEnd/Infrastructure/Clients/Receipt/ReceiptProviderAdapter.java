package ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Infrastructure.Clients.Receipt;

import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model.Enums.BankResponseCode;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model.Payment;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Ports.ReceiptProvider;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Infrastructure.Clients.Promotion.Dto.PromotionRequests.PromotionRequest;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Infrastructure.Clients.Promotion.Dto.PromotionResponses.PromotionResponse;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Infrastructure.Clients.Receipt.Dto.ReceiptRequests.CreateReceiptRequest;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Infrastructure.Clients.Receipt.Dto.ReceiptResponses.CreateReceiptResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReceiptProviderAdapter implements ReceiptProvider {
    private final RestTemplate restTemplate;

    @Value("${microservices.receipt.url}")
    private String baseUrl;

    @Value("${microservices.receipt.base-path}")
    private String basePath;

    @Override
    public CreateReceiptResponse createReceipt(Payment payment) {
        try {
            log.info("Processing Receipt for order: {}", payment.getOrderId());
            CreateReceiptRequest receiptRequest = mapToReceiptRequest(payment);

            HttpHeaders headers = createHeaders();
            HttpEntity<CreateReceiptRequest> entity = new HttpEntity<>(receiptRequest, headers);

            ResponseEntity<CreateReceiptResponse> response = restTemplate.exchange(
                    baseUrl+basePath, HttpMethod.POST, entity, CreateReceiptResponse.class);

            CreateReceiptResponse receiptResponse = response.getBody();

            log.info("Request response received for order {}", payment.getOrderId());

            return receiptResponse;

        } catch (Exception e) {
            log.error("Error processing receipt for order {} Error: {}",payment.getOrderId(), e.getMessage());
            return new CreateReceiptResponse(null,null, null,0,null);
        }
    }

    private CreateReceiptRequest mapToReceiptRequest(Payment payment) {

        return new CreateReceiptRequest(
                payment.getOrderId(),
                payment.getClientId(),
                payment.getStoreId(),
                payment.getFinalAmount(),
                payment.getPaymentMethod(),
                payment.getPaymentStatus(),
                payment.getTimeStamps(),
                payment.getAppliedPromotions());
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Accept", MediaType.APPLICATION_JSON_VALUE);
        return headers;
    }
}
