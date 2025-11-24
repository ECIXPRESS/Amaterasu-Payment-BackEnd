package ECIEXPRESS.AmaterasuPagos.Payment.BackEnd;

import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Application.Services.ValidationService;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model.BankDetails;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model.BankValidationResult;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model.Enums.BankAccountType;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model.Enums.BankPaymentType;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Exception.BankValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ValidationServiceTest {

    @InjectMocks
    private ValidationService validationService;

    private BankDetails validBankDetails;
    private BankDetails invalidBankDetails;

    @BeforeEach
    void setUp() {
        validBankDetails = new BankDetails();
        validBankDetails.setBankName("Bancolombia");
        validBankDetails.setBankPaymentType(BankPaymentType.CREDIT_CARD);
        validBankDetails.setBankAccountType(BankAccountType.CHECKING_ACCOUNT);
        validBankDetails.setAccountNumber("4539578763621486"); // Tarjeta válida Luhn
        validBankDetails.setExpiryDate("12/25");
        validBankDetails.setCvv("123");
        validBankDetails.setCardHolderName("Juan Perez");

        invalidBankDetails = new BankDetails();
        invalidBankDetails.setBankName("");
        invalidBankDetails.setBankPaymentType(BankPaymentType.CREDIT_CARD);
        invalidBankDetails.setBankAccountType(BankAccountType.CHECKING_ACCOUNT); // NO null
        invalidBankDetails.setAccountNumber("1234"); // Número inválido
        invalidBankDetails.setExpiryDate("invalid");
        invalidBankDetails.setCvv("1");
        invalidBankDetails.setCardHolderName("");
    }

    @Test
    void createValidation_WithValidBankDetails_ShouldReturnValidResult() {
        // When
        BankValidationResult result = validationService.createValidation(validBankDetails);

        // Then
        assertNotNull(result);
        assertTrue(result.isValid());
        assertEquals(0.0, result.getRiskScore(), 0.01);
        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    void createValidation_WithInvalidBankDetails_ShouldThrowException() {
        // When & Then
        BankValidationException exception = assertThrows(
                BankValidationException.class,
                () -> validationService.createValidation(invalidBankDetails)
        );

        assertNotNull(exception);
        assertNotNull(exception.getValidationResult());
        assertFalse(exception.getValidationResult().isValid());
        assertTrue(exception.getValidationResult().getRiskScore() > 0);
    }

    @Test
    void createValidation_WithNullBankDetails_ShouldThrowException() {
        // When & Then
        assertThrows(NullPointerException.class,
                () -> validationService.createValidation(null));
    }
}