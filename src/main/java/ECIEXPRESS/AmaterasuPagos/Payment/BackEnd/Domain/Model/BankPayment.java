package ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model;

import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Application.Dto.StrategyContext;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = false)
@AllArgsConstructor
@NoArgsConstructor
public class BankPayment extends Payment {
    private BankValidationResult bankValidationResult;
    private GatewayResponse gatewayResponse;

    @Override
    public Payment createPayment(StrategyContext strategyContext){
        return null;
    }
}
