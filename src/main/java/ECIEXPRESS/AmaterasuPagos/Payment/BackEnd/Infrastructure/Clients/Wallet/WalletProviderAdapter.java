package ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Infrastructure.Clients.Wallet;

import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Ports.WalletProvider;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Infrastructure.Clients.Wallet.Dto.WalletRequests.PayWithWalletRequest;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Infrastructure.Clients.Wallet.Dto.WalletResponses.PayWithWalletResponse;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Infrastructure.Web.Dto.PaymentRequests.CreatePaymentRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;

@Slf4j
@Component
@RequiredArgsConstructor
public class WalletProviderAdapter implements WalletProvider {

    private final RestTemplate restTemplate;

    @Value("${gateway.url:http://gateway:8081/api}")
    private String baseUrl;

    @Value("${microservices.wallet.base-path}")
    private String basePath;

    @Override
    public PayWithWalletResponse processPayment(CreatePaymentRequest createPaymentRequest) {
        if (createPaymentRequest == null) {
            throw new IllegalArgumentException("CreatePaymentRequest cannot be null");
        }

        PayWithWalletRequest walletRequest = mapToWalletRequest(createPaymentRequest);

        String url = buildUrl("/pay");
        HttpEntity<PayWithWalletRequest> entity = new HttpEntity<>(walletRequest, createHeaders());

        try {
            ResponseEntity<PayWithWalletResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    PayWithWalletResponse.class
            );

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new IllegalStateException("Wallet service returned non-2xx status: " + response.getStatusCode());
            }

            PayWithWalletResponse body = response.getBody();
            if (body == null) {
                throw new IllegalStateException("Wallet service returned empty body");
            }

            return body;

        } catch (HttpServerErrorException e) {
            String errBody = safeBody(e);
            log.warn("Wallet service returned {}. Returning fallback PayWithWalletResponse.", e.getStatusCode());
            if (!errBody.isBlank()) {
                log.debug("Wallet service error body: {}", errBody);
            }
            return new PayWithWalletResponse(null);

        } catch (HttpClientErrorException.NotFound e) {
            throw new IllegalStateException("Wallet not found for clientId=" + walletRequest.clientId(), e);

        } catch (HttpClientErrorException e) {
            String errBody = safeBody(e);
            log.error("Wallet service error. status={}, body={}", e.getStatusCode(), errBody);
            throw new IllegalStateException(
                    "Wallet service error: " + e.getStatusCode() + (errBody.isBlank() ? "" : (" - " + errBody)),
                    e
            );
        }
    }

    private PayWithWalletRequest mapToWalletRequest(CreatePaymentRequest createPaymentRequest) {
        Object amountObj = createPaymentRequest.originalAmount();
        double amount = toDouble(amountObj);

        return new PayWithWalletRequest(
                createPaymentRequest.clientId(),
                amount
        );
    }


    private double toDouble(Object amountObj) {
        if (amountObj == null) return 0d;
        if (amountObj instanceof Number n) {
            return n.doubleValue();
        }
        try {
            return Double.parseDouble(amountObj.toString());
        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to convert amount to double: " + amountObj, e);
        }
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        return headers;
    }

    private String safeBody(org.springframework.web.client.RestClientResponseException e) {
        try {
            String body = e.getResponseBodyAsString();
            return body == null ? "" : body;
        } catch (Exception ignored) {
            return "";
        }
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
        String v = value == null ? "" : value.trim();
        if (v.isBlank()) return "";
        v = stripTrailingSlash(v);
        return ensureLeadingSlash(v);
    }

    private String buildUrl(String pathTemplate) {
        String cleanBaseUrl = stripTrailingSlash(baseUrl);
        String cleanBasePath = normalizePath(basePath);
        String cleanPath = normalizePath(pathTemplate);

        return cleanBaseUrl + cleanBasePath + cleanPath;
    }
}