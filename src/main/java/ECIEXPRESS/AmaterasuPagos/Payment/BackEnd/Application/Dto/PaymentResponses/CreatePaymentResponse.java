package ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Application.Dto.PaymentResponses;

import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model.Enums.PaymentStatus;

public record CreatePaymentResponse(
        String receiptId,
        String orderId,
        String storeId,
        double finalAmount,
        PaymentStatus paymentStatus
) {
}
