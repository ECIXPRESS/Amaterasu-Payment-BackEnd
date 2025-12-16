package ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Application.Mappers;

import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Application.Dto.PaymentDto;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Infrastructure.Web.Dto.PaymentRequests.CreatePaymentRequest;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Infrastructure.Web.Dto.PaymentResponses.CreatePaymentResponse;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model.Enums.PaymentStatus;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model.TimeStamps;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Infrastructure.Clients.Promotion.Dto.PromotionResponses.ApplyPromotionResponse;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Infrastructure.Clients.Receipt.Dto.ReceiptResponses.CreateReceiptResponse;

public class ApplicationMapper {
    public static PaymentDto createCashPaymentDto(CreatePaymentRequest createPaymentRequest, ApplyPromotionResponse applyPromotionResponse, TimeStamps timeStamps){
        return new PaymentDto(
                createPaymentRequest.orderId(),
                createPaymentRequest.clientId(),
                createPaymentRequest.storeId(),
                createPaymentRequest.originalAmount(),
                applyPromotionResponse.finalAmount(),
                createPaymentRequest.paymentMethod(),
                PaymentStatus.PENDING,
                timeStamps,
                applyPromotionResponse.appliedPromotions());
    }

    public static PaymentDto createBankPaymentDto(CreatePaymentRequest createPaymentRequest, ApplyPromotionResponse applyPromotionResponse, TimeStamps timeStamps){
        return new PaymentDto(
                createPaymentRequest.orderId(),
                createPaymentRequest.clientId(),
                createPaymentRequest.storeId(),
                createPaymentRequest.originalAmount(),
                applyPromotionResponse.finalAmount(),
                createPaymentRequest.paymentMethod(),
                PaymentStatus.COMPLETED,
                timeStamps,
                applyPromotionResponse.appliedPromotions());
    }

    public static CreatePaymentRequest updatePaymentRequest(CreatePaymentRequest createPaymentRequest, ApplyPromotionResponse applyPromotionResponse){
        return new CreatePaymentRequest(
                createPaymentRequest.orderId(),
                createPaymentRequest.clientId(),
                createPaymentRequest.storeId(),
                applyPromotionResponse.finalAmount(),
                createPaymentRequest.paymentMethod(),
                createPaymentRequest.bankDetails());
    }

    public static CreatePaymentResponse receiptResponseToPaymentResponse(CreateReceiptResponse receiptResponse){
        return new CreatePaymentResponse(
                receiptResponse.receiptId(),
                receiptResponse.orderId(),
                receiptResponse.storeId(),
                receiptResponse.finalAmount(),
                receiptResponse.receiptStatus(),
                receiptResponse.qrCode());
    }
}
