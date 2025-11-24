package ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Application.Services.Strategy;

import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Application.Dto.PaymentRequests.CreatePaymentRequest;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Application.Dto.PaymentResponses.CreatePaymentResponse;

public interface PaymentStrategy {
    public CreatePaymentResponse createPayment(CreatePaymentRequest createPaymentRequest);
}
