package ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Application.Mappers;

import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Application.Dto.PaymentDto;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Application.Dto.PaymentRequests.CreatePaymentRequest;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Application.Dto.PaymentResponses.CreatePaymentResponse;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model.Enums.PaymentStatus;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model.TimeStamps;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Infrastructure.Clients.Promotion.Dto.PromotionResponses.PromotionResponse;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Infrastructure.Clients.Receipt.Dto.ReceiptResponses.CreateReceiptResponse;

public class ApplicationMapper {
    public static PaymentDto createCashPaymentDto(CreatePaymentRequest createPaymentRequest, PromotionResponse promotionResponse, TimeStamps timeStamps){
        return new PaymentDto(
                createPaymentRequest.orderId(),
                createPaymentRequest.clientId(),
                createPaymentRequest.storeId(),
                createPaymentRequest.originalAmount(),
                promotionResponse.finalAmount(),
                createPaymentRequest.paymentMethod(),
                PaymentStatus.PENDING,
                timeStamps,
                promotionResponse.appliedPromotions());
    }

    public static PaymentDto createBankPaymentDto(CreatePaymentRequest createPaymentRequest, PromotionResponse promotionResponse, TimeStamps timeStamps){
        return new PaymentDto(
                createPaymentRequest.orderId(),
                createPaymentRequest.clientId(),
                createPaymentRequest.storeId(),
                createPaymentRequest.originalAmount(),
                promotionResponse.finalAmount(),
                createPaymentRequest.paymentMethod(),
                PaymentStatus.COMPLETED,
                timeStamps,
                promotionResponse.appliedPromotions());
    }

    public static CreatePaymentRequest updatePaymentRequest(CreatePaymentRequest createPaymentRequest, PromotionResponse promotionResponse){
        return new CreatePaymentRequest(
                createPaymentRequest.orderId(),
                createPaymentRequest.clientId(),
                createPaymentRequest.storeId(),
                promotionResponse.finalAmount(),
                createPaymentRequest.paymentMethod(),
                createPaymentRequest.bankDetails());
    }

    public static CreatePaymentResponse ReceiptResponseToPaymentResponse(CreateReceiptResponse receiptResponse){
        return new CreatePaymentResponse(
                receiptResponse.receiptId(),
                receiptResponse.orderId(),
                receiptResponse.storeId(),
                receiptResponse.finalAmount(),
                receiptResponse.paymentStatus());
    }
}
