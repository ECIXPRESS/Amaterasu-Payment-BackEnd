package ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model;

import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model.Enums.PaymentMethodType;
import lombok.Data;

@Data
public class Cash implements PaymentMethod {
    private PaymentMethodType paymentMethodType;

    public PaymentMethod createPaymentMethod() {
        Cash cash = new Cash();
        cash.setPaymentMethodType(PaymentMethodType.CASH);
        return cash;
    }
    //TODO: implementar
}
