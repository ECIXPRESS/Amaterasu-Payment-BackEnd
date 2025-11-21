package ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Infrastructure.Clients.Receipt;

import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Ports.ReceiptProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class ReceiptProviderAdapter implements ReceiptProvider {

    private final RestTemplate restTemplate;

    @Value("${microservices.receipt.url}")
    private String receiptServiceUrl;

    public ReceiptProviderAdapter(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

}
