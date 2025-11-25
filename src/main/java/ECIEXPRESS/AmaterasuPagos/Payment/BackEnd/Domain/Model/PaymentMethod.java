package ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model;

import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model.Enums.PaymentMethodType;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = Cash.class, name = "CASH"),
        @JsonSubTypes.Type(value = Wallet.class, name = "WALLET"),
        @JsonSubTypes.Type(value = Bank.class, name = "BANK")
})
@Data
@AllArgsConstructor
@NoArgsConstructor
public abstract class PaymentMethod {
    private PaymentMethodType paymentMethodType;

    public abstract PaymentMethod createPaymentMethod();

}
