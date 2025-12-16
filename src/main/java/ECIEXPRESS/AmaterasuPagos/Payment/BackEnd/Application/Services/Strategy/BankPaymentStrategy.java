package ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Application.Services.Strategy;

import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Application.Dto.Context;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Application.Dto.PaymentDto;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Application.Mappers.ApplicationMapper;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Application.Services.ValidationService;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model.*;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Ports.BankGatewayProvider;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Ports.PromotionProvider;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Ports.ReceiptProvider;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Infrastructure.Clients.Promotion.Dto.PromotionResponses.ApplyPromotionResponse;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Infrastructure.Clients.Receipt.Dto.ReceiptResponses.CreateReceiptResponse;
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
public class BankPaymentStrategy implements PaymentStrategy {

    private final PromotionProvider promotionProvider;
    private final BankGatewayProvider bankGatewayProvider;
    private final ValidationService validationService;
    private final ReceiptProvider receiptProvider;

    @Override
    public CreatePaymentResponse createPayment(CreatePaymentRequest createPaymentRequest) {
        final long startMs = System.currentTimeMillis();
        final String orderId = createPaymentRequest.orderId();
        log.info("Starting BANK payment flow: orderId={}", orderId);

        Payment payment = new BankPayment();

        TimeStamps timeStamps = new TimeStamps();
        timeStamps.setCreatedAt(DateUtils.formatDate(new Date(), DateUtils.TIMESTAMP_FORMAT));

        BankValidationResult bankValidationResult = validationService.createValidation(createPaymentRequest.bankDetails());
        log.info("Bank validation completed: orderId={}, validationResult={}", orderId, bankValidationResult);

        ApplyPromotionResponse applyPromotionResponse = promotionProvider.applyPromotions(createPaymentRequest.orderId());
        log.info("Promotions service responded: orderId={}, applyPromotionResponse={}", orderId, applyPromotionResponse);
        createPaymentRequest = updatePaymentRequest(createPaymentRequest, applyPromotionResponse);

        GatewayResponse gatewayResponse = bankGatewayProvider.processPayment(createPaymentRequest);
        log.info("Bank gateway processed payment: orderId={}, gatewayResponse={}", orderId, gatewayResponse);
        timeStamps.setPaymentProcessedAt(DateUtils.formatDate(new Date(), DateUtils.TIMESTAMP_FORMAT));

        if (gatewayResponse == null || gatewayResponse.getBankReceiptNumber() == null || gatewayResponse.getBankReceiptNumber().isBlank()) {
            log.error("PayU did not return transactionId (bankReceiptNumber). Cannot create receipt.");
            throw new IllegalStateException("PayU did not return transactionId (bankReceiptNumber). Cannot create receipt. orderId=" + orderId);
        }

        if (createPaymentRequest.paymentMethod() instanceof Bank bank) {
            String bankReceiptNumber = gatewayResponse != null ? gatewayResponse.getBankReceiptNumber() : null;

            if (bankReceiptNumber == null || bankReceiptNumber.isBlank()) {
                log.error("PayU did not return a transactionId (bankReceiptNumber). Cannot create receipt for BANK payment.");
                throw new IllegalStateException(
                        "PayU did not return a transactionId (bankReceiptNumber). Cannot create receipt for BANK payment."
                );
            }

            bank.setBankReceiptNumber(bankReceiptNumber);

            if (bank.getBankName() == null || bank.getBankName().isBlank()) {
                bank.setBankName(createPaymentRequest.bankDetails().getBankName());
            }
            if (bank.getBankPaymentType() == null) {
                bank.setBankPaymentType(createPaymentRequest.bankDetails().getBankPaymentType());
            }
            if (bank.getBankAccountType() == null) {
                bank.setBankAccountType(createPaymentRequest.bankDetails().getBankAccountType());
            }
        }

        PaymentDto paymentDto = createBankPaymentDto(createPaymentRequest, applyPromotionResponse, timeStamps);
        payment = payment.createPayment(new Context(paymentDto, gatewayResponse, bankValidationResult));

        CreateReceiptResponse receiptResponse = receiptProvider.createReceipt(payment);
        log.info("Receipt created: orderId={}", orderId);
        log.info("Payment flow completed: orderId={}, elapsedMs={}", orderId, System.currentTimeMillis() - startMs);

        return ApplicationMapper.receiptResponseToPaymentResponse(receiptResponse);
    }
}

