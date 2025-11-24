package ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Infrastructure.Clients.Promotion.Dto.PromotionResponses;

import java.util.List;

public record PromotionResponse(
        double finalAmount,
        List<String> appliedPromotions) {
}
