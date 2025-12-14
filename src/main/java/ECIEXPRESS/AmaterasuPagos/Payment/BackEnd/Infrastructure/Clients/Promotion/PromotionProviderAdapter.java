package ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Infrastructure.Clients.Promotion;


import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Ports.PromotionProvider;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Infrastructure.Clients.Promotion.Dto.PromotionRequests.ApplyPromotionRequest;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Infrastructure.Clients.Promotion.Dto.PromotionResponses.ApplyPromotionResponse;
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

    @Value("${gateway.url:http://gateway:8081/api}")
    private String baseUrl;

    @Value("${microservices.promotion.base-path}")
    private String basePath;

    @Override
    public ApplyPromotionResponse applyPromotions(String orderId) {
        try {
            log.info("Processing applicable promotions to: {}", orderId);
            ApplyPromotionRequest applyPromotionRequest = new ApplyPromotionRequest(orderId);

            HttpHeaders headers = createHeaders();
            HttpEntity<ApplyPromotionRequest> entity = new HttpEntity<>(applyPromotionRequest, headers);

            ResponseEntity<ApplyPromotionResponse> response = restTemplate.exchange(
                    baseUrl+basePath, HttpMethod.POST, entity, ApplyPromotionResponse.class);

            ApplyPromotionResponse applyPromotionResponse = response.getBody();

            log.info("Promotion response received for order {}", orderId);

            return applyPromotionResponse;

        } catch (Exception e) {
            log.error("Error applying promotions for order {} Error: {}",orderId, e.getMessage());
            return new ApplyPromotionResponse(0,null);
        }
    }
    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Accept", MediaType.APPLICATION_JSON_VALUE);
        return headers;
    }
}