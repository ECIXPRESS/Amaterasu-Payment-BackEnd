package ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Application.Dto.PaymentResponses;

public record CreatePaymentResponse(
        String receiptId,
        String orderId,
        String storeId,
        double finalAmount
) {
}
