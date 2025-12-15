package ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Infrastructure.Clients.BankGateway.Dto.BankGatewayRequests;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
public class PayuPaymentRequest {
    private String language;
    private String command;
    private Boolean test;
    private Merchant merchant;
    private Transaction transaction;

    @Data
    @Builder
    public static class Merchant {
        @JsonProperty("apiLogin")
        private String apiLogin;
        @JsonProperty("apiKey")
        private String apiKey;
    }

    @Data
    @Builder
    public static class Transaction {
        private Order order;

        /**
         * Use either creditCard or debitCard depending on the payment method.
         * Do not send both.
         */
        private CreditCard creditCard;
        private DebitCard debitCard;

        private String type;
        private String paymentMethod;
        private String paymentCountry;
        private Payer payer;

        // Anti-fraud fields (mandatory for card payments in PayU Payments API - Colombia docs)
        private String deviceSessionId;
        private String ipAddress;
        private String cookie;
        private String userAgent;

        // Extra parameters (e.g., INSTALLMENTS_NUMBER)
        private Map<String, Object> extraParameters;
    }

    @Data
    @Builder
    public static class Order {
        @JsonProperty("accountId")
        private String accountId;

        private String referenceCode;
        private String description;
        private String language;
        private String signature;
        private Buyer buyer;

        /**
         * PayU expects keys such as TX_VALUE, TX_TAX, TX_TAX_RETURN_BASE
         */
        private Map<String, AdditionalValue> additionalValues;

        // Optional confirmation/notification URL
        private String notifyUrl;
    }

    @Data
    @Builder
    public static class AdditionalValue {
        /**
         * For Colombia, TX_VALUE must not include decimals. Send it as an integer value (BigDecimal scale 0).
         */
        private BigDecimal value;
        private String currency;
    }

    @Data
    @Builder
    public static class CreditCard {
        private String number;
        private String securityCode;
        private String expirationDate; // YYYY/MM
        private String name;
        private Boolean processWithoutCvv2;
    }

    @Data
    @Builder
    public static class DebitCard {
        private String number;
        private String securityCode;
        private String expirationDate; // YYYY/MM
        private String name;
        private Boolean processWithoutCvv2;
    }

    @Data
    @Builder
    public static class Buyer {
        private String merchantBuyerId;
        private String fullName;
        private String emailAddress;
        private String contactPhone;
        private String dniNumber;
        private Address shippingAddress;
    }

    @Data
    @Builder
    public static class Payer {
        private String merchantPayerId;
        private String fullName;
        private String emailAddress;
        private String contactPhone;
        private String dniNumber;
        private Address billingAddress;
    }

    @Data
    @Builder
    public static class Address {
        private String street1;
        private String city;
        private String state;
        private String country;
        private String postalCode;
        private String phone;
    }
}
