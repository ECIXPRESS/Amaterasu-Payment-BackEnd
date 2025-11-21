package ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Application.Dto.PaymentRequests;

import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model.BankDetails;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model.PaymentMethod;

public record CreatePaymentRequest(
        String orderId,
        String clientId,
        String storeId,
        double originalAmount,
        PaymentMethod paymentMethod,
        BankDetails bankDetails
        ) {

}

