package ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model;

import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Application.Dto.Context;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class WalletPayment extends Payment {

    @Override
    public Payment createPayment(Context context) {
        WalletPayment walletPayment = new WalletPayment();
        walletPayment.setOrderId(context.paymentDto().orderId());
        walletPayment.setClientId(context.paymentDto().clientId());
        walletPayment.setStoreId(context.paymentDto().storeId());
        walletPayment.setOriginalAmount(context.paymentDto().originalAmount());
        walletPayment.setFinalAmount(context.paymentDto().finalAmount());
        walletPayment.setPaymentMethod(context.paymentDto().paymentMethod());
        walletPayment.setPaymentStatus(context.paymentDto().paymentStatus());
        walletPayment.setTimeStamps(context.paymentDto().timeStamps());
        walletPayment.setAppliedPromotions(context.paymentDto().appliedPromotions());
        return walletPayment;
    }
}
