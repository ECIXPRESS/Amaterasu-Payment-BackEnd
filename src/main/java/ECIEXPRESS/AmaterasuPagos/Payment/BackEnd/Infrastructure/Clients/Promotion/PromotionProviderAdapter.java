package ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Infrastructure.Clients.Promotion;


import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Ports.PromotionProvider;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Infrastructure.Clients.BankGateway.Dto.BankGatewayRequests.PayuPaymentRequest;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Infrastructure.Clients.BankGateway.Dto.BankGatewayResponses.PayuPaymentResponse;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Infrastructure.Clients.Promotion.Dto.PromotionRequests.PromotionRequest;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Infrastructure.Clients.Promotion.Dto.PromotionResponses.PromotionResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
@RequiredArgsConstructor
public class PromotionProviderAdapter implements PromotionProvider {

    private final RestTemplate restTemplate;

    @Value("${microservices.promotion.url}")
    private String baseUrl;

    @Value("${microservices.promotion.base-path}")
    private String basePath;

    @Override
    public PromotionResponse applyPromotions(String orderId) {
        try {
            log.info("Processing applicable promotions to: {}", orderId);
            PromotionRequest promotionRequest = new PromotionRequest(orderId);

            HttpHeaders headers = createHeaders();
            HttpEntity<PromotionRequest> entity = new HttpEntity<>(promotionRequest, headers);

            ResponseEntity<PromotionResponse> response = restTemplate.exchange(
                    baseUrl+basePath, HttpMethod.POST, entity, PromotionResponse.class);

            PromotionResponse promotionResponse = response.getBody();

            log.info("Promotion response received for order {}", orderId);

            return promotionResponse;

        } catch (Exception e) {
            log.error("Error applying promotions for order {} Error: {}",orderId, e.getMessage());
            return new PromotionResponse(null,null);
        }
    }
    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Accept", MediaType.APPLICATION_JSON_VALUE);
        return headers;
    }
}