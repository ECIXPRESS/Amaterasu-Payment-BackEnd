package ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Application.Dto;

import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Application.Dto.PaymentRequests.CreatePaymentRequest;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model.BankValidationResult;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model.GatewayResponse;

public record StrategyContext(
        CreatePaymentRequest createPaymentRequest,
        GatewayResponse gatewayResponse,
        BankValidationResult bankValidationResult
) {
}
