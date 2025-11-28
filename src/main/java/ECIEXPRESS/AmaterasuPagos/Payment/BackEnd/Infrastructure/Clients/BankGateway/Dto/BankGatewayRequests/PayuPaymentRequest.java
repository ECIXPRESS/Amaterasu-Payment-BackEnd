package ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Infrastructure.Clients.BankGateway.Dto.BankGatewayRequests;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

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
        private CreditCard creditCard;
        private String type;
        private String paymentMethod;
        private String paymentCountry;
        private Payer payer;

        @Data
        @Builder
        public static class Order {
            @JsonProperty("accountId")
            private String accountId;
            private String referenceCode;
            private String description;
            private String language;
            private String notifyUrl;
            private Map<String, Amount> additionalValues;
            private Buyer buyer;
            private String signature;
        }

        @Data
        @Builder
        public static class CreditCard {
            private String number;
            private String securityCode;
            private String expirationDate;
            private String name;
            private Boolean processWithoutCvv2;
        }

        @Data
        @Builder
        public static class Payer {
            private String emailAddress;
            private String fullName;
            private String contactPhone;
            private String dniNumber;
            private BillingAddress billingAddress;
        }

        @Data
        @Builder
        public static class BillingAddress {
            private String street1;
            private String city;
            private String state;
            private String country;
            private String postalCode;
            private String phone;
        }

        @Data
        @Builder
        public static class Buyer {
            private String merchantBuyerId;
            private String fullName;
            private String emailAddress;
            private String contactPhone;
            private String dniNumber;
            private ShippingAddress shippingAddress;
        }

        @Data
        @Builder
        public static class ShippingAddress {
            private String street1;
            private String city;
            private String state;
            private String country;
            private String postalCode;
            private String phone;
        }
    }

    @Data
    @Builder
    public static class Amount {
        private String value;
        private String currency;
    }
}
