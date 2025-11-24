package ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model;

import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Application.Dto.Context;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model.Enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public abstract class Payment {
    private String orderId;
    private String clientId;
    private String storeId;
    private double originalAmount;
    private double finalAmount;
    private PaymentMethod paymentMethod;
    private PaymentStatus paymentStatus;
    private TimeStamps timeStamps;
    private List<String> appliedPromotions;
    public abstract Payment createPayment(Context context);

}
