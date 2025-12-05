package ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Infrastructure.Clients.Wallet.Dto.WalletRequests;

public record PayWithWalletRequest(
        String clientId,
        double finalAmount) {
}
