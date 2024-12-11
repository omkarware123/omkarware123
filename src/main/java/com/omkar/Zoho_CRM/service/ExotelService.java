package com.omkar.Zoho_CRM.service;

import com.omkar.Zoho_CRM.config.ExotelProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;

@Service
public class ExotelService {

    @Autowired
    private final RestTemplate restTemplate;

    @Autowired
    private final ExotelProperties properties;

    @Autowired
    public ExotelService(RestTemplate restTemplate, ExotelProperties properties) {
        this.restTemplate = restTemplate;
        this.properties = properties;
    }

    public ResponseEntity<?> connectCall(String from, String to) throws IOException {
        String apiKey = properties.getApiKey();
        String apiToken = properties.getApiToken();
        String subdomain = properties.getSubDomain();
        String sid = properties.getSid();
        String url = "https://" + apiKey + ":" + apiToken + "@" + subdomain + "/v1/Accounts/" + sid + "/Calls/connect";

        MultiValueMap<String, String> request = getStringStringMultiValueMap(from, to);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBasicAuth(apiKey, apiToken);
            HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(request, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            if (response.getStatusCode() == HttpStatus.OK) {
                return ResponseEntity.ok().body(response.getBody());
            } else {
                return ResponseEntity.badRequest().body("Invalid CallSid");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred: " + e.getMessage());
        }
    }

    private static MultiValueMap<String, String> getStringStringMultiValueMap(String from, String to) {
        MultiValueMap<String, String> request = new LinkedMultiValueMap<>();
        request.add("From", from);
        request.add("To", to);
        request.add("CallerId", "09513886363");
        request.add("TimeLimit", "120");
        request.add("Record", "true");
        request.add("StatusCallback", "https://9c5e-117-248-248-230.ngrok-free.app/statusCallback");
        request.add("StatusCallbackEvents[]", "terminal");
        return request;
    }
}

