package ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Infrastructure.Clients.Receipt;

import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model.Payment;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Ports.ReceiptProvider;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Infrastructure.Clients.Receipt.Dto.ReceiptRequests.CreateReceiptRequest;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Infrastructure.Clients.Receipt.Dto.ReceiptResponses.CreateReceiptResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReceiptProviderAdapter implements ReceiptProvider {

    private static final Set<HttpStatus> SUCCESS_STATUSES = Set.of(HttpStatus.CREATED, HttpStatus.OK);

    private final RestTemplate restTemplate;

    @Value("${gateway.url:http://gateway:8081/api}")
    private String baseUrl;

    @Value("${microservices.receipt.base-path}")
    private String basePath;

    @Override
    public CreateReceiptResponse createReceipt(Payment payment) {
        try {
            log.info("Creating receipt for order: {}", payment.getOrderId());

            CreateReceiptRequest request = mapToCreateReceiptRequest(payment);

            HttpHeaders headers = createHeaders();
            HttpEntity<CreateReceiptRequest> entity = new HttpEntity<>(request, headers);

            String url = buildUrl("");

            ResponseEntity<CreateReceiptResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    CreateReceiptResponse.class
            );

            if (!SUCCESS_STATUSES.contains(response.getStatusCode()) || response.getBody() == null) {
                log.error("Failed to create receipt for order {}. Status: {}", payment.getOrderId(), response.getStatusCode());
                return emptyResponse(payment);
            }

            log.info("Receipt created successfully for order {}", payment.getOrderId());
            return response.getBody();

        } catch (HttpClientErrorException.NotFound e) {
            log.warn("Receipt service returned 404 while creating receipt for order {}.", payment.getOrderId());
            return emptyResponse(payment);

        } catch (HttpClientErrorException e) {
            log.error("Receipt service returned {} while creating receipt for order {}. Body: {}",
                    e.getStatusCode(), payment.getOrderId(), safeBody(e), e);
            return emptyResponse(payment);

        } catch (Exception e) {
            log.error("Error creating receipt for order {}: {}", payment.getOrderId(), e.getMessage(), e);
            return emptyResponse(payment);
        }
    }

    private CreateReceiptRequest mapToCreateReceiptRequest(Payment payment) {
        return new CreateReceiptRequest(
                payment.getOrderId(),
                payment.getClientId(),
                payment.getStoreId(),
                payment.getOriginalAmount(),
                payment.getFinalAmount(),
                payment.getPaymentMethod(),
                payment.getTimeStamps(),
                payment.getAppliedPromotions()
        );
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        return headers;
    }

    private String buildUrl(String pathSuffix) {
        String cleanBaseUrl = stripTrailingSlash(baseUrl);
        String cleanBasePath = normalizePath(basePath);

        if (cleanBaseUrl.endsWith("/api")) {
            if (cleanBasePath.equals("/api")) {
                cleanBasePath = "";
            } else if (cleanBasePath.startsWith("/api/")) {
                cleanBasePath = cleanBasePath.substring("/api".length());
            }
        }

        String cleanSuffix = (pathSuffix == null || pathSuffix.isBlank())
                ? ""
                : (pathSuffix.startsWith("/") ? pathSuffix : "/" + pathSuffix);

        return String.format("%s%s%s", cleanBaseUrl, cleanBasePath, cleanSuffix);
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
        String v = stripTrailingSlash(value);
        return ensureLeadingSlash(v);
    }

    private CreateReceiptResponse emptyResponse(Payment payment) {
        return new CreateReceiptResponse(
                null,
                payment != null ? payment.getOrderId() : null,
                payment != null ? payment.getClientId() : null,
                payment != null ? payment.getStoreId() : null,
                payment != null ? payment.getFinalAmount() : 0,
                null,
                null
        );
    }

    private String safeBody(HttpClientErrorException e) {
        try {
            return e.getResponseBodyAsString();
        } catch (Exception ignored) {
            return "";
        }
    }
}
