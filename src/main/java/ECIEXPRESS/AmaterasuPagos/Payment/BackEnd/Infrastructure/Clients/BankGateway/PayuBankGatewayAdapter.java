package ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Infrastructure.Clients.BankGateway;

import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model.BankDetails;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model.Enums.BankPaymentType;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model.Enums.BankResponseCode;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model.GatewayResponse;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Ports.BankGatewayProvider;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Infrastructure.Clients.BankGateway.Dto.BankGatewayRequests.PayuPaymentRequest;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Infrastructure.Clients.BankGateway.Dto.BankGatewayResponses.PayuPaymentResponse;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Infrastructure.Web.Dto.PaymentRequests.CreatePaymentRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class PayuBankGatewayAdapter implements BankGatewayProvider {

    private final RestTemplate restTemplate;

    @Value("${microservices.bank-gateway.payu.base-url:https://sandbox.api.payulatam.com/payments-api/4.0/service.cgi}")
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
    private boolean testMode;

    @Value("${microservices.bank-gateway.payu.notify-url:}")
    private String notifyUrl;

    @Value("${microservices.bank-gateway.payu.installments-number:1}")
    private int installmentsNumber;

    @Value("${microservices.bank-gateway.payu.include-iva:true}")
    private boolean includeIva;

    @Override
    public GatewayResponse processPayment(CreatePaymentRequest request) {
        final long startMs = System.currentTimeMillis();
        final String orderId = (request != null) ? request.orderId() : null;
        log.info("PayU processPayment started: orderId={}, testMode={}, currency={}", orderId, testMode, currency);

        try {
            PayuPaymentRequest payuRequest = buildPayuRequest(request);

            HttpHeaders headers = createHeaders();
            HttpEntity<PayuPaymentRequest> entity = new HttpEntity<>(payuRequest, headers);

            ResponseEntity<PayuPaymentResponse> response = restTemplate.exchange(
                    baseUrl,
                    HttpMethod.POST,
                    entity,
                    PayuPaymentResponse.class
            );
            log.info("PayU response received: orderId={}, httpStatus={}, hasBody={}", orderId, response.getStatusCode(), response.getBody() != null);
            GatewayResponse gatewayResponse = mapToGatewayResponse(response.getBody());
            log.info("PayU processPayment completed: orderId={}, success={}, bankResponseCode={}, elapsedMs={}",
                    orderId,
                    gatewayResponse.isSuccess(),
                    gatewayResponse.getBankResponseCode(),
                    System.currentTimeMillis() - startMs);
            return gatewayResponse;
        } catch (HttpClientErrorException e) {
            log.error("Client error when processing PayU payment: status={}, body={}",
                    e.getStatusCode(), safeBody(e.getResponseBodyAsString()), e);
            return createErrorResponse(BankResponseCode.INVALID_REQUEST, e.getMessage());
        } catch (HttpServerErrorException e) {
            log.error("Server error when processing PayU payment: status={}, body={}",
                    e.getStatusCode(), safeBody(e.getResponseBodyAsString()), e);
            return createErrorResponse(BankResponseCode.BANK_ERROR, e.getMessage());
        } catch (ResourceAccessException e) {
            log.error("Timeout/network error when processing PayU payment: {}", e.getMessage(), e);
            return createErrorResponse(BankResponseCode.TIMEOUT, "Connection error to PayU: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error when processing PayU payment", e);
            return createErrorResponse(BankResponseCode.UNKNOWN_ERROR, e.getMessage());
        }
    }

    private PayuPaymentRequest buildPayuRequest(CreatePaymentRequest request) {
        BigDecimal txValue = toCopAmount(request.originalAmount());

        String referenceCode = request.orderId();
        String signature = generateSignature(referenceCode, txValue);

        BankDetails bank = request.bankDetails();
        validateSupportedBankPaymentType(bank);

        String paymentMethod = resolvePayuPaymentMethod(bank);

        ClientContext clientContext = resolveClientContext();

        PayuPaymentRequest.Merchant merchant = PayuPaymentRequest.Merchant.builder()
                .apiLogin(apiLogin)
                .apiKey(apiKey)
                .build();

        PayuPaymentRequest.Address shippingAddress = PayuPaymentRequest.Address.builder()
                .street1("Calle 123")
                .city("Bogotá")
                .state("Cundinamarca")
                .country("CO")
                .postalCode("110111")
                .phone("3000000000")
                .build();

        String buyerFullName = bank.getCardHolderName() != null ? bank.getCardHolderName() : request.clientId();
        String buyerEmail = sanitizeEmailFallback(request.clientId());

        PayuPaymentRequest.Buyer buyer = PayuPaymentRequest.Buyer.builder()
                .merchantBuyerId(request.clientId())
                .fullName(buyerFullName)
                .emailAddress(buyerEmail)
                .contactPhone("3000000000")
                .dniNumber("12345678")
                .shippingAddress(shippingAddress)
                .build();

        PayuPaymentRequest.Payer payer = PayuPaymentRequest.Payer.builder()
                .merchantPayerId(request.clientId())
                .fullName(buyerFullName)
                .emailAddress(buyerEmail)
                .contactPhone("3000000000")
                .dniNumber("12345678")
                .billingAddress(shippingAddress)
                .build();

        PayuPaymentRequest.Order.OrderBuilder orderBuilder = PayuPaymentRequest.Order.builder()
                .accountId(accountId)
                .referenceCode(referenceCode)
                .description("Payment for order " + referenceCode)
                .language("es")
                .signature(signature)
                .buyer(buyer)
                .additionalValues(buildAdditionalValues(txValue));

        if (notifyUrl != null && !notifyUrl.isBlank()) {
            orderBuilder.notifyUrl(notifyUrl);
        }

        PayuPaymentRequest.Order order = orderBuilder.build();

        PayuPaymentRequest.Transaction.TransactionBuilder txBuilder = PayuPaymentRequest.Transaction.builder()
                .order(order)
                .type("AUTHORIZATION_AND_CAPTURE")
                .paymentMethod(paymentMethod)
                .paymentCountry("CO")
                .payer(payer)
                .deviceSessionId(clientContext.deviceSessionId())
                .ipAddress(clientContext.ipAddress())
                .cookie(clientContext.cookie())
                .userAgent(clientContext.userAgent())
                .extraParameters(Map.of("INSTALLMENTS_NUMBER", Math.max(1, installmentsNumber)));

        boolean isDebit = bank.getBankPaymentType() == BankPaymentType.DEBIT_CARD;

        if (isDebit) {
            txBuilder.debitCard(PayuPaymentRequest.DebitCard.builder()
                    .number(bank.getAccountNumber())
                    .securityCode(bank.getCvv())
                    .expirationDate(formatExpiryDate(bank.getExpiryDate()))
                    .name(buyerFullName)
                    .processWithoutCvv2(false)
                    .build());
        } else {
            txBuilder.creditCard(PayuPaymentRequest.CreditCard.builder()
                    .number(bank.getAccountNumber())
                    .securityCode(bank.getCvv())
                    .expirationDate(formatExpiryDate(bank.getExpiryDate()))
                    .name(buyerFullName)
                    .processWithoutCvv2(false)
                    .build());
        }

        return PayuPaymentRequest.builder()
                .language("es")
                .command("SUBMIT_TRANSACTION")
                .test(testMode)
                .merchant(merchant)
                .transaction(txBuilder.build())
                .build();
    }

    private void validateSupportedBankPaymentType(BankDetails bank) {
        if (bank == null || bank.getBankPaymentType() == null) {
            throw new IllegalArgumentException("bankPaymentType is required to process a PayU payment");
        }

        if (bank.getBankPaymentType() == BankPaymentType.PSE || bank.getBankPaymentType() == BankPaymentType.APP) {
            throw new IllegalArgumentException("BankPaymentType " + bank.getBankPaymentType() + " is not implemented in PayuBankGatewayAdapter yet");
        }
    }

    private Map<String, PayuPaymentRequest.AdditionalValue> buildAdditionalValues(BigDecimal totalAmount) {
        BigDecimal txValue = totalAmount.stripTrailingZeros();

        BigDecimal tax;
        BigDecimal taxBase;

        if (!includeIva) {
            tax = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            taxBase = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        } else {
            taxBase = totalAmount.divide(new BigDecimal("1.19"), 2, RoundingMode.HALF_UP);
            tax = totalAmount.subtract(taxBase).setScale(2, RoundingMode.HALF_UP);
        }

        return Map.of(
                "TX_VALUE", PayuPaymentRequest.AdditionalValue.builder()
                        .value(txValue.setScale(0, RoundingMode.UNNECESSARY))
                        .currency(currency)
                        .build(),
                "TX_TAX", PayuPaymentRequest.AdditionalValue.builder()
                        .value(tax)
                        .currency(currency)
                        .build(),
                "TX_TAX_RETURN_BASE", PayuPaymentRequest.AdditionalValue.builder()
                        .value(taxBase)
                        .currency(currency)
                        .build()
        );
    }

    /**
     * PayU signature format (MD5/SHA): ApiKey~merchantId~referenceCode~tx_value~currency
     */
    private String generateSignature(String referenceCode, BigDecimal txValue) {
        String value = txValue.stripTrailingZeros().toPlainString();
        String raw = apiKey + "~" + merchantId + "~" + referenceCode + "~" + value + "~" + currency;
        return payuMd5Hex(raw);
    }

    @SuppressWarnings("java:S4790") // PayU interoperability requirement (signature)
    private String payuMd5Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to compute PayU MD5 signature", e);
        }
    }

    private String resolvePayuPaymentMethod(BankDetails bank) {
        String pan = bank.getAccountNumber();
        if (pan == null || pan.isBlank()) {
            throw new IllegalArgumentException("bankDetails.accountNumber (PAN) is required");
        }

        String brand = inferCardBrand(pan);

        if (bank.getBankPaymentType() == BankPaymentType.DEBIT_CARD) {
           if ("VISA".equals(brand)) {
                return "VISA_DEBIT";
            }
        }
        return brand;
    }

    private String inferCardBrand(String pan) {
        String digits = pan.replaceAll("\\s+", "");
        if (digits.startsWith("4")) {
            return "VISA";
        }
        if (digits.matches("^5[1-5].*")) {
            return "MASTERCARD";
        }
        if (digits.matches("^3[47].*")) {
            return "AMEX";
        }
        if (digits.matches("^(30[0-5]|36|38).*")) {
            return "DINERS";
        }
        throw new IllegalArgumentException("Unsupported/unknown card brand for PAN prefix: " + digits.substring(0, Math.min(6, digits.length())));
    }

    private BigDecimal toCopAmount(double amount) {
        BigDecimal bd = BigDecimal.valueOf(amount).stripTrailingZeros();
        if (bd.scale() > 0) {
            throw new IllegalArgumentException("COP amount must not include decimals. Received: " + amount);
        }
        return bd.setScale(0, RoundingMode.UNNECESSARY);
    }

    private ClientContext resolveClientContext() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) {
                return ClientContext.fallback();
            }

            HttpServletRequest req = attrs.getRequest();

            String ip = extractClientIp(req);

            String userAgent = firstNonBlank(req.getHeader("User-Agent"), "Unknown");
            String cookie = firstNonBlank(req.getHeader("Cookie"), "");

            String deviceSessionId = firstNonBlank(
                    req.getHeader("X-Device-Session-Id"),
                    req.getParameter("deviceSessionId")
            );

            if (deviceSessionId == null || deviceSessionId.isBlank()) {
                String sessionId = null;
                if (req.getSession(false) != null) {
                    sessionId = req.getSession(false).getId();
                }
                if (sessionId == null) {
                    sessionId = UUID.randomUUID().toString();
                }
                deviceSessionId = md5Hex(sessionId + System.currentTimeMillis());
            }

            return new ClientContext(deviceSessionId, ip, cookie, userAgent);

        } catch (Exception e) {
            log.warn("Could not resolve client context for PayU anti-fraud fields: {}", e.getMessage());
            return ClientContext.fallback();
        }
    }

    private String extractClientIp(HttpServletRequest req) {
        String xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        String xrip = req.getHeader("X-Real-IP");
        if (xrip != null && !xrip.isBlank()) {
            return xrip.trim();
        }
        return req.getRemoteAddr() != null ? req.getRemoteAddr() : "127.0.0.1";
    }

    private String firstNonBlank(String value, String fallback) {
        return (value != null && !value.isBlank()) ? value : fallback;
    }

    private String safeBody(String body) {
        if (body == null) return "";
        return body.length() > 1000 ? body.substring(0, 1000) + "..." : body;
    }

    private String md5Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to compute MD5 signature", e);
        }
    }

    private String formatExpiryDate(String expiryDate) {
        if (expiryDate == null || expiryDate.isBlank()) {
            throw new IllegalArgumentException("Card expiryDate is required");
        }
        String exp = expiryDate.trim();

        if (exp.matches("\\d{4}/\\d{2}")) {
            return exp;
        }

        if (exp.matches("\\d{2}/\\d{2}")) {
            String[] parts = exp.split("/");
            return "20" + parts[1] + "/" + parts[0];
        }

        if (exp.matches("\\d{2}/\\d{4}")) {
            String[] parts = exp.split("/");
            return parts[1] + "/" + parts[0];
        }

        if (exp.matches("\\d{4}-\\d{2}")) {
            String[] parts = exp.split("-");
            return parts[0] + "/" + parts[1];
        }

        throw new IllegalArgumentException("Invalid expiryDate format. Expected MM/YY, MM/YYYY, or YYYY/MM. Received: " + expiryDate);
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Accept", MediaType.APPLICATION_JSON_VALUE);
        return headers;
    }

    private GatewayResponse mapToGatewayResponse(PayuPaymentResponse payuResponse) {
        if (payuResponse == null || payuResponse.getTransactionResponse() == null) {
            return createErrorResponse(BankResponseCode.ERROR, "No response from PayU");
        }

        PayuPaymentResponse.PayuTransactionResponse tx = payuResponse.getTransactionResponse();

        GatewayResponse response = new GatewayResponse();
        response.setSuccess("APPROVED".equalsIgnoreCase(tx.getState()));

        response.setBankReceiptNumber(tx.getTransactionId());
        response.setAuthorizationNumber(tx.getAuthorizationCode());

        String msg = firstNonBlank(
                tx.getResponseMessage(),
                tx.getPaymentNetworkResponseErrorMessage(),
                tx.getPendingReason(),
                tx.getErrorCode()
        );
        response.setGatewayMessage(msg);

        String payuCode = firstNonBlank(tx.getResponseCode(), tx.getErrorCode(), tx.getState());
        response.setResponseCode(payuCode);

        response.setBankResponseCode(
                mapToBankResponseCode(tx.getResponseCode(), tx.getState())
        );

        if (tx.getAdditionalInfo() != null
                && tx.getAdditionalInfo().getPayments() != null
                && !tx.getAdditionalInfo().getPayments().isEmpty()) {

            PayuPaymentResponse.PayuPayment p = tx.getAdditionalInfo().getPayments().get(0);

            response.setCurrency(p.getCurrency());
            response.setProcessedAmount(safeParseDouble(p.getAmount()));
        } else {
            response.setCurrency("COP");
            response.setProcessedAmount(0.0);
        }

        return response;
    }

    private static String firstNonBlank(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) return v;
        }
        return "";
    }

    private static double safeParseDouble(String raw) {
        if (raw == null || raw.isBlank()) return 0.0;
        try {
            return Double.parseDouble(raw.trim());
        } catch (Exception ignored) {
            return 0.0;
        }
    }

    private BankResponseCode mapToBankResponseCode(String payuResponseCode, String state) {
        if ("APPROVED".equalsIgnoreCase(state)) {
            return BankResponseCode.APPROVED;
        }
        if ("PENDING".equalsIgnoreCase(state)) {
            return BankResponseCode.PENDING;
        }
        if ("DECLINED".equalsIgnoreCase(state)) {
            return switch (payuResponseCode) {
                case "INSUFFICIENT_FUNDS" -> BankResponseCode.INSUFFICIENT_FUNDS;
                case "INVALID_CARD" -> BankResponseCode.INVALID_CARD;
                case "EXPIRED" -> BankResponseCode.EXPIRED_CARD;
                default -> BankResponseCode.DECLINED;
            };
        }
        return BankResponseCode.UNKNOWN_ERROR;
    }

    private GatewayResponse createErrorResponse(BankResponseCode code, String message) {
        GatewayResponse response = new GatewayResponse();
        response.setSuccess(false);
        response.setBankResponseCode(code);
        response.setGatewayMessage(message);
        response.setResponseCode(code.name());
        response.setProcessedAmount(0.0);
        response.setCurrency("COP");
        return response;
    }

    private String sanitizeEmailFallback(String clientId) {
        String safe = clientId == null ? "unknown" : clientId.replaceAll("[^A-Za-z0-9._%+-]", "");
        if (safe.isBlank()) safe = "unknown";
        return safe + "@example.com";
    }

    private record ClientContext(String deviceSessionId, String ipAddress, String cookie, String userAgent) {
        static ClientContext fallback() {
            String dsid = md5Static(UUID.randomUUID().toString() + System.currentTimeMillis());
            return new ClientContext(dsid, "127.0.0.1", "", "Unknown");
        }
        private static String md5Static(String input) {
            try {
                MessageDigest md = MessageDigest.getInstance("MD5");
                byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
                return HexFormat.of().formatHex(digest);
            } catch (Exception e) {
                return UUID.randomUUID().toString().replace("-", "");
            }
        }
    }
}
