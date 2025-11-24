package ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model.Enums;

public enum PaymentStatus {
    PENDING,
    VALIDATING,
    PROCESSING,
    COMPLETED,
    FAILED,
    REFUNDED,
    CANCELLED,
    TIMEOUT
}
