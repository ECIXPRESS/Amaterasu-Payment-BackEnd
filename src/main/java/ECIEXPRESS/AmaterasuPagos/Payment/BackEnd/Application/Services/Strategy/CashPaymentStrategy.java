package ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Application.Services.Strategy;

import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Application.Dto.PaymentDto;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Application.Dto.PaymentRequests.CreatePaymentRequest;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Application.Dto.PaymentResponses.CreatePaymentResponse;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Application.Dto.StrategyContext;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model.CashPayment;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model.Payment;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model.TimeStamps;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Ports.PromotionProvider;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Ports.ReceiptProvider;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Infrastructure.Clients.Promotion.Dto.PromotionResponses.PromotionResponse;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Infrastructure.Clients.Receipt.Dto.ReceiptResponses.CreateReceiptResponse;

import java.util.Date;

import static ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Application.Mappers.ApplicationMapper.*;

public class CashPaymentStrategy implements PaymentStrategy{
    private PromotionProvider promotionProvider;
    private ReceiptProvider receiptProvider;
    @Override
    public CreatePaymentResponse createPayment(CreatePaymentRequest createPaymentRequest){
        Payment payment = new CashPayment();
        CreatePaymentResponse createPaymentResponse;
        CreateReceiptResponse receiptResponse;
        TimeStamps timeStamps = new TimeStamps();
        timeStamps.setCreatedAt(new Date().toString());
        PromotionResponse promotionResponse = promotionProvider.applyPromotions(createPaymentRequest.orderId());
        createPaymentRequest = updatePaymentRequest(createPaymentRequest, promotionResponse);
        PaymentDto paymentDto = createCashPaymentDto(createPaymentRequest, promotionResponse, timeStamps);
        payment = payment.createPayment(new StrategyContext(paymentDto, null, null));
        receiptResponse = receiptProvider.createReceipt(payment);
        createPaymentResponse = ReceiptResponseToPaymentResponse(receiptResponse);
        return createPaymentResponse;
    }
}
