package ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model;

import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model.Enums.PaymentMethodType;
import lombok.Data;

@Data
public class Wallet implements PaymentMethod {
    private PaymentMethodType paymentMethodType;

    public PaymentMethod createPaymentMethod() {
        Wallet wallet = new Wallet();
        wallet.setPaymentMethodType(PaymentMethodType.WALLET);
        return wallet;
    }
    //TODO: implementar
}
