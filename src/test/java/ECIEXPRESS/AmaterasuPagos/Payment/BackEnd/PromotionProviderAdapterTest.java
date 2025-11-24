package ECIEXPRESS.AmaterasuPagos.Payment.BackEnd;

import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Infrastructure.Clients.Promotion.Dto.PromotionResponses.PromotionResponse;
import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Infrastructure.Clients.Promotion.PromotionProviderAdapter;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class PromotionProviderAdapterTest {

    private MockWebServer mockWebServer;
    private PromotionProviderAdapter promotionProviderAdapter;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        promotionProviderAdapter = new PromotionProviderAdapter(new RestTemplate());

        // Usar reflexión para setear la URL base
        setField(promotionProviderAdapter, "baseUrl", mockWebServer.url("/").toString());
        setField(promotionProviderAdapter, "basePath", "/api/v1/promotions");
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    void applyPromotions_WithSuccessfulResponse_ShouldReturnPromotionResponse() throws Exception {
        // Given
        String orderId = "ORDER-123";
        String jsonResponse = """
            {
                "finalAmount": 90000.0,
                "appliedPromotions": ["PROMO-10%", "WELCOME-5"]
            }
            """;

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(jsonResponse));

        // When
        PromotionResponse response = promotionProviderAdapter.applyPromotions(orderId);

        // Then
        assertNotNull(response);
        assertEquals(90000.0, response.finalAmount());
        assertEquals(2, response.appliedPromotions().size());
        assertTrue(response.appliedPromotions().contains("PROMO-10%"));
    }

    @Test
    void applyPromotions_WithServerError_ShouldReturnFallbackResponse() throws Exception {
        // Given
        String orderId = "ORDER-123";

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(500)
                .setBody("Server Error"));

        // When
        PromotionResponse response = promotionProviderAdapter.applyPromotions(orderId);

        // Then
        assertNotNull(response);
        assertEquals(0.0, response.finalAmount()); // Cambiado de null a 0.0
        assertNull(response.appliedPromotions());
    }

    // Método helper para setear campos privados
    private void setField(Object target, String fieldName, Object value) {
        try {
            var field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}