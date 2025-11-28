package ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model;

import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model.Enums.PaymentMethodType;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class Wallet extends PaymentMethod {
    private PaymentMethodType paymentMethodType;

    public PaymentMethod createPaymentMethod() {
        Wallet wallet = new Wallet();
        wallet.setPaymentMethodType(PaymentMethodType.WALLET);
        return wallet;
    }
}
