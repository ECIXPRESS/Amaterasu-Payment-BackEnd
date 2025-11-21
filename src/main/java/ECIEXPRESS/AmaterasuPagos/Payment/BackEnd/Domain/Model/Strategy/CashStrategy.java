package ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model.Strategy;

import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Application.Dto.PaymentResponses.CreatePaymentResponse;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Application.Dto.StrategyContext;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model.BankValidationResult;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model.Enums.PaymentStatus;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model.GatewayResponse;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model.PaymentMethod;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CashStrategy implements  PaymentStrategy {
    private String orderId;
    private String clientId;
    private String storeId;
    private double originalAmount;
    private double finalAmount;
    private PaymentMethod paymentMethod;
    private PaymentStatus paymentStatus;
    private List<String> appliedPromotions;

    public CreatePaymentResponse createPayment(StrategyContext strategyContext){
        return null;
    }
}
