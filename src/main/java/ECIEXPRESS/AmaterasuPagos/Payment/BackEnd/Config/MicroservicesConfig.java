package ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "microservices")
@Data
public class MicroservicesConfig {
    private ServiceConfig receipt;
    private ServiceConfig promotion;
    private ServiceConfig wallet;
    private ServiceConfig bankGateway;

    @Data
    public static class ServiceConfig {
        private String url;
        private String basePath;
        private int timeout;
        private RetryConfig retry;
        private AuthConfig auth;
    }

    @Data
    public static class BankGatewayConfig {
        private String url;
        private String basePath;
        private int timeout;
        private RetryConfig retry;
        private AuthConfig auth;
    }


    @Data
    public static class RetryConfig {
        private int maxAttempts;
        private long backoffDelay;
    }

    @Data
    public static class AuthConfig {
        private String apiKey;
        private String secret;
        private String token;
    }
}
