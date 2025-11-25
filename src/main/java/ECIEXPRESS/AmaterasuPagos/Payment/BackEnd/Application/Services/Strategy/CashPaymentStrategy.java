package ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Application.Services.Strategy;

import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Application.Dto.PaymentDto;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Application.Mappers.ApplicationMapper;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Infrastructure.Web.Dto.PaymentRequests.CreatePaymentRequest;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Infrastructure.Web.Dto.PaymentResponses.CreatePaymentResponse;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Application.Dto.Context;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model.CashPayment;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model.Payment;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model.TimeStamps;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Ports.PromotionProvider;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Ports.ReceiptProvider;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Infrastructure.Clients.Promotion.Dto.PromotionResponses.PromotionResponse;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Infrastructure.Clients.Receipt.Dto.ReceiptResponses.CreateReceiptResponse;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Date;

import static ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Application.Mappers.ApplicationMapper.*;

@AllArgsConstructor
@NoArgsConstructor
@Service
public class CashPaymentStrategy implements PaymentStrategy{
    private PromotionProvider promotionProvider;
    private ReceiptProvider receiptProvider;
    @Override
    public CreatePaymentResponse createPayment(CreatePaymentRequest createPaymentRequest){
        Payment payment = new CashPayment();
        TimeStamps timeStamps = new TimeStamps();
        timeStamps.setCreatedAt(new Date().toString());
        PromotionResponse promotionResponse = promotionProvider.applyPromotions(createPaymentRequest.orderId());
        createPaymentRequest = updatePaymentRequest(createPaymentRequest, promotionResponse);
        PaymentDto paymentDto = createCashPaymentDto(createPaymentRequest, promotionResponse, timeStamps);
        payment = payment.createPayment(new Context(paymentDto, null, null));
        CreateReceiptResponse receiptResponse = receiptProvider.createReceipt(payment);
        return ApplicationMapper.receiptResponseToPaymentResponse(receiptResponse);
    }
}
