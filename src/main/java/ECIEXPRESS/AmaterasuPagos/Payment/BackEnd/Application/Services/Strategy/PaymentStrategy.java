package ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Application.Services.Strategy;

import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Infrastructure.Web.Dto.PaymentRequests.CreatePaymentRequest;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Infrastructure.Web.Dto.PaymentResponses.CreatePaymentResponse;

public interface PaymentStrategy {
    CreatePaymentResponse createPayment(CreatePaymentRequest createPaymentRequest);
}
