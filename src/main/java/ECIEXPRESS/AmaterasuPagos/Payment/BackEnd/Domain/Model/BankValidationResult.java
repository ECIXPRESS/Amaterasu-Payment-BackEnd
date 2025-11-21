package ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model;

import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Application.Dto.PaymentRequests.CreatePaymentRequest;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BankValidationResult {
    private boolean isValid;
    private List<ValidationError> errors;
    private double riskScore;
    private Date validatedAt;

    public BankValidationResult createValidation(BankDetails bankDetails) {
    }
}
