package ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Application.Dto;

import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model.BankValidationResult;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model.GatewayResponse;

public record Context(
        PaymentDto paymentDto,
        GatewayResponse gatewayResponse,
        BankValidationResult bankValidationResult
) {
}
