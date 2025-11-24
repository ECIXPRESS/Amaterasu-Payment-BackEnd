package ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model;

import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model.Enums.BankAccountType;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model.Enums.BankPaymentType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public abstract class Bank extends PaymentMethod{
    private BankPaymentType bankPaymentType;
    private BankAccountType bankAccountType;
    private String BankName;
    @Override
    public PaymentMethod createPaymentMethod() {
        return null;
    }
    //TODO: implementar
}
