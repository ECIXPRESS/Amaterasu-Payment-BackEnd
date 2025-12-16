package ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Application.Services;

import ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Infrastructure.Web.Dto.UserDetailsDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserServiceClient {

    private final RestTemplate restTemplate;

    @Value("${user.service.url:http://localhost:8081}")
    private String userServiceBaseUrl;

    public String getUserEmailById(String userId) {
        try {
            String url = UriComponentsBuilder.fromHttpUrl(userServiceBaseUrl)
                    .path("/api/users/{userId}/email")
                    .buildAndExpand(userId)
                    .toUriString();

            log.info("Obteniendo email del usuario {} desde: {}", userId, url);

            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            return response.getBody();

        } catch (HttpClientErrorException.NotFound e) {
            log.warn("Usuario no encontrado con ID: {}", userId);
            throw new RuntimeException("Usuario no encontrado: " + userId);
        } catch (ResourceAccessException e) {
            log.error("No se puede conectar al servicio de usuarios: {} - URL: {}",
                    e.getMessage(), userServiceBaseUrl);
            throw new RuntimeException("Servicio de usuarios no disponible");
        } catch (Exception e) {
            log.error("Error obteniendo email del usuario {}: {}", userId, e.getMessage());
            throw new RuntimeException("Error obteniendo información del usuario");
        }
    }

    public Optional<    UserDetailsDto> getUserDetails(String userId) {
        try {
            String url = UriComponentsBuilder.fromHttpUrl(userServiceBaseUrl)
                    .path("/api/users/{userId}")
                    .buildAndExpand(userId)
                    .toUriString();

            log.info("Obteniendo detalles del usuario {} desde: {}", userId, url);

            ResponseEntity<UserDetailsDto> response = restTemplate.getForEntity(url, UserDetailsDto.class);
            return Optional.ofNullable(response.getBody());

        } catch (HttpClientErrorException.NotFound e) {
            log.warn("Usuario no encontrado con ID: {}", userId);
            return Optional.empty();
        } catch (Exception e) {
            log.error("Error obteniendo detalles del usuario {}: {}", userId, e.getMessage());
            return Optional.empty();
        }
    }
}
