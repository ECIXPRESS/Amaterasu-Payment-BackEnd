package ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Application.Services;

import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Application.Dto.PaymentDto;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Application.Dto.PaymentRequests.CreatePaymentRequest;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Application.Dto.PaymentResponses.CreatePaymentResponse;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Application.Dto.StrategyContext;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Application.Ports.PaymentUseCases;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model.*;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model.Enums.PaymentMethodType;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model.Enums.PaymentStatus;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model.BankPayment;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model.CashPayment;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model.Payment;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model.WalletPayment;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Ports.BankGatewayProvider;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Ports.PromotionProvider;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Ports.ReceiptProvider;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Ports.WalletProvider;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Infrastructure.Clients.Promotion.Dto.PromotionResponses.PromotionResponse;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Infrastructure.Clients.Receipt.Dto.ReceiptResponses.CreateReceiptResponse;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Infrastructure.Clients.Wallet.Dto.WalletResponses.WalletResponse;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Map;

@Slf4j
@AllArgsConstructor
@RequiredArgsConstructor
@Service
public class PaymentService implements PaymentUseCases {
    private ValidationService  validationService;
    private BankGatewayProvider bankGatewayProvider;
    private PromotionProvider promotionProvider;
    private WalletProvider walletProvider;
    private ReceiptProvider receiptProvider;
    private final Map<PaymentMethodType, Payment> strategyMap =Map.of(
            PaymentMethodType.BANK, new BankPayment(),
            PaymentMethodType.WALLET, new WalletPayment(),
            PaymentMethodType.CASH, new CashPayment());

    @Override
    public CreatePaymentResponse createPayment(CreatePaymentRequest createPaymentRequest) {
        CreatePaymentResponse createPaymentResponse;
        CreateReceiptResponse receiptResponse;
        TimeStamps timeStamps = new TimeStamps();
        timeStamps.setCreatedAt(new Date().toString());
        if(createPaymentRequest.paymentMethod().getPaymentMethodType().equals(PaymentMethodType.CASH)){
            PromotionResponse promotionResponse = promotionProvider.applyPromotions(createPaymentRequest.orderId());
            createPaymentRequest = updatePaymentRequest(createPaymentRequest, promotionResponse);
            PaymentDto paymentDto = createCashPaymentDto(createPaymentRequest, promotionResponse, timeStamps);
            Payment payment = strategyMap.get(paymentDto.paymentMethod().getPaymentMethodType()).createPayment(new StrategyContext(paymentDto, null, null));
            receiptResponse = receiptProvider.createReceipt(payment);
        }
        else if (createPaymentRequest.paymentMethod().getPaymentMethodType().equals(PaymentMethodType.WALLET)) {
            PromotionResponse promotionResponse = promotionProvider.applyPromotions(createPaymentRequest.orderId());
            createPaymentRequest = updatePaymentRequest(createPaymentRequest, promotionResponse);
            WalletResponse walletResponse = walletProvider.processPayment(createPaymentRequest);
            timeStamps.setPaymentProcessedAt(new Date().toString());
            PaymentDto paymentDto = createBankPaymentDto(createPaymentRequest, promotionResponse, timeStamps);
            Payment payment = strategyMap.get(PaymentMethodType.BANK).createPayment(new StrategyContext(paymentDto, gatewayResponse, bankValidationResult));
            receiptResponse = receiptProvider.createReceipt(payment);
        }
        else{
            BankValidationResult bankValidationResult = validationService.createValidation(createPaymentRequest.bankDetails());
            PromotionResponse promotionResponse = promotionProvider.applyPromotions(createPaymentRequest.orderId());
            createPaymentRequest = updatePaymentRequest(createPaymentRequest, promotionResponse);
            GatewayResponse gatewayResponse = bankGatewayProvider.processPayment(createPaymentRequest);
            timeStamps.setPaymentProcessedAt(new Date().toString());
            PaymentDto paymentDto = createBankPaymentDto(createPaymentRequest, promotionResponse, timeStamps);
            Payment payment = strategyMap.get(PaymentMethodType.BANK).createPayment(new StrategyContext(paymentDto, gatewayResponse, bankValidationResult));
            receiptResponse = receiptProvider.createReceipt(payment);
        }
        createPaymentResponse = ReceiptResponseToPaymentResponse(receiptResponse);
        return createPaymentResponse;
    }

    private PaymentDto createCashPaymentDto(CreatePaymentRequest createPaymentRequest, PromotionResponse promotionResponse, TimeStamps timeStamps){
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

    private PaymentDto createBankPaymentDto(CreatePaymentRequest createPaymentRequest, PromotionResponse promotionResponse, TimeStamps timeStamps){
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

    private CreatePaymentRequest updatePaymentRequest(CreatePaymentRequest createPaymentRequest, PromotionResponse promotionResponse){
        return new CreatePaymentRequest(
                createPaymentRequest.orderId(),
                createPaymentRequest.clientId(),
                createPaymentRequest.storeId(),
                promotionResponse.finalAmount(),
                createPaymentRequest.paymentMethod(),
                createPaymentRequest.bankDetails());
    }

    private CreatePaymentResponse ReceiptResponseToPaymentResponse(CreateReceiptResponse receiptResponse){
        return new CreatePaymentResponse(
                receiptResponse.receiptId(),
                receiptResponse.orderId(),
                receiptResponse.storeId(),
                receiptResponse.finalAmount(),
                receiptResponse.paymentStatus());
    }
}
