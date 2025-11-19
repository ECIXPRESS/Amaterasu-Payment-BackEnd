package ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model;

import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model.Enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Payment {
    private String OrderId;
    private String clientId;
    private String storeId;
    private double originalAmount;
    private double finalAmount;
    private PaymentMethod paymentMethod;
    private PaymentStatus paymentStatus;
    private List<String> appliedPromotions;
    private BankValidationResult bankValidationResult;
    private GatewayResponse gatewayResponse;
}
