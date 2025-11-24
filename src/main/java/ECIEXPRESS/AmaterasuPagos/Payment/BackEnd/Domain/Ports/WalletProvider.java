package ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Ports;

import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Application.Dto.PaymentRequests.CreatePaymentRequest;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Infrastructure.Clients.Wallet.Dto.WalletResponses.CreateWalletResponse;

public interface WalletProvider {
    public CreateWalletResponse processPayment(CreatePaymentRequest createPaymentRequest);
}
