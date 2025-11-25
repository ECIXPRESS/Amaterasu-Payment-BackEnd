package ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model;

import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model.Enums.PaymentMethodType;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class Cash extends PaymentMethod {
    private PaymentMethodType paymentMethodType;

    public PaymentMethod createPaymentMethod() {
        Cash cash = new Cash();
        cash.setPaymentMethodType(PaymentMethodType.CASH);
        return cash;
    }
}
