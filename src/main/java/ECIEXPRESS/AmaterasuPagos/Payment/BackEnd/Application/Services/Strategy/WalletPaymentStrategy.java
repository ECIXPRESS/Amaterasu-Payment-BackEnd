package ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Application.Services.Strategy;

import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Application.Dto.PaymentDto;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Infrastructure.Web.Dto.PaymentRequests.CreatePaymentRequest;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Infrastructure.Web.Dto.PaymentResponses.CreatePaymentResponse;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Application.Dto.Context;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model.Payment;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model.TimeStamps;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model.WalletPayment;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Ports.PromotionProvider;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Ports.ReceiptProvider;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Ports.WalletProvider;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Infrastructure.Clients.Promotion.Dto.PromotionResponses.PromotionResponse;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Infrastructure.Clients.Receipt.Dto.ReceiptResponses.CreateReceiptResponse;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Infrastructure.Clients.Wallet.Dto.WalletResponses.CreateWalletResponse;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Date;

import static ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Application.Mappers.ApplicationMapper.*;

@AllArgsConstructor
@NoArgsConstructor
@Service
public class WalletPaymentStrategy implements PaymentStrategy{
    private PromotionProvider promotionProvider;
    private WalletProvider walletProvider;
    private ReceiptProvider receiptProvider;
    @Override
    public CreatePaymentResponse createPayment(CreatePaymentRequest createPaymentRequest){
        Payment payment = new WalletPayment();
        CreatePaymentResponse createPaymentResponse;
        CreateReceiptResponse receiptResponse;
        TimeStamps timeStamps = new TimeStamps();
        timeStamps.setCreatedAt(new Date().toString());
        PromotionResponse promotionResponse = promotionProvider.applyPromotions(createPaymentRequest.orderId());
        createPaymentRequest = updatePaymentRequest(createPaymentRequest, promotionResponse);
        CreateWalletResponse createWalletResponse = walletProvider.processPayment(createPaymentRequest);
        timeStamps.setPaymentProcessedAt(new Date().toString());
        PaymentDto paymentDto = createBankPaymentDto(createPaymentRequest, promotionResponse, timeStamps);
        payment = payment.createPayment(new Context(paymentDto, null, null));
        receiptResponse = receiptProvider.createReceipt(payment);
        createPaymentResponse = ReceiptResponseToPaymentResponse(receiptResponse);
        return createPaymentResponse;
    }
}
