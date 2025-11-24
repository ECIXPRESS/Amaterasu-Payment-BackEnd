package ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Infrastructure.Clients.Wallet.Dto.WalletRequests;

public record CreateWalletRequest(
        String clientId,
        double finalAmount) {
}
