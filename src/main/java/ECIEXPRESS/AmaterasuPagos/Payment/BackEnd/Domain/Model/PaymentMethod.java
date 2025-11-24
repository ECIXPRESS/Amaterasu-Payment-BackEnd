package ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model;


import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model.Enums.PaymentMethodType;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = Cash.class, name = "CASH"),
        @JsonSubTypes.Type(value = Wallet.class, name = "WALLET"),
        @JsonSubTypes.Type(value = Bank.class, name = "BANK")
})
public interface PaymentMethod {
    PaymentMethodType getPaymentMethodType();
    void setPaymentMethodType(PaymentMethodType paymentMethodType);
}
