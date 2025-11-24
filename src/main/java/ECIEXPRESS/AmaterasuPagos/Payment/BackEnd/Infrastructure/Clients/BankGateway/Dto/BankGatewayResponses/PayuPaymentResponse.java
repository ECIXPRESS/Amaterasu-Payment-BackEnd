package ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Infrastructure.Clients.BankGateway.Dto.BankGatewayResponses;

import lombok.Data;

import java.util.List;

@Data
public class PayuPaymentResponse {
    private String code;
    private String error;
    private PayuTransactionResponse transactionResponse;

    @Data
    public static class PayuTransactionResponse {
        private String orderId;
        private String transactionId;
        private String state;
        private String paymentNetworkResponseCode;
        private String paymentNetworkResponseErrorMessage;
        private String trazabilityCode;
        private String authorizationCode;
        private String pendingReason;
        private String responseCode;
        private String errorCode;
        private String responseMessage;
        private String transactionDate;
        private String transactionTime;
        private String operationDate;
        private String extraParameters;
        private PayuAdditionalInfo additionalInfo;
    }

    @Data
    public static class PayuAdditionalInfo {
        private List<PayuPayment> payments;
    }

    @Data
    public static class PayuPayment {
        private String type;
        private String reason;
        private String amount;
        private String currency;
    }
}
