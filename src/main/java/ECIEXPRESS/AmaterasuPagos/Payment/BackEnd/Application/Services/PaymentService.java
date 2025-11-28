package ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Application.Services;

import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Infrastructure.Web.Dto.PaymentRequests.CreatePaymentRequest;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Infrastructure.Web.Dto.PaymentResponses.CreatePaymentResponse;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Application.Ports.PaymentUseCases;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Application.Services.Strategy.BankPaymentStrategy;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Application.Services.Strategy.CashPaymentStrategy;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Application.Services.Strategy.PaymentStrategy;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Application.Services.Strategy.WalletPaymentStrategy;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model.Enums.PaymentMethodType;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@NoArgsConstructor
@Service
public class PaymentService implements PaymentUseCases {
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
