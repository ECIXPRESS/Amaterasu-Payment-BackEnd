package ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Application.Dto;

import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model.BankDetails;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model.Enums.PaymentStatus;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model.PaymentMethod;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model.TimeStamps;

import java.util.List;

public record PaymentDto (
        String orderId,
        String clientId,
        String storeId,
        double originalAmount,
        double finalAmount,
        PaymentMethod paymentMethod,
        PaymentStatus paymentStatus,
         TimeStamps timeStamps,
        List<String> appliedPromotions){
}
