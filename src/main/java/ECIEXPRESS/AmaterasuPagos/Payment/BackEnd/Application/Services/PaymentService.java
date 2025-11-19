package ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Application.Services;

import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Application.Ports.PaymentUseCases;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@AllArgsConstructor
@Service
public class PaymentService implements PaymentUseCases {
    private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);

}
