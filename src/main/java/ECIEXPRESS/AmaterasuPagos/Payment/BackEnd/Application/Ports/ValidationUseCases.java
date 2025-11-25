package ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Application.Ports;

import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Application.Dto.PaymentRequests.CreatePaymentRequest;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model.BankDetails;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model.BankValidationResult;

public interface ValidationUseCases {
    public BankValidationResult createValidation(BankDetails bankDetails);
}
