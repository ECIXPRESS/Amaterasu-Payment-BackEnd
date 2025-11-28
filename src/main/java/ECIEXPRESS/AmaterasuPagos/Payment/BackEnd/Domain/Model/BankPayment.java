package ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model;

import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Application.Dto.Context;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class BankPayment extends Payment {
    private BankValidationResult bankValidationResult;
    private GatewayResponse gatewayResponse;

    @Override
    public Payment createPayment(Context context) {
        BankPayment bankPayment = new BankPayment();
        bankPayment.setOrderId(context.paymentDto().orderId());
        bankPayment.setClientId(context.paymentDto().clientId());
        bankPayment.setStoreId(context.paymentDto().storeId());
        bankPayment.setOriginalAmount(context.paymentDto().originalAmount());
        bankPayment.setFinalAmount(context.paymentDto().finalAmount());
        bankPayment.setPaymentMethod(context.paymentDto().paymentMethod());
        bankPayment.setPaymentStatus(context.paymentDto().paymentStatus());
        bankPayment.setTimeStamps(context.paymentDto().timeStamps());
        bankPayment.setAppliedPromotions(context.paymentDto().appliedPromotions());

        bankPayment.setBankValidationResult(context.bankValidationResult());
        bankPayment.setGatewayResponse(context.gatewayResponse());

        return bankPayment;
    }
}
