package ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model.Enums;

public enum ValidationStatus {
    PENDING_VALIDATION,
    VALID,
    INVALID,
    EXPIRED,
    INSUFFICIENT_FUNDS,
    SUSPENDED,
    RISK_FLAG_DETECTED
}
