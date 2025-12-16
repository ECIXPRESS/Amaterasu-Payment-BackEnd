package ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Infrastructure.Clients.Promotion;

import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Ports.PromotionProvider;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Infrastructure.Clients.Promotion.Dto.PromotionRequests.ApplyPromotionRequest;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Infrastructure.Clients.Promotion.Dto.PromotionResponses.ApplyPromotionResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;

@Slf4j
@Component
@RequiredArgsConstructor
public class PromotionProviderAdapter implements PromotionProvider {

    private static final String APPLY_PATH_SUFFIX = "/apply";

    private final RestTemplate restTemplate;

    @Value("${gateway.url:http://gateway:8081}")
    private String baseUrl;

    @Value("${microservices.promotion.base-path}")
    private String basePath;

    @Override
    public ApplyPromotionResponse applyPromotions(String orderId) {
        try {
            log.info("Applying promotions for order: {}", orderId);

            ApplyPromotionRequest promotionRequest = new ApplyPromotionRequest(orderId);

            HttpHeaders headers = createHeaders();
            HttpEntity<ApplyPromotionRequest> entity = new HttpEntity<>(promotionRequest, headers);

            String url = resolveApplyUrl();

            ResponseEntity<ApplyPromotionResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    ApplyPromotionResponse.class
            );

            if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
                log.error("Failed to apply promotions for order: {}. Status: {}", orderId, response.getStatusCode());
                return new ApplyPromotionResponse(0, null);
            }

            log.info("Promotion response received for order {}", orderId);
            return response.getBody();

        } catch (HttpClientErrorException.NotFound e) {
            log.warn("Order {} not found while applying promotions (promotion service returned 404)", orderId);
            return new ApplyPromotionResponse(0, null);

        } catch (Exception e) {
            log.error("Error applying promotions for order {}: {}", orderId, e.getMessage(), e);
            return new ApplyPromotionResponse(0, null);
        }
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        return headers;
    }

    private String resolveApplyUrl() {
        String normalizedBasePath = normalizePath(basePath);

        if (normalizedBasePath.endsWith(APPLY_PATH_SUFFIX)) {
            return buildUrl("");
        }

        return buildUrl(APPLY_PATH_SUFFIX);
    }

    private String buildUrl(String pathSuffix) {
        String cleanBaseUrl = stripTrailingSlash(baseUrl);
        String cleanBasePath = stripTrailingSlash(basePath);

        String cleanSuffix = "";
        if (pathSuffix != null && !pathSuffix.isBlank()) {
            if (pathSuffix.startsWith("/")) {
                cleanSuffix = pathSuffix;
            } else {
                cleanSuffix = "/" + pathSuffix;
            }
        }

        return String.format("%s%s%s", cleanBaseUrl, ensureLeadingSlash(cleanBasePath), cleanSuffix);
    }

    private String stripTrailingSlash(String value) {
        if (value == null) return "";
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private String ensureLeadingSlash(String value) {
        if (value == null || value.isBlank()) return "";
        return value.startsWith("/") ? value : "/" + value;
    }

    private String normalizePath(String value) {
        return ensureLeadingSlash(stripTrailingSlash(value));
    }
}