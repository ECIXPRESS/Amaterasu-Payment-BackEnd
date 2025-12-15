package ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Application.Services.Strategy;

import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Application.Dto.Context;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Application.Dto.PaymentDto;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Application.Mappers.ApplicationMapper;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model.Payment;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model.TimeStamps;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model.WalletPayment;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Ports.PromotionProvider;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Ports.ReceiptProvider;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Ports.WalletProvider;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Infrastructure.Clients.Promotion.Dto.PromotionResponses.ApplyPromotionResponse;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Infrastructure.Clients.Receipt.Dto.ReceiptResponses.CreateReceiptResponse;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Infrastructure.Clients.Wallet.Dto.WalletResponses.PayWithWalletResponse;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Infrastructure.Web.Dto.PaymentRequests.CreatePaymentRequest;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Infrastructure.Web.Dto.PaymentResponses.CreatePaymentResponse;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Utils.DateUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Date;

import static ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Application.Mappers.ApplicationMapper.createBankPaymentDto;
import static ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Application.Mappers.ApplicationMapper.updatePaymentRequest;

@Service
@Slf4j
@RequiredArgsConstructor
public class WalletPaymentStrategy implements PaymentStrategy {

    private final PromotionProvider promotionProvider;
    private final WalletProvider walletProvider;
    private final ReceiptProvider receiptProvider;

    @Override
    public CreatePaymentResponse createPayment(CreatePaymentRequest createPaymentRequest) {
        final long startMs = System.currentTimeMillis();
        final String orderId = createPaymentRequest.orderId();
        log.info("Starting WALLET payment flow: orderId={}", orderId);

        Payment payment = new WalletPayment();

        TimeStamps timeStamps = new TimeStamps();
        timeStamps.setCreatedAt(DateUtils.formatDate(new Date(), DateUtils.TIMESTAMP_FORMAT));

        ApplyPromotionResponse applyPromotionResponse = promotionProvider.applyPromotions(createPaymentRequest.orderId());
        log.info("Promotions service responded: orderId={}, applyPromotionResponse={}", orderId, applyPromotionResponse);
        createPaymentRequest = updatePaymentRequest(createPaymentRequest, applyPromotionResponse);

        PayWithWalletResponse payWithWalletResponse = walletProvider.processPayment(createPaymentRequest);
        log.info("Wallet processed payment: orderId={}, walletResponsePresent={}", orderId, payWithWalletResponse != null);
        timeStamps.setPaymentProcessedAt(DateUtils.formatDate(new Date(), DateUtils.TIMESTAMP_FORMAT));

        PaymentDto paymentDto = createBankPaymentDto(createPaymentRequest, applyPromotionResponse, timeStamps);
        payment = payment.createPayment(new Context(paymentDto, null, null));

        CreateReceiptResponse receiptResponse = receiptProvider.createReceipt(payment);
        log.info("Receipt created: orderId={}", orderId);
        log.info("Payment flow completed: orderId={}, elapsedMs={}", orderId, System.currentTimeMillis() - startMs);

        return ApplicationMapper.receiptResponseToPaymentResponse(receiptResponse);
    }
}
