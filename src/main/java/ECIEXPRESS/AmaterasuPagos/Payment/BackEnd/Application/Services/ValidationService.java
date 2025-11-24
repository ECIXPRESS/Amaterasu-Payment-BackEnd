package ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Application.Services;

import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Application.Dto.PaymentRequests.CreatePaymentRequest;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Application.Ports.ValidationUseCases;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model.BankDetails;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model.BankValidationResult;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Exception.BankValidationException;
import com.sun.jdi.event.ExceptionEvent;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ValidationService implements ValidationUseCases{

    @Override
    public BankValidationResult createValidation(BankDetails bankDetails) {
        try {
            BankValidationResult bankValidationResult = new BankValidationResult();
            bankValidationResult.createValidation(bankDetails);

            log.info("BankPayment validation completed successfully. Valid: {}, Risk Score: {}",
                    bankValidationResult.isValid(),
                    bankValidationResult.getRiskScore());

            return bankValidationResult;

        } catch (BankValidationException e) {
            log.warn("BankPayment validation failed: {}", e.getMessage());
            throw e;
        }
    }

}
