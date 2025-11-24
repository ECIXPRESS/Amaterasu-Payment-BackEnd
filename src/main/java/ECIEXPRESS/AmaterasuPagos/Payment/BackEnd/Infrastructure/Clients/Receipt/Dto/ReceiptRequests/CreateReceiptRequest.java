package ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Infrastructure.Clients.Receipt.Dto.ReceiptRequests;

import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model.Enums.PaymentStatus;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model.GatewayResponse;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model.PaymentMethod;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model.TimeStamps;

import java.util.List;

public record CreateReceiptRequest(
        String orderId,
        String clientId,
        String storeId,
        double finalAmount,
        PaymentMethod paymentMethod,
        PaymentStatus paymentStatus,
        TimeStamps timeStamps,
        List<String> appliedPromotions) {
}
