package ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model;

import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Application.Dto.Context;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class CashPayment extends Payment {

    @Override
    public Payment createPayment(Context context) {
        CashPayment cashPayment = new CashPayment();
        cashPayment.setOrderId(context.paymentDto().orderId());
        cashPayment.setClientId(context.paymentDto().clientId());
        cashPayment.setStoreId(context.paymentDto().storeId());
        cashPayment.setOriginalAmount(context.paymentDto().originalAmount());
        cashPayment.setFinalAmount(context.paymentDto().finalAmount());
        cashPayment.setPaymentMethod(context.paymentDto().paymentMethod());
        cashPayment.setPaymentStatus(context.paymentDto().paymentStatus());
        cashPayment.setTimeStamps(context.paymentDto().timeStamps());
        cashPayment.setAppliedPromotions(context.paymentDto().appliedPromotions());
        return cashPayment;
    }
}
