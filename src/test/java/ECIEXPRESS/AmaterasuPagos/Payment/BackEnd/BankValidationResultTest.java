package ECIEXPRESS.AmaterasuPagos.Payment.BackEnd;

import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model.BankDetails;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model.BankValidationResult;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model.Enums.BankAccountType;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model.Enums.BankPaymentType;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Exception.BankValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BankValidationResultTest {

    private BankDetails validBankDetails;
    private BankValidationResult bankValidationResult;

    @BeforeEach
    void setUp() {
        bankValidationResult = new BankValidationResult();

        // Usar fecha futura para evitar expiración
        int nextYear = java.time.Year.now().getValue() + 1;
        String expiryDate = "12/" + (nextYear % 100);

        validBankDetails = new BankDetails();
        validBankDetails.setBankName("Bancolombia");
        validBankDetails.setBankPaymentType(BankPaymentType.CREDIT_CARD);
        validBankDetails.setBankAccountType(BankAccountType.CHECKING_ACCOUNT);
        validBankDetails.setAccountNumber("4539578763621486"); // Visa válida Luhn
        validBankDetails.setExpiryDate(expiryDate);
        validBankDetails.setCvv("123");
        validBankDetails.setCardHolderName("Juan Perez");
    }

    @Test
    void createValidation_WithValidDetails_ShouldPass() {
        // When
        bankValidationResult.createValidation(validBankDetails);

        // Then
        assertTrue(bankValidationResult.isValid());
        assertEquals(0.0, bankValidationResult.getRiskScore(), 0.01);
        assertTrue(bankValidationResult.getErrors().isEmpty());
    }

    @Test
    void isValidLuhn_WithValidNumbers_ShouldReturnTrue() {
        // Given - Números de tarjeta válidos (test numbers)
        String[] validNumbers = {
                "4539578763621486", // Visa válida
                "5506922400634930", // Mastercard válida
                "371449635398431"   // American Express válida
        };

        // When & Then
        for (String number : validNumbers) {
            assertTrue(invokePrivateIsValidLuhn(number), "Should be valid: " + number);
        }
    }

    @Test
    void isValidLuhn_WithInvalidNumbers_ShouldReturnFalse() {
        // Given - Números inválidos
        String[] invalidNumbers = {
                "4539578763621487", // Visa inválida (último dígito cambiado)
                "1234567890123456", // Número secuencial inválido
                "0000000000000000"// Número de prueba pero inválido para Luhn
        };

        // When & Then
        for (String number : invalidNumbers) {
            assertFalse(invokePrivateIsValidLuhn(number), "Should be invalid: " + number);
        }
    }

    @Test
    void createValidation_WithInvalidCardNumber_ShouldFail() {
        // Given
        validBankDetails.setAccountNumber("4539578763621487"); // Número inválido Luhn

        // When & Then
        BankValidationException exception = assertThrows(
                BankValidationException.class,
                () -> bankValidationResult.createValidation(validBankDetails)
        );

        assertFalse(bankValidationResult.isValid());
        assertTrue(bankValidationResult.getRiskScore() > 0);
        assertFalse(bankValidationResult.getErrors().isEmpty());
    }

    @Test
    void createValidation_WithExpiredCard_ShouldFail() {
        // Given
        validBankDetails.setExpiryDate("01/20"); // Past date

        // When & Then
        BankValidationException exception = assertThrows(
                BankValidationException.class,
                () -> bankValidationResult.createValidation(validBankDetails)
        );

        assertFalse(bankValidationResult.isValid());
        assertTrue(bankValidationResult.getRiskScore() > 0);
        assertTrue(bankValidationResult.getErrors().stream()
                .anyMatch(error -> error.getErrors() != null && error.getErrors().contains("Card has expired")));
    }

    @Test
    void createValidation_WithInvalidCVV_ShouldAddWarning() {
        // Given
        validBankDetails.setCvv("12"); // Invalid CVV length

        // When
        bankValidationResult.createValidation(validBankDetails);

        // Then
        assertTrue(bankValidationResult.isValid()); // Should still be valid since it's only a MEDIUM severity issue
        assertTrue(bankValidationResult.getRiskScore() > 0);
        assertTrue(bankValidationResult.getErrors().stream()
                .anyMatch(error -> error.getErrors() != null &&
                        error.getErrors().contains("Invalid CVV format")));
    }

    @Test
    void createValidation_WithEmptyCardHolderName_ShouldFail() {
        // Given
        validBankDetails.setCardHolderName(" "); // Empty name with spaces

        // When & Then
        BankValidationException exception = assertThrows(
                BankValidationException.class,
                () -> bankValidationResult.createValidation(validBankDetails)
        );

        assertFalse(bankValidationResult.isValid());
        assertTrue(bankValidationResult.getRiskScore() > 0);
        assertTrue(bankValidationResult.getErrors().stream()
                .anyMatch(error -> error.getErrors() != null && error.getErrors().contains("Card holder name is required")));
    }

    @Test
    void createValidation_WithInvalidAccountType_ShouldFail() {
        // Given
        validBankDetails.setBankAccountType(null); // Null account type

        // When & Then
        assertThrows(
                NullPointerException.class,
                () -> bankValidationResult.createValidation(validBankDetails)
        );
    }

    @Test
    void createValidation_WithPSEType_ShouldValidateBankName() {
        // Given
        validBankDetails.setBankPaymentType(BankPaymentType.PSE);
        validBankDetails.setBankName(""); // Empty bank name

        // When & Then
        BankValidationException exception = assertThrows(
                BankValidationException.class,
                () -> bankValidationResult.createValidation(validBankDetails)
        );

        assertFalse(bankValidationResult.isValid());
        assertTrue(bankValidationResult.getErrors().stream()
                .anyMatch(error -> error.getErrors() != null && error.getErrors().contains("BankPayment name is required")));
    }

    @Test
    void createValidation_WithDebitCard_ShouldAddWarning() {
        // Given
        validBankDetails.setBankPaymentType(BankPaymentType.DEBIT_CARD);

        // When
        bankValidationResult.createValidation(validBankDetails);

        // Then
        assertTrue(bankValidationResult.isValid());
        assertTrue(bankValidationResult.getErrors().stream()
                .anyMatch(error -> error.getWarnings() != null && error.getWarnings().contains("Debit card transaction")));
    }

    @Test
    void createValidation_WithInvalidExpiryDate_ShouldAddWarning() {
        // Given
        validBankDetails.setExpiryDate("13/25"); // Invalid month

        // When
        bankValidationResult.createValidation(validBankDetails);

        // Then
        assertTrue(bankValidationResult.isValid()); // Should still be valid since it's only a MEDIUM severity issue
        assertTrue(bankValidationResult.getErrors().stream()
                .anyMatch(error -> error.getErrors() != null &&
                        error.getErrors().contains("Invalid expiry month")));
    }

    @Test
    void createValidation_WithEmptyBankName_ShouldFail() {
        // Given
        validBankDetails.setBankName(" "); // Empty bank name with spaces

        // When & Then
        BankValidationException exception = assertThrows(
                BankValidationException.class,
                () -> bankValidationResult.createValidation(validBankDetails)
        );

        assertFalse(bankValidationResult.isValid());
        assertTrue(bankValidationResult.getErrors().stream()
                .anyMatch(error -> error.getErrors() != null && 
                        error.getErrors().contains("BankPayment name is required")));
    }

    // Método helper para acceder al método privado isValidLuhn
    private boolean invokePrivateIsValidLuhn(String number) {
        try {
            var method = BankValidationResult.class.getDeclaredMethod("isValidLuhn", String.class);
            method.setAccessible(true);
            return (boolean) method.invoke(bankValidationResult, number);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}