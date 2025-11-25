package ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model;

import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model.Enums.BankAccountType;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model.Enums.BankPaymentType;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model.Enums.PaymentMethodType;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class Bank extends PaymentMethod {
    private PaymentMethodType paymentMethodType;
    private BankPaymentType bankPaymentType;
    private BankAccountType bankAccountType;
    private String bankReceiptNumber;
    private String bankName;

    public PaymentMethod createPaymentMethod() {
        Bank bank = new Bank();
        bank.setPaymentMethodType(PaymentMethodType.BANK);
        return bank;
    }

    @Override
    public String getBankReceiptNumber() {
        return bankReceiptNumber;
    }

    @Override
    public String getBankName() {
        return bankName;
    }
}
