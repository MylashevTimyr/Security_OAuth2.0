package org.example.security_oauth2.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenRevocationService {

    @Value("${spring.security.oauth2.client.registration.github.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.github.client-secret}")
    private String clientSecret;

    public boolean revokeToken(String token) {
        String revokeUrl = "https://api.github.com/applications/" + clientId + "/token";

        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBasicAuth(clientId, clientSecret);

        String requestBody = "{\"access_token\":\"" + token + "\"}";

        HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Void> response = restTemplate.exchange(
                    revokeUrl,
                    HttpMethod.DELETE,
                    entity,
                    Void.class
            );

            if (response.getStatusCode() == HttpStatus.NO_CONTENT) {
                log.info("Токен успешно отозван.");
                return true;
            } else {
                log.warn("Не удалось отозвать токен. Статус: {}", response.getStatusCode());
                return false;
            }
        } catch (Exception e) {
            log.error("Ошибка при отзыве токена: ", e);
            return false;
        }
    }
}
