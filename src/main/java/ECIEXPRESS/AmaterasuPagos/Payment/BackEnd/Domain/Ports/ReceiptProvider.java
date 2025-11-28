package ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Ports;

import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model.Payment;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Infrastructure.Clients.Receipt.Dto.ReceiptResponses.CreateReceiptResponse;

public interface ReceiptProvider {
    public CreateReceiptResponse createReceipt(Payment payment);
}
