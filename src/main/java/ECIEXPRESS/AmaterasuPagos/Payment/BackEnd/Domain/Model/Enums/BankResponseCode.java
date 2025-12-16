package ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model.Enums;

public enum BankResponseCode {
    APPROVED,
    DECLINED,
    INSUFFICIENT_FUNDS,
    EXPIRED_CARD,
    INVALID_CVV,
    SUSPECTED_FRAUD,
    BANK_UNAVAILABLE,
    TIMEOUT,
    PENDING,
    ERROR,
    BANK_ERROR,
    INVALID_CARD,
    UNKNOWN_ERROR,
    INVALID_REQUEST
}
