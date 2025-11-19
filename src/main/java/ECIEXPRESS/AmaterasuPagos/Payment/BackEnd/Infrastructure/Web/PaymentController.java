package ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Infrastructure.Web;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/Payment")
@Tag(name = "Payment Controller", description = "API for managing the payments done in ECIEXPRESS")
public class PaymentController {

}
