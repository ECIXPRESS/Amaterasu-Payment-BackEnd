package ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Application.Services;

import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Application.Ports.PaymentUseCases;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Application.Services.Strategy.BankPaymentStrategy;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Application.Services.Strategy.CashPaymentStrategy;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Application.Services.Strategy.PaymentStrategy;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Application.Services.Strategy.WalletPaymentStrategy;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model.Enums.PaymentMethodType;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Infrastructure.Web.Dto.PaymentRequests.CreatePaymentRequest;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Infrastructure.Web.Dto.PaymentResponses.CreatePaymentResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService implements PaymentUseCases {

    private final BankPaymentStrategy bankPaymentStrategy;
    private final WalletPaymentStrategy walletPaymentStrategy;
    private final CashPaymentStrategy cashPaymentStrategy;

    @Override
    public CreatePaymentResponse createPayment(CreatePaymentRequest createPaymentRequest) {
        PaymentMethodType methodType = createPaymentRequest.paymentMethod().getPaymentMethodType();

        PaymentStrategy paymentStrategy;
        switch (methodType) {
            case BANK -> paymentStrategy = bankPaymentStrategy;
            case WALLET -> paymentStrategy = walletPaymentStrategy;
            case CASH -> paymentStrategy = cashPaymentStrategy;
            default -> {
                log.error("Unsupported payment method type: {}", methodType);
                throw new IllegalArgumentException("Unsupported payment method type: " + methodType);
            }
        }

        return paymentStrategy.createPayment(createPaymentRequest);
    }
}