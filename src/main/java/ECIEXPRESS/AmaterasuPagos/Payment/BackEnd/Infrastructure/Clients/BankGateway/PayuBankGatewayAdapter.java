package ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Infrastructure.Clients.BankGateway;

import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Application.Dto.PaymentRequests.CreatePaymentRequest;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model.BankDetails;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model.GatewayResponse;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model.Enums.BankResponseCode;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Ports.BankGatewayProvider;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Infrastructure.Clients.BankGateway.Dto.BankGatewayRequests.PayuPaymentRequest;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Infrastructure.Clients.BankGateway.Dto.BankGatewayResponses.PayuPaymentResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class PayuBankGatewayAdapter implements BankGatewayProvider {

    private final RestTemplate restTemplate;

    @Value("${microservices.bank-gateway.payu.base-url}")
    private String baseUrl;

    @Value("${microservices.bank-gateway.payu.api-login}")
    private String apiLogin;

    @Value("${microservices.bank-gateway.payu.api-key}")
    private String apiKey;

    @Value("${microservices.bank-gateway.payu.merchant-id}")
    private String merchantId;

    @Value("${microservices.bank-gateway.payu.account-id}")
    private String accountId;

    @Value("${microservices.bank-gateway.payu.currency:COP}")
    private String currency;

    @Value("${microservices.bank-gateway.payu.test-mode:true}")
    private Boolean testMode;

    @Override
    public GatewayResponse processPayment(CreatePaymentRequest createPaymentRequest) {
        try {
            log.info("Processing payment with PayU Colombia for order: {}", createPaymentRequest.orderId());

            PayuPaymentRequest payuRequest = buildPayuRequest(createPaymentRequest);

            HttpHeaders headers = createHeaders();
            HttpEntity<PayuPaymentRequest> entity = new HttpEntity<>(payuRequest, headers);

            ResponseEntity<PayuPaymentResponse> response = restTemplate.exchange(
                    baseUrl, HttpMethod.POST, entity, PayuPaymentResponse.class);

            PayuPaymentResponse payuResponse = response.getBody();

            log.info("PayU response received for order {}: State: {}",
                    createPaymentRequest.orderId(),
                    payuResponse != null && payuResponse.getTransactionResponse() != null ?
                            payuResponse.getTransactionResponse().getState() : "null");

            return mapToGatewayResponse(payuResponse, createPaymentRequest.originalAmount());

        } catch (Exception e) {
            log.error("Error processing payment with PayU for order {}: {}",
                    createPaymentRequest.orderId(), e.getMessage());
            return createErrorResponse("PAYU_PROCESSING_ERROR", e.getMessage());
        }
    }

    private PayuPaymentRequest buildPayuRequest(CreatePaymentRequest request) {
        return PayuPaymentRequest.builder()
                .language("es")
                .command("SUBMIT_TRANSACTION")
                .test(testMode)
                .merchant(PayuPaymentRequest.Merchant.builder()
                        .apiLogin(apiLogin)
                        .apiKey(apiKey)
                        .build())
                .transaction(PayuPaymentRequest.Transaction.builder()
                        .order(PayuPaymentRequest.Transaction.Order.builder()
                                .accountId(accountId)
                                .referenceCode(request.orderId())
                                .description("Payment for order " + request.orderId())
                                .language("es")
                                .notifyUrl("http://localhost:8080/Payment/webhook/payu")
                                .additionalValues(buildAdditionalValues(request.originalAmount()))
                                .buyer(buildBuyer(request))
                                .signature(generateSignature(request))
                                .build())
                        .creditCard(buildCreditCard(request.bankDetails()))
                        .type("AUTHORIZATION_AND_CAPTURE")
                        .paymentMethod(request.bankDetails().getBankPaymentType().toString())
                        .paymentCountry("CO")
                        .payer(buildPayer(request))
                        .build())
                .build();
    }

    private Map<String, PayuPaymentRequest.Amount> buildAdditionalValues(Double amount) {
        Map<String, PayuPaymentRequest.Amount> additionalValues = new HashMap<>();

        additionalValues.put("TX_VALUE", PayuPaymentRequest.Amount.builder()
                .value(String.format("%.0f", amount))
                .currency(currency)
                .build());

        additionalValues.put("TX_TAX", PayuPaymentRequest.Amount.builder()
                .value(String.format("%.0f", amount * 0.19))
                .currency(currency)
                .build());

        additionalValues.put("TX_TAX_RETURN_BASE", PayuPaymentRequest.Amount.builder()
                .value(String.format("%.0f", amount))
                .currency(currency)
                .build());

        return additionalValues;
    }

    private PayuPaymentRequest.Transaction.CreditCard buildCreditCard(BankDetails bankDetails) {
        return PayuPaymentRequest.Transaction.CreditCard.builder()
                .number(bankDetails.getAccountNumber())
                .securityCode(bankDetails.getCvv())
                .expirationDate(formatExpiryDate(bankDetails.getExpiryDate()))
                .name(bankDetails.getCardHolderName())
                .processWithoutCvv2(false)
                .build();
    }

    private PayuPaymentRequest.Transaction.Buyer buildBuyer(CreatePaymentRequest request) {
        return PayuPaymentRequest.Transaction.Buyer.builder()
                .merchantBuyerId(request.clientId())
                .fullName("Customer " + request.clientId())
                .emailAddress(request.clientId() + "@eciexpress.com")
                .contactPhone("573001234567")
                .dniNumber("123456789")
                .shippingAddress(PayuPaymentRequest.Transaction.ShippingAddress.builder()
                        .street1("Calle 123")
                        .city("Bogotá")
                        .state("Bogotá D.C.")
                        .country("CO")
                        .postalCode("110111")
                        .phone("573001234567")
                        .build())
                .build();
    }

    private PayuPaymentRequest.Transaction.Payer buildPayer(CreatePaymentRequest request) {
        return PayuPaymentRequest.Transaction.Payer.builder()
                .emailAddress(request.clientId() + "@eciexpress.com")
                .fullName("Customer " + request.clientId())
                .contactPhone("573001234567")
                .dniNumber("123456789")
                .billingAddress(PayuPaymentRequest.Transaction.BillingAddress.builder()
                        .street1("Calle 123")
                        .city("Bogotá")
                        .state("Bogotá D.C.")
                        .country("CO")
                        .postalCode("110111")
                        .phone("573001234567")
                        .build())
                .build();
    }

    private String formatExpiryDate(String expiryDate) {
        if (expiryDate != null && expiryDate.contains("/")) {
            String[] parts = expiryDate.split("/");
            if (parts.length == 2) {
                return "20" + parts[1] + "/" + parts[0];
            }
        }
        return "2025/12";
    }

    private String generateSignature(CreatePaymentRequest request) {
        return String.format("%s~%s~%s~%.0f~%s",
                apiKey, merchantId, request.orderId(), request.originalAmount(), currency);
        //return "test-signature";
    }

    private GatewayResponse mapToGatewayResponse(PayuPaymentResponse payuResponse, double amount) {
        GatewayResponse response = new GatewayResponse();

        if (payuResponse != null && payuResponse.getTransactionResponse() != null) {
            PayuPaymentResponse.PayuTransactionResponse tx = payuResponse.getTransactionResponse();

            response.setSuccess("APPROVED".equals(tx.getState()));
            response.setBankReceiptNumber(tx.getTransactionId());
            response.setAuthorizationNumber(tx.getAuthorizationCode());
            response.setGatewayMessage(tx.getResponseMessage());
            response.setResponseCode(tx.getResponseCode());
            response.setBankResponseCode(mapToBankResponseCode(tx.getState()));
            response.setProcessedAmount(amount);
            response.setCurrency(currency);
        } else {
            response.setSuccess(false);
            response.setGatewayMessage(payuResponse != null ? payuResponse.getError() : "No response from PayU");
            response.setResponseCode("500");
            response.setBankResponseCode(BankResponseCode.BANK_UNAVAILABLE);
        }
        return response;
    }

    private BankResponseCode mapToBankResponseCode(String payuState) {
        if (payuState == null) return BankResponseCode.BANK_UNAVAILABLE;

        switch (payuState) {
            case "APPROVED":
                return BankResponseCode.APPROVED;
            case "DECLINED":
                return BankResponseCode.DECLINED;
            case "EXPIRED":
                return BankResponseCode.EXPIRED_CARD;
            case "PENDING":
                return BankResponseCode.TIMEOUT;
            default:
                return BankResponseCode.BANK_UNAVAILABLE;
        }
    }

    private GatewayResponse createErrorResponse(String errorCode, String errorMessage) {
        GatewayResponse response = new GatewayResponse();
        response.setSuccess(false);
        response.setGatewayMessage(errorMessage);
        response.setResponseCode(errorCode);
        response.setBankResponseCode(BankResponseCode.BANK_UNAVAILABLE);
        return response;
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Accept", MediaType.APPLICATION_JSON_VALUE);
        return headers;
    }
}
