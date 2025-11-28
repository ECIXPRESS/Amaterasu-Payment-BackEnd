package ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Infrastructure.Web.Dto.PaymentResponses;

import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model.Enums.ReceiptStatus;

public record CreatePaymentResponse(
        String receiptId,
        String orderId,
        String storeId,
        double finalAmount,
        ReceiptStatus receiptStatus
) {
}
