package ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Domain.Ports;

import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Infrastructure.Clients.Promotion.Dto.PromotionResponses.ApplyPromotionResponse;

public interface PromotionProvider {
    public ApplyPromotionResponse applyPromotions(String orderId);
}
