package ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Ports;

import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Infrastructure.Web.Dto.PaymentRequests.CreatePaymentRequest;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model.GatewayResponse;

public interface BankGatewayProvider {
    public GatewayResponse processPayment(CreatePaymentRequest createPaymentRequest);
}
