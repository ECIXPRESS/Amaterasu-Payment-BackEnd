package ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Application.Services;

import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Application.Dto.PaymentRequests.CreatePaymentRequest;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Application.Ports.ValidationUseCases;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model.BankDetails;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model.BankValidationResult;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Exception.BankValidationException;
import com.sun.jdi.event.ExceptionEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ValidationService implements ValidationUseCases{
    private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);

    @Override
    public BankValidationResult createValidation(BankDetails bankDetails) {
        try {
            BankValidationResult bankValidationResult = new BankValidationResult();
            bankValidationResult.createValidation(bankDetails);

            logger.info("Bank validation completed successfully. Valid: {}, Risk Score: {}",
                    bankValidationResult.isValid(),
                    bankValidationResult.getRiskScore());

            return bankValidationResult;

        } catch (BankValidationException e) {
            logger.warn("Bank validation failed: {}", e.getMessage());
            throw e;
        }
    }

}
