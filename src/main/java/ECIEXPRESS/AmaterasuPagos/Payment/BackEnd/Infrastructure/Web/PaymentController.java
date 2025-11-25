package ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Infrastructure.Web;

import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Infrastructure.Web.Dto.PaymentRequests.CreatePaymentRequest;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Infrastructure.Web.Dto.PaymentResponses.CreatePaymentResponse;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Application.Services.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payments")
@Tag(name = "Payment Controller", description = "API for managing the payments done in ECIEXPRESS")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/ProcessPayment")
    @Operation(summary = "Create a new payment", 
               description = "Processes a new payment based on the provided payment method")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", 
                    description = "Payment created successfully",
                    content = @Content(schema = @Schema(implementation = CreatePaymentResponse.class))),
        @ApiResponse(responseCode = "400", 
                    description = "Invalid input data",
                    content = @Content),
        @ApiResponse(responseCode = "500", 
                    description = "Internal server error",
                    content = @Content)
    })
    public ResponseEntity<CreatePaymentResponse> createPayment(
            @Valid @RequestBody CreatePaymentRequest createPaymentRequest) {
        CreatePaymentResponse response = paymentService.createPayment(createPaymentRequest);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
}
