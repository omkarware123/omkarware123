package com.omkar.Zoho_CRM.service;

import com.omkar.Zoho_CRM.config.ZohoProperties;
import com.zoho.api.authenticator.OAuthToken;
import com.zoho.api.authenticator.Token;
import com.zoho.api.authenticator.store.FileStore;
import com.zoho.api.authenticator.store.TokenStore;
import com.zoho.api.logger.Logger;
import com.zoho.api.logger.Logger.Levels;
import com.zoho.crm.api.Initializer;
import com.zoho.crm.api.SDKConfig;
import com.zoho.crm.api.dc.DataCenter.Environment;
import com.zoho.crm.api.dc.INDataCenter;
import com.zoho.crm.api.exception.SDKException;
import jakarta.annotation.PostConstruct;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.http.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class Initialize {

    private final RestTemplate restTemplate;
    private final ZohoProperties zohoProperties;

    private final String clientId;
    private final String clientSecret;
    private final String redirectUri;
    private String refreshToken;
    private String accessToken;

    @Autowired
    public Initialize(RestTemplate restTemplate, ZohoProperties zohoProperties) {
        this.restTemplate = restTemplate;
        this.zohoProperties = zohoProperties;
        this.clientId = zohoProperties.getClientId();
        this.clientSecret = zohoProperties.getClientSecret();
        this.redirectUri = zohoProperties.getRedirectUri();
        this.refreshToken = zohoProperties.getRefreshToken();
        this.accessToken = zohoProperties.getAccessToken();
    }

    @PostConstruct
    public void setupZohoSDK() {
        try {
            initialize();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void initialize() throws Exception {
        Logger logger = new Logger.Builder()
                .level(Levels.INFO)
                .filePath("java_sdk_log.log")
                .build();

        Environment environment = INDataCenter.PRODUCTION;

        Token token = new OAuthToken.Builder()
                .clientID(clientId)
                .clientSecret(clientSecret)
                .refreshToken(refreshToken)
                .accessToken(accessToken)
                .redirectURL(redirectUri)
                .build();

        TokenStore tokenStore = new FileStore("zoho_oauth_tokens.txt");

        SDKConfig sdkConfig = new SDKConfig.Builder()
                .autoRefreshFields(true)
                .pickListValidation(true)
                .build();

        new Initializer.Builder()
                .environment(environment)
                .token(token)
                .store(tokenStore)
                .SDKConfig(sdkConfig)
                .logger(logger)
                .initialize();
    }

    public long refreshAccessToken() {
        String tokenUrl = "https://accounts.zoho.in/oauth/v2/token";

        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String formattedTime = now.format(formatter);
        System.out.println("\nrefreshAccessToken is called at: " + formattedTime);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> bodyParams = new LinkedMultiValueMap<>();
        bodyParams.add("client_id", clientId);
        bodyParams.add("client_secret", clientSecret);
        bodyParams.add("grant_type", "refresh_token");
        bodyParams.add("refresh_token", refreshToken);

        try {
            HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(bodyParams, headers);
            ResponseEntity<String> response = restTemplate.exchange(tokenUrl, HttpMethod.POST, requestEntity, String.class);

            JSONObject responseObject = new JSONObject(response.getBody());

            if (responseObject.has("access_token")) {
                String newAccessToken = responseObject.getString("access_token");
                System.out.println("New access token: " + newAccessToken);

                if (responseObject.has("refresh_token")) {
                    refreshToken = responseObject.getString("refresh_token");
                    System.out.println("New refresh token: " + refreshToken);
                }

                TokenStore tokenStore = new FileStore("./zoho_oauth_tokens.txt");
                Token token = new OAuthToken.Builder()
                        .clientID(clientId)
                        .clientSecret(clientSecret)
                        .refreshToken(refreshToken)
                        .accessToken(newAccessToken)
                        .build();
                tokenStore.saveToken(token);
                Initializer.getInitializer().getStore().saveToken(token);

                long expiresIn = responseObject.has("expires_in") ? responseObject.getLong("expires_in") : 3600;
                System.out.println("Token expires in: " + expiresIn + " seconds");

                accessToken = newAccessToken;

                return expiresIn;
            }
        } catch (Exception e) {
            System.out.println("Exception is: " + e);
            e.printStackTrace();
        }

        return 3600;
    }

    public String getAccessToken() {
        try {
            if (Initializer.getInitializer() == null) {
                initialize();
            }
            List<Token> tokens = Initializer.getInitializer().getStore().getTokens();

            for (Token token : tokens) {
                if (token instanceof OAuthToken oauthToken) {
                    return oauthToken.getAccessToken();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public Map<String, String> retrieveTokens() throws SDKException {
        Map<String, String> tokenMap = new HashMap<>();

        List<Token> tokens = Initializer.getInitializer().getStore().getTokens();

        for (Token token : tokens) {
            if (token instanceof OAuthToken oauthToken) {
                tokenMap.put("accessToken", oauthToken.getAccessToken());
                tokenMap.put("refreshToken", oauthToken.getRefreshToken());
            }
        }

        return tokenMap;
    }

    public ResponseEntity<String> makeZohoAPICall(String url) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(getAccessToken());
            HttpEntity<String> entity = new HttpEntity<>(headers);

            return restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                refreshAccessToken();
                return makeZohoAPICall(url);
            } else {
                throw e;
            }
        }
    }
}
