package ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Infrastructure.Clients.Receipt.Dto.ReceiptResponses;

import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model.Enums.PaymentStatus;

public record CreateReceiptResponse(
        String receiptId,
        String orderId,
        String storeId,
        double finalAmount,
        PaymentStatus paymentStatus
){
}
