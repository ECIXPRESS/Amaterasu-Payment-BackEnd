package ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Application.Ports;

import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Application.Dto.PaymentRequests.CreatePaymentRequest;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Application.Dto.PaymentResponses.CreatePaymentResponse;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model.Strategy.BankStrategy;

public interface PaymentUseCases {
    public CreatePaymentResponse createPayment(CreatePaymentRequest createPaymentRequest);
}
