package ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Exception;

import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model.BankValidationResult;
import lombok.Getter;

@Getter
public class BankValidationException extends RuntimeException {
    private final BankValidationResult validationResult;
    private final String errorCode;

    public BankValidationException(BankValidationResult validationResult) {
        super("Bank validation failed with risk score: " + validationResult.getRiskScore());
        this.validationResult = validationResult;
        this.errorCode = "BANK_VALIDATION_FAILED";
    }

    public BankValidationException(BankValidationResult validationResult, String message) {
        super(message);
        this.validationResult = validationResult;
        this.errorCode = "BANK_VALIDATION_FAILED";
    }

    public BankValidationException(BankValidationResult validationResult, String message, String errorCode) {
        super(message);
        this.validationResult = validationResult;
        this.errorCode = errorCode;
    }
}