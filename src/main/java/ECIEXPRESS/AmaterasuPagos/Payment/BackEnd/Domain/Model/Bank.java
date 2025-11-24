package ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model;

import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model.Enums.BankAccountType;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model.Enums.BankPaymentType;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model.Enums.PaymentMethodType;
import lombok.Data;

@Data
public class Bank implements PaymentMethod {
    private PaymentMethodType paymentMethodType;
    private BankPaymentType bankPaymentType;
    private BankAccountType bankAccountType;
    private String bankName;

    public PaymentMethod createPaymentMethod() {
        Bank bank = new Bank();
        bank.setPaymentMethodType(PaymentMethodType.BANK);
        return bank;
    }
    //TODO: implementar
}
