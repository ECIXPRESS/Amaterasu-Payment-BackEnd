package ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Application.Ports;

import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Infrastructure.Web.Dto.PaymentRequests.CreatePaymentRequest;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Infrastructure.Web.Dto.PaymentResponses.CreatePaymentResponse;

public interface PaymentUseCases {
    public CreatePaymentResponse createPayment(CreatePaymentRequest createPaymentRequest);
}
