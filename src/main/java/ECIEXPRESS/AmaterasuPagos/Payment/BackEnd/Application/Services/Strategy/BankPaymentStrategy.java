package ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Application.Services.Strategy;

import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Application.Dto.PaymentDto;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Application.Dto.PaymentRequests.CreatePaymentRequest;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Application.Dto.PaymentResponses.CreatePaymentResponse;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Application.Dto.Context;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Application.Services.Event.PaymentEventPublisher;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Application.Services.ValidationService;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model.*;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Ports.BankGatewayProvider;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Ports.PromotionProvider;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Ports.ReceiptProvider;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Infrastructure.Clients.Promotion.Dto.PromotionResponses.PromotionResponse;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Infrastructure.Clients.Receipt.Dto.ReceiptResponses.CreateReceiptResponse;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;


import java.util.Date;

import static ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Application.Mappers.ApplicationMapper.*;

@AllArgsConstructor
@NoArgsConstructor
@Service
public class BankPaymentStrategy implements PaymentStrategy{
    private PromotionProvider promotionProvider;
    private BankGatewayProvider bankGatewayProvider;
    private ValidationService validationService;
    private ReceiptProvider receiptProvider;
    private PaymentEventPublisher paymentEventPublisher;
    @Override
    public CreatePaymentResponse createPayment(CreatePaymentRequest createPaymentRequest){
        Payment payment = new BankPayment();
        CreatePaymentResponse createPaymentResponse;
        CreateReceiptResponse receiptResponse;
        TimeStamps timeStamps = new TimeStamps();
        timeStamps.setCreatedAt(new Date().toString());
        BankValidationResult bankValidationResult = validationService.createValidation(createPaymentRequest.bankDetails());
        PromotionResponse promotionResponse = promotionProvider.applyPromotions(createPaymentRequest.orderId());
        createPaymentRequest = updatePaymentRequest(createPaymentRequest, promotionResponse);
        PaymentDto pendingPaymentDto = createBankPaymentDto(createPaymentRequest, promotionResponse, timeStamps);
        paymentEventPublisher.publishPaymentCreated(pendingPaymentDto);
        GatewayResponse gatewayResponse = bankGatewayProvider.processPayment(createPaymentRequest);
        timeStamps.setPaymentProcessedAt(new Date().toString());
        PaymentDto paymentDto = createBankPaymentDto(createPaymentRequest, promotionResponse, timeStamps);
        payment = payment.createPayment(new Context(paymentDto, gatewayResponse, bankValidationResult));

        if (gatewayResponse.isSuccess()) {
            paymentEventPublisher.publishPaymentCompleted(paymentDto);
        } else {
            paymentEventPublisher.publishPaymentFailed(paymentDto);
        }

        receiptResponse = receiptProvider.createReceipt(payment);
        createPaymentResponse = ReceiptResponseToPaymentResponse(receiptResponse);
        return createPaymentResponse;
    }
}
