package ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model;

import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Model.Enums.BankResponseCode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GatewayResponse {
    private boolean isSuccess;
    private String bankReceiptNumber;
    private String authorizationNumber;
    private String gatewayMessage;
    private String ResponseCode;
    private BankResponseCode bankResponseCode;
    private double processedAmount;
    private String currency;
}
