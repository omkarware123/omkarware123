package com.omkar.Zoho_CRM.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auth.oauth2.GoogleCredentials;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Map;

@Service
public class FCMService {

    private static final String FCM_API_URL = "https://fcm.googleapis.com/v1/projects/seismic-box-432211-p1/messages:send";
    private static final String MESSAGING_SCOPE = "https://www.googleapis.com/auth/firebase.messaging";
    private static final String FCM_CREDENTIALS_PATH = "/fireCreds.json";

    private final ObjectMapper objectMapper = new ObjectMapper(); // Jackson object mapper

    private String getAccessToken() throws IOException {
        InputStream in = FCMService.class.getResourceAsStream(FCM_CREDENTIALS_PATH);
        GoogleCredentials googleCredentials = GoogleCredentials
                .fromStream(in)
                .createScoped(Collections.singletonList(MESSAGING_SCOPE));
        googleCredentials.refreshIfExpired();
        return googleCredentials.getAccessToken().getTokenValue();
    }

    public void sendFcmNotification(String token, String title, Map<String, String> body) {
        try {
            String accessToken = getAccessToken();
            RestTemplate restTemplate = new RestTemplate();

            // Serialize the body map into a JSON string
            String bodyJson = objectMapper.writeValueAsString(body);

            String messageJson = String.format(
                    "{\"message\": {\"token\": \"%s\", \"notification\": {\"title\": \"%s\", \"body\": \"%s\"}, \"data\": %s}}",
                    token, title, body.toString(), bodyJson);  // bodyJson contains the JSON structure

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> request = new HttpEntity<>(messageJson, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(FCM_API_URL, request, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                System.out.println(response.getStatusCode() + ":FCM Notification Sent Successfully\nResponse = " + response.getBody() + "\n");
            }

        } catch (IOException e) {
            System.out.println("\nException while sending FCM notification:" + e.getMessage());
        }
    }
}

