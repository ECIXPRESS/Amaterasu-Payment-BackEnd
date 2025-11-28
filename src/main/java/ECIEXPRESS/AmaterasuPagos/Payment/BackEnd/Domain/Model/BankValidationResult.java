package ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model;

import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model.Enums.BankAccountType;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model.Enums.BankPaymentType;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model.Enums.ErrorSeverity;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Exception.BankValidationException;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
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

    public void createValidation(BankDetails bankDetails) {
        this.validatedAt = new Date();
        this.errors = new ArrayList<>();
        this.riskScore = 0.0;

        validateBankDetails(bankDetails);

        this.isValid = errors.stream()
                .noneMatch(error -> error.getErrorSeverity() == ErrorSeverity.HIGH ||
                        error.getErrorSeverity() == ErrorSeverity.CRITICAL);

        if(!this.isValid){
            throw new BankValidationException(
                    this,
                    String.format(
                            "BankPayment validation failed for account ending in %s. Risk score: %.2f, Errors: %d",
                            bankDetails.getAccountNumber() != null && bankDetails.getAccountNumber().length() > 4 ?
                                    "***" + bankDetails.getAccountNumber().substring(bankDetails.getAccountNumber().length() - 4) : "N/A",
                            this.riskScore,
                            this.errors.size()
                    )
            );
        }
    }

    private void validateBankDetails(BankDetails bankDetails) {
        if (bankDetails.getBankName() == null || bankDetails.getBankName().trim().isEmpty()) {
            addError("BankPayment name is required", "Please provide a valid bank name", ErrorSeverity.HIGH);
            riskScore += 20;
        }

        validateAccountNumber(bankDetails.getAccountNumber(), bankDetails.getBankAccountType());

        if (bankDetails.getBankPaymentType() == BankPaymentType.CREDIT_CARD ||
                bankDetails.getBankPaymentType() == BankPaymentType.DEBIT_CARD) {
            validateExpiryDate(bankDetails.getExpiryDate());
            validateCVV(bankDetails.getCvv());
        }

        validateCardHolderName(bankDetails.getCardHolderName());

        validatePaymentTypeSpecificRules(bankDetails);
    }

    private void validateAccountNumber(String accountNumber, BankAccountType accountType) {
        if (accountNumber == null || accountNumber.trim().isEmpty()) {
            addError("Account number is required", "Please provide a valid account number", ErrorSeverity.HIGH);
            riskScore += 25;
            return;
        }

        switch (accountType) {
            case SAVINGS_ACCOUNT:
            case CHECKING_ACCOUNT:
                if (!accountNumber.matches("\\d{10,20}")) {
                    addError("Invalid account number format",
                            "Account number should contain 10-20 digits", ErrorSeverity.HIGH);
                    riskScore += 15;
                }
                break;
        }

        if (accountNumber.length() >= 13 && accountNumber.length() <= 19) {
            if (!isValidLuhn(accountNumber)) {
                addError("Invalid account number",
                        "Account number failed validation check", ErrorSeverity.HIGH);
                riskScore += 30;
            }
        }
    }

    private void validateExpiryDate(String expiryDate) {
        if (expiryDate == null || expiryDate.trim().isEmpty()) {
            addError("Expiry date is required for card payments",
                    "Please provide card expiry date", ErrorSeverity.HIGH);
            riskScore += 20;
            return;
        }

        try {
            String[] parts = expiryDate.split("/");
            if (parts.length != 2) {
                throw new IllegalArgumentException("Invalid format");
            }

            int month = Integer.parseInt(parts[0]);
            int year = Integer.parseInt(parts[1]);

            if (month < 1 || month > 12) {
                addError("Invalid expiry month",
                        "Month must be between 01 and 12", ErrorSeverity.MEDIUM);
                riskScore += 10;
            }

            Date currentDate = new Date();
            int currentYear = currentDate.getYear() % 100;
            int currentMonth = currentDate.getMonth() + 1;

            if (year < currentYear || (year == currentYear && month < currentMonth)) {
                addError("Card has expired",
                        "Please use a card with valid expiry date", ErrorSeverity.HIGH);
                riskScore += 25;
            }

        } catch (Exception e) {
            addError("Invalid expiry date format",
                    "Please use MM/YY format", ErrorSeverity.MEDIUM);
            riskScore += 10;
        }
    }

    private void validateCVV(String cvv) {
        if (cvv == null || cvv.trim().isEmpty()) {
            addError("CVV is required for card payments",
                    "Please provide card security code", ErrorSeverity.HIGH);
            riskScore += 15;
            return;
        }

        if (!cvv.matches("\\d{3,4}")) {
            addError("Invalid CVV format",
                    "CVV should be 3 or 4 digits", ErrorSeverity.MEDIUM);
            riskScore += 10;
        }
    }

    private void validateCardHolderName(String cardHolderName) {
        if (cardHolderName == null || cardHolderName.trim().isEmpty()) {
            addError("Card holder name is required",
                    "Please provide card holder name", ErrorSeverity.HIGH);
            riskScore += 15;
            return;
        }

        if (cardHolderName.trim().length() < 2) {
            addError("Card holder name is too short",
                    "Please provide full card holder name", ErrorSeverity.MEDIUM);
            riskScore += 5;
        }

        if (!cardHolderName.matches("^[a-zA-Z\\s]+$")) {
            addWarning("Card holder name contains special characters",
                    "Card holder name should only contain letters", ErrorSeverity.LOW);
            riskScore += 2;
        }
    }

    private void validatePaymentTypeSpecificRules(BankDetails bankDetails) {
        switch (bankDetails.getBankPaymentType()) {
            case CREDIT_CARD:
                break;

            case DEBIT_CARD:
                addWarning("Debit card transaction",
                        "Daily limits may apply", ErrorSeverity.LOW);
                break;

            case PSE:
                if (bankDetails.getBankName() == null) {
                    addError("BankPayment name is required for PSE payments",
                            "Please select a bank", ErrorSeverity.HIGH);
                    riskScore += 20;
                }
                break;

            case APP:
                addWarning("Mobile app payment",
                        "Ensure app is installed and updated", ErrorSeverity.LOW);
                break;
        }
    }

    private boolean isValidLuhn(String number) {
        if (number == null || number.trim().isEmpty()) {
            return false;
        }

        try {
            String cleanNumber = number.replaceAll("\\s+", "");
            if (!cleanNumber.matches("\\d+")) {
                return false;
            }

            if (cleanNumber.matches("^0+$")) {
                return false;
            }

            int sum = 0;
            boolean alternate = false;

            for (int i = cleanNumber.length() - 1; i >= 0; i--) {
                int digit = Character.getNumericValue(cleanNumber.charAt(i));

                if (alternate) {
                    digit *= 2;
                    if (digit > 9) {
                        digit = digit - 9;
                    }
                }

                sum += digit;
                alternate = !alternate;
            }

            return (sum % 10 == 0);
        } catch (Exception e) {
            return false;
        }
    }

    private void addError(String error, String warning, ErrorSeverity severity) {
        ValidationError validationError = new ValidationError(error, warning, severity, new Date());
        this.errors.add(validationError);
    }

    private void addWarning(String warning, String suggestion, ErrorSeverity severity) {
        ValidationError validationError = new ValidationError(null, warning, severity, new Date());
        this.errors.add(validationError);
    }
}
