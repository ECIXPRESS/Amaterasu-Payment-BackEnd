package ECIEXPRESS.AmaterasuPagos.Payment.BackEnd;

import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model.Bank;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model.BankDetails;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model.Enums.BankAccountType;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model.Enums.BankPaymentType;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model.Enums.BankResponseCode;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model.Enums.PaymentMethodType;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model.GatewayResponse;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Infrastructure.Clients.BankGateway.PayuBankGatewayAdapter;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Infrastructure.Web.Dto.PaymentRequests.CreatePaymentRequest;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class PayuBankGatewayAdapterTest {

    private MockWebServer mockWebServer;
    private PayuBankGatewayAdapter adapter;

    @BeforeEach
    void setUp() throws Exception {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        adapter = new PayuBankGatewayAdapter(new RestTemplate());

        setField(adapter, "baseUrl", mockWebServer.url("/api/payu").toString());
        setField(adapter, "apiLogin", "login");
        setField(adapter, "apiKey", "key");
        setField(adapter, "merchantId", "merchant");
        setField(adapter, "accountId", "account");
        setField(adapter, "currency", "COP");
        setField(adapter, "testMode", true);

        // Ensure deterministic defaults (this adapter is constructed outside Spring)
        setField(adapter, "notifyUrl", "");
        setField(adapter, "installmentsNumber", 1);
        setField(adapter, "includeIva", false);

        RequestContextHolder.resetRequestAttributes();
    }

    @AfterEach
    void tearDown() throws Exception {
        RequestContextHolder.resetRequestAttributes();
        mockWebServer.shutdown();
    }

    @Test
    void processPayment_WithClientError400_ShouldReturnInvalidRequest() {
        CreatePaymentRequest request = buildCardPaymentRequest(
                "ORDER-400", "CLIENT-1", "STORE-1", 100000.0,
                BankPaymentType.CREDIT_CARD, "4111111111111111", "12/25", "123", "John Doe"
        );

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(400)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN_VALUE)
                .setBody("bad request"));

        GatewayResponse response = adapter.processPayment(request);

        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertEquals(BankResponseCode.INVALID_REQUEST, response.getBankResponseCode());
        assertEquals("INVALID_REQUEST", response.getResponseCode());
        assertEquals(0.0, response.getProcessedAmount());
        assertEquals("COP", response.getCurrency());
    }

    @Test
    void processPayment_WithDeclinedInsufficientFunds_ShouldMapBankResponseCode() {
        CreatePaymentRequest request = buildCardPaymentRequest(
                "ORDER-DECLINED", "CLIENT-2", "STORE-2", 100000.0,
                BankPaymentType.CREDIT_CARD, "5111111111111118", "01/29", "999", "Jane Doe"
        );

        String jsonResponse = """
                {
                  "code": "SUCCESS",
                  "transactionResponse": {
                    "transactionId": "TX-DECLINED-1",
                    "state": "DECLINED",
                    "authorizationCode": "AUTH-DECL",
                    "responseCode": "INSUFFICIENT_FUNDS",
                    "responseMessage": "Not enough funds",
                    "additionalInfo": {
                      "payments": [
                        { "type": "CREDIT_CARD", "amount": "100000.0", "currency": "COP" }
                      ]
                    }
                  }
                }
                """;

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(jsonResponse));

        GatewayResponse response = adapter.processPayment(request);

        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertEquals("TX-DECLINED-1", response.getBankReceiptNumber());
        assertEquals("AUTH-DECL", response.getAuthorizationNumber());
        assertEquals("Not enough funds", response.getGatewayMessage());
        assertEquals("INSUFFICIENT_FUNDS", response.getResponseCode());
        assertEquals(BankResponseCode.INSUFFICIENT_FUNDS, response.getBankResponseCode());
        assertEquals(100000.0, response.getProcessedAmount());
        assertEquals("COP", response.getCurrency());
    }

    @Test
    void processPayment_WithPendingAndNoPayments_ShouldDefaultAmountAndCurrency() {
        CreatePaymentRequest request = buildCardPaymentRequest(
                "ORDER-PENDING", "CLIENT-3", "STORE-3", 100000.0,
                BankPaymentType.CREDIT_CARD, "371449635398431", "2025/12", "1234", "Alex Doe"
        );

        String jsonResponse = """
                {
                  "code": "SUCCESS",
                  "transactionResponse": {
                    "transactionId": "TX-PENDING-1",
                    "state": "PENDING",
                    "authorizationCode": "AUTH-PEND",
                    "responseCode": "PENDING_TRANSACTION_CONFIRMATION",
                    "responseMessage": "Pending confirmation"
                  }
                }
                """;

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(jsonResponse));

        GatewayResponse response = adapter.processPayment(request);

        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertEquals(BankResponseCode.PENDING, response.getBankResponseCode());
        assertEquals("COP", response.getCurrency());
        assertEquals(0.0, response.getProcessedAmount());
    }

    @Test
    void processPayment_WithDebitVisaNotifyUrlAndIva_ShouldBuildRequestWithExpectedFields() throws Exception {
        setField(adapter, "notifyUrl", "https://example.com/notify");
        setField(adapter, "includeIva", true);
        setField(adapter, "installmentsNumber", 0); // should be clamped to 1

        CreatePaymentRequest request = buildCardPaymentRequest(
                "ORDER-DEBIT", "CLIENT-4", "STORE-4", 119000.0,
                BankPaymentType.DEBIT_CARD, "4111111111111111", "12/25", "321", "Debit User"
        );

        String jsonResponse = """
                {
                  "code": "SUCCESS",
                  "transactionResponse": {
                    "transactionId": "TX-DEBIT-1",
                    "state": "APPROVED",
                    "authorizationCode": "AUTH-DEBIT",
                    "responseCode": "APPROVED",
                    "responseMessage": "APPROVED",
                    "additionalInfo": {
                      "payments": [
                        { "type": "DEBIT_CARD", "amount": "119000.0", "currency": "COP" }
                      ]
                    }
                  }
                }
                """;

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(jsonResponse));

        GatewayResponse response = adapter.processPayment(request);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals(BankResponseCode.APPROVED, response.getBankResponseCode());

        RecordedRequest recorded = mockWebServer.takeRequest(2, TimeUnit.SECONDS);
        assertNotNull(recorded);
        assertEquals("POST", recorded.getMethod());
        assertTrue(recorded.getHeader("Content-Type").contains("application/json"));

        String body = recorded.getBody().readUtf8();

        // Debit VISA mapping + expiry formatting
        assertTrue(body.matches("(?s).*\\\"paymentMethod\\\"\\s*:\\s*\\\"VISA_DEBIT\\\".*"));
        assertTrue(body.matches("(?s).*\\\"expirationDate\\\"\\s*:\\s*\\\"2025/12\\\".*"));
        assertTrue(body.contains("\"debitCard\""));

        // notifyUrl and installments clamp
        assertTrue(body.matches("(?s).*\\\"notifyUrl\\\"\\s*:\\s*\\\"https://example.com/notify\\\".*"));
        assertTrue(body.contains("\"extraParameters\""));
        assertTrue(body.contains("INSTALLMENTS_NUMBER"));
        assertTrue(body.contains(":1"));

        // IVA included => tax base should exist and be ~100000
        assertTrue(body.contains("TX_TAX_RETURN_BASE"));
        assertTrue(body.contains("100000"));
    }

    @Test
    void resolveClientContext_ShouldUseRequestHeadersAndIpResolution() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("X-Forwarded-For", "190.0.0.1, 10.0.0.1");
        req.addHeader("User-Agent", "JUnit");
        req.addHeader("Cookie", "test_cookie=1");
        req.addHeader("X-Device-Session-Id", "device-session-123");

        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(req));

        Object ctx = invokePrivate(adapter, "resolveClientContext");
        assertNotNull(ctx);

        String ip = (String) invokeOnObject(ctx, "ipAddress");
        String ua = (String) invokeOnObject(ctx, "userAgent");
        String cookie = (String) invokeOnObject(ctx, "cookie");
        String dsid = (String) invokeOnObject(ctx, "deviceSessionId");

        assertEquals("190.0.0.1", ip);
        assertEquals("JUnit", ua);
        assertEquals("test_cookie=1", cookie);
        assertEquals("device-session-123", dsid);
    }

    @Test
    void resolveClientContext_WhenNoDeviceSessionId_ShouldComputeMd5FromSession() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("X-Real-IP", "1.2.3.4");
        req.addHeader("User-Agent", "JUnit-UA");
        req.getSession(true); // ensure getSession(false) is not null in adapter

        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(req));

        Object ctx = invokePrivate(adapter, "resolveClientContext");
        assertNotNull(ctx);

        String ip = (String) invokeOnObject(ctx, "ipAddress");
        String dsid = (String) invokeOnObject(ctx, "deviceSessionId");

        assertEquals("1.2.3.4", ip);
        assertNotNull(dsid);
        assertTrue(dsid.matches("^[0-9a-f]{32,44}$"));
    }

    @Test
    void processPayment_WhenRestTemplateThrowsResourceAccessException_ShouldReturnTimeout() throws Exception {
        RestTemplate throwing = new RestTemplate() {
            public <T> org.springframework.http.ResponseEntity<T> exchange(
                    String url,
                    org.springframework.http.HttpMethod method,
                    org.springframework.http.HttpEntity<?> requestEntity,
                    Class<T> responseType
            ) {
                throw new ResourceAccessException("simulated connection issue");
            }
        };

        PayuBankGatewayAdapter local = new PayuBankGatewayAdapter(throwing);
        setField(local, "baseUrl", "http://does-not-matter");
        setField(local, "apiLogin", "login");
        setField(local, "apiKey", "key");
        setField(local, "merchantId", "merchant");
        setField(local, "accountId", "account");
        setField(local, "currency", "COP");
        setField(local, "testMode", true);

        CreatePaymentRequest request = buildCardPaymentRequest(
                "ORDER-TIMEOUT", "CLIENT-5", "STORE-5", 100000.0,
                BankPaymentType.CREDIT_CARD, "4111111111111111", "12/25", "123", "John Doe"
        );

        GatewayResponse response = local.processPayment(request);

        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertEquals(BankResponseCode.TIMEOUT, response.getBankResponseCode());
        assertEquals("TIMEOUT", response.getResponseCode());
    }

    @Test
    void privateHelpers_ShouldCoverExpiryBrandCopAmountAndSafeBody() throws Exception {
        // formatExpiryDate branches
        assertEquals("2025/12", invokePrivate(adapter, "formatExpiryDate", "12/25"));
        assertEquals("2025/12", invokePrivate(adapter, "formatExpiryDate", "12/2025"));
        assertEquals("2025/12", invokePrivate(adapter, "formatExpiryDate", "2025/12"));
        assertEquals("2025/12", invokePrivate(adapter, "formatExpiryDate", "2025-12"));

        assertThrows(IllegalArgumentException.class, () -> invokePrivate(adapter, "formatExpiryDate", "bogus"));
        assertThrows(IllegalArgumentException.class, () -> invokePrivate(adapter, "formatExpiryDate", "  "));

        // inferCardBrand branches
        assertEquals("VISA", invokePrivate(adapter, "inferCardBrand", "4111111111111111"));
        assertEquals("MASTERCARD", invokePrivate(adapter, "inferCardBrand", "5111111111111118"));
        assertEquals("AMEX", invokePrivate(adapter, "inferCardBrand", "371449635398431"));
        assertEquals("DINERS", invokePrivate(adapter, "inferCardBrand", "30569309025904"));
        assertThrows(IllegalArgumentException.class, () -> invokePrivate(adapter, "inferCardBrand", "6011000990139424"));

        // toCopAmount branches
        assertEquals("100000", invokePrivate(adapter, "toCopAmount", 100000.0).toString());
        assertThrows(IllegalArgumentException.class, () -> invokePrivate(adapter, "toCopAmount", 100000.5));

        // safeBody branches (null + truncation)
        assertEquals("", invokePrivate(adapter, "safeBody", (Object) null));
        String longBody = "a".repeat(1200);
        String safe = (String) invokePrivate(adapter, "safeBody", longBody);
        assertTrue(safe.length() <= 1003);
        assertTrue(safe.endsWith("..."));
    }

    @Test
    void mapToBankResponseCode_ShouldCoverSwitchBranches() throws Exception {
        assertEquals(BankResponseCode.APPROVED, invokePrivate(adapter, "mapToBankResponseCode", "ANY", "APPROVED"));
        assertEquals(BankResponseCode.PENDING, invokePrivate(adapter, "mapToBankResponseCode", "ANY", "PENDING"));
        assertEquals(BankResponseCode.INSUFFICIENT_FUNDS, invokePrivate(adapter, "mapToBankResponseCode", "INSUFFICIENT_FUNDS", "DECLINED"));
        assertEquals(BankResponseCode.INVALID_CARD, invokePrivate(adapter, "mapToBankResponseCode", "INVALID_CARD", "DECLINED"));
        assertEquals(BankResponseCode.EXPIRED_CARD, invokePrivate(adapter, "mapToBankResponseCode", "EXPIRED", "DECLINED"));
        assertEquals(BankResponseCode.DECLINED, invokePrivate(adapter, "mapToBankResponseCode", "OTHER", "DECLINED"));
        assertEquals(BankResponseCode.UNKNOWN_ERROR, invokePrivate(adapter, "mapToBankResponseCode", "ANY", "SOMETHING_ELSE"));
    }



    @Test
    void mapToGatewayResponse_WhenNull_ShouldReturnErrorResponse() throws Exception {
        GatewayResponse response = (GatewayResponse) invokePrivate(adapter, "mapToGatewayResponse", (Object) null);

        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertEquals(BankResponseCode.ERROR, response.getBankResponseCode());
        assertEquals("ERROR", response.getResponseCode());
        assertEquals("COP", response.getCurrency());
        assertEquals(0.0, response.getProcessedAmount());
    }

    @Test
    void processPayment_WhenAmountIsNotNumeric_ShouldReturnZeroProcessedAmount() {
        CreatePaymentRequest request = buildCardPaymentRequest(
                "ORDER-AMOUNT-NA", "CLIENT-6", "STORE-6", 100000.0,
                BankPaymentType.CREDIT_CARD, "4111111111111111", "12/25", "123", "John Doe"
        );

        String jsonResponse = """
                {
                  "code": "SUCCESS",
                  "transactionResponse": {
                    "transactionId": "TX-AMT-1",
                    "state": "APPROVED",
                    "authorizationCode": "AUTH-AMT",
                    "responseCode": "APPROVED",
                    "responseMessage": "APPROVED",
                    "additionalInfo": {
                      "payments": [
                        { "type": "CREDIT_CARD", "amount": "not-a-number", "currency": "COP" }
                      ]
                    }
                  }
                }
                """;

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(jsonResponse));

        GatewayResponse response = adapter.processPayment(request);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals(0.0, response.getProcessedAmount());
        assertEquals("COP", response.getCurrency());
    }
// ---------------------------
    // Helpers
    // ---------------------------

    private CreatePaymentRequest buildCardPaymentRequest(
            String orderId,
            String clientId,
            String storeId,
            double originalAmount,
            BankPaymentType bankPaymentType,
            String pan,
            String expiryDate,
            String cvv,
            String holderName
    ) {
        Bank bankMethod = new Bank();
        bankMethod.setPaymentMethodType(PaymentMethodType.BANK);
        bankMethod.setBankPaymentType(bankPaymentType);
        bankMethod.setBankAccountType(BankAccountType.SAVINGS_ACCOUNT);

        BankDetails bankDetails = new BankDetails();
        bankDetails.setBankName("Bancolombia");
        bankDetails.setBankPaymentType(bankPaymentType);
        bankDetails.setBankAccountType(BankAccountType.SAVINGS_ACCOUNT);
        bankDetails.setAccountNumber(pan);
        bankDetails.setExpiryDate(expiryDate);
        bankDetails.setCvv(cvv);
        bankDetails.setCardHolderName(holderName);

        return new CreatePaymentRequest(orderId, clientId, storeId, originalAmount, bankMethod, bankDetails);
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            var field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Object invokePrivate(Object target, String methodName, Object... args) throws Exception {
        Method m = findMethod(target.getClass(), methodName, args);
        m.setAccessible(true);
        try {
            return m.invoke(target, args);
        } catch (InvocationTargetException ite) {
            if (ite.getTargetException() instanceof RuntimeException re) {
                throw re;
            }
            if (ite.getTargetException() instanceof Error err) {
                throw err;
            }
            throw ite;
        }
    }

    private Method findMethod(Class<?> clazz, String methodName, Object[] args) throws NoSuchMethodException {
        for (Method m : clazz.getDeclaredMethods()) {
            if (!m.getName().equals(methodName)) continue;
            if (m.getParameterCount() != args.length) continue;
            return m;
        }
        throw new NoSuchMethodException("No method " + methodName + " with " + args.length + " args");
    }

    private Object invokeOnObject(Object target, String methodName) throws Exception {
        Method m = target.getClass().getDeclaredMethod(methodName);
        m.setAccessible(true);
        return m.invoke(target);
    }
}