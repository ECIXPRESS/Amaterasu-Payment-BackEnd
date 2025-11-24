package ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Application.Services;

import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Application.Dto.PaymentRequests.CreatePaymentRequest;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Application.Dto.PaymentResponses.CreatePaymentResponse;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Application.Ports.PaymentUseCases;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Application.Services.Strategy.BankPaymentStrategy;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Application.Services.Strategy.CashPaymentStrategy;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Application.Services.Strategy.PaymentStrategy;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Application.Services.Strategy.WalletPaymentStrategy;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model.*;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model.Enums.PaymentMethodType;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Ports.BankGatewayProvider;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Ports.PromotionProvider;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Ports.ReceiptProvider;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Ports.WalletProvider;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Map;

import static ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Application.Mappers.ApplicationMapper.*;

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
    private final Map<PaymentMethodType, PaymentStrategy> strategyMap =Map.of(
            PaymentMethodType.BANK, new BankPaymentStrategy(),
            PaymentMethodType.WALLET, new WalletPaymentStrategy(),
            PaymentMethodType.CASH, new CashPaymentStrategy());

    @Override
    public CreatePaymentResponse createPayment(CreatePaymentRequest createPaymentRequest) {
        PaymentStrategy paymentStrategy = strategyMap.get(createPaymentRequest.paymentMethod().getPaymentMethodType());
        return paymentStrategy.createPayment(createPaymentRequest);
    }


}
