package ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Application.Services;

import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Application.Dto.PaymentRequests.CreatePaymentRequest;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Application.Dto.PaymentResponses.CreatePaymentResponse;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Application.Dto.StrategyContext;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Application.Ports.PaymentUseCases;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model.*;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model.Enums.PaymentMethodType;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model.Strategy.BankStrategy;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model.Strategy.CashStrategy;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model.Strategy.PaymentStrategy;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model.Strategy.WalletStrategy;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Ports.BankGatewayProvider;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;

@AllArgsConstructor
@RequiredArgsConstructor
@Service
public class PaymentService implements PaymentUseCases {
    private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);
    private ValidationService  validationService;
    private BankGatewayProvider bankGatewayProvider;
    private final Map<PaymentMethodType, PaymentStrategy> strategyMap =Map.of(
            PaymentMethodType.BANK, new BankStrategy(),
            PaymentMethodType.WALLET, new WalletStrategy(),
            PaymentMethodType.CASH, new CashStrategy());

    @Override
    public CreatePaymentResponse createPayment(CreatePaymentRequest createPaymentRequest) {
        CreatePaymentResponse createPaymentResponse;
        if(!createPaymentRequest.paymentMethod().getPaymentMethodType().equals(PaymentMethodType.BANK)){
            createPaymentResponse = strategyMap.get(createPaymentRequest.paymentMethod().getPaymentMethodType()).createPayment(new StrategyContext(createPaymentRequest, null, null));
        }
        else{
            BankValidationResult bankValidationResult = validationService.createValidation(createPaymentRequest.bankDetails());
            GatewayResponse gatewayResponse = bankGatewayProvider.processPayment(createPaymentRequest);
            createPaymentResponse = strategyMap.get(PaymentMethodType.BANK).createPayment(new StrategyContext(createPaymentRequest, gatewayResponse, bankValidationResult));
        }
        return createPaymentResponse;
    }
}
