package ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TimeStamps {
    private String createdAt;
    private String PaymentProcessedAt;
}
