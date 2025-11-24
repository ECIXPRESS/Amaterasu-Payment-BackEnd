package ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Application.Ports;

import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Application.Dto.PaymentRequests.CreatePaymentRequest;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Application.Dto.PaymentResponses.CreatePaymentResponse;

public interface PaymentUseCases {
    public CreatePaymentResponse createPayment(CreatePaymentRequest createPaymentRequest);
}
