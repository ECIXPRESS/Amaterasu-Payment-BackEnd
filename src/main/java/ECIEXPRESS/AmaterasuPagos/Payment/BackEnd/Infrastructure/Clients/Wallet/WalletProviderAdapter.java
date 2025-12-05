package ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Infrastructure.Clients.Wallet;

import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Infrastructure.Web.Dto.PaymentRequests.CreatePaymentRequest;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Ports.WalletProvider;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Infrastructure.Clients.Wallet.Dto.WalletRequests.PayWithWalletRequest;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Infrastructure.Clients.Wallet.Dto.WalletResponses.PayWithWalletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
@RequiredArgsConstructor
public class WalletProviderAdapter implements WalletProvider {

    private final RestTemplate restTemplate;

    @Value("${microservices.wallet.url}")
    private String baseUrl;

    @Value("${microservices.wallet.base-path}")
    private String basePath;

    @Override
    public PayWithWalletResponse processPayment(CreatePaymentRequest createPaymentRequest) {
        try {
            log.info("Processing Receipt for order: {}", createPaymentRequest.orderId());
            PayWithWalletRequest payWithWalletRequest = mapToWalletRequest(createPaymentRequest);

            HttpHeaders headers = createHeaders();
            HttpEntity<PayWithWalletRequest> entity = new HttpEntity<>(payWithWalletRequest, headers);

            ResponseEntity<PayWithWalletResponse> response = restTemplate.exchange(
                    baseUrl+basePath, HttpMethod.POST, entity, PayWithWalletResponse.class);

            PayWithWalletResponse walletResponse = response.getBody();

            log.info("Request response received for order {}", createPaymentRequest.orderId());

            return walletResponse;

        } catch (Exception e) {
            log.error("Error processing receipt for order {} Error: {}",createPaymentRequest.orderId(), e.getMessage());
            return new PayWithWalletResponse(null);
        }
    }
    private PayWithWalletRequest mapToWalletRequest(CreatePaymentRequest createPaymentRequest) {

        return new PayWithWalletRequest(
                createPaymentRequest.clientId(),
                createPaymentRequest.originalAmount());
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Accept", MediaType.APPLICATION_JSON_VALUE);
        return headers;
    }
}
