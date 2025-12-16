package ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Ports;

import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Infrastructure.Web.Dto.PaymentRequests.CreatePaymentRequest;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Infrastructure.Clients.Wallet.Dto.WalletResponses.PayWithWalletResponse;

public interface WalletProvider {
    public PayWithWalletResponse processPayment(CreatePaymentRequest createPaymentRequest);
}
