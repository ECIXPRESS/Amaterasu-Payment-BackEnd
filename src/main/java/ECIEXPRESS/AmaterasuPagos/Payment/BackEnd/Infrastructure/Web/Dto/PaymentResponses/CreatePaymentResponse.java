package ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Infrastructure.Web.Dto.PaymentResponses;

import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model.Enums.PaymentStatus;

public record CreatePaymentResponse(
        String receiptId,
        String orderId,
        String storeId,
        double finalAmount,
        PaymentStatus paymentStatus
) {
}
