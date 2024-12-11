package com.omkar.Zoho_CRM.service;

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
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class Initialize {

    private static final String CLIENT_ID = "1000.AVFYUVNMDYIZBV1R9QA7FAYJYPVBGN";
    private static final String CLIENT_SECRET = "922e6aec5e5149e42195b9e247e6cb5a611bd0dd0f";
    private static final String REDIRECT_URI = "https://www.google.com";
    private static String REFRESH_TOKEN = "1000.2fc873b7fb7702f39371590857015821.9efa9a260f3e52eedb179c4d713b85ad";
    private static String ACCESS_TOKEN = "1000.02e3077032aa9e94fecb79df94b5e274.469ac57b583b2077f1bb7b4ea2e655e2";

    @Autowired
    private final RestTemplate restTemplate;

    @Autowired
    public Initialize(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        System.out.println("RestTemplate injected: " + restTemplate);
    }


    @PostConstruct
    public void setupZohoSDK() {
        try {
            initialize(); // Initialize once after bean creation
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void initialize() throws Exception {
        Logger logger = new Logger.Builder()
                .level(Levels.INFO)
                .filePath("java_sdk_log.log")
                .build();

        Environment environment = INDataCenter.PRODUCTION;

        Token token = new OAuthToken.Builder()
                .clientID(CLIENT_ID)
                .clientSecret(CLIENT_SECRET)
                .refreshToken(REFRESH_TOKEN)
                .accessToken(ACCESS_TOKEN)
                .redirectURL(REDIRECT_URI)
                .build();

        TokenStore tokenstore = new FileStore("zoho_oauth_tokens.txt");

        SDKConfig sdkConfig = new SDKConfig.Builder()
                .autoRefreshFields(true)
                .pickListValidation(true)
                .build();

        new Initializer.Builder()
                .environment(environment)
                .token(token)
                .store(tokenstore)
                .SDKConfig(sdkConfig)
                .logger(logger)
                .initialize();
    }

    // Refresh the access token using the refresh token
    public long refreshAccessToken() {
        String tokenUrl = "https://accounts.zoho.in/oauth/v2/token";

        LocalDateTime now = LocalDateTime.now();                //prints current date and time w/o timezone info
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String formattedTime = now.format(formatter);
//        ZonedDateTime zonedDateTime = ZonedDateTime.now();    //prints current date and time with timezone info
//        Instant instant = Instant.now();                      //prints current UTC time
        System.out.println("\nrefreshAccessToken is called at: " + formattedTime);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> bodyParams = new LinkedMultiValueMap<>();
        bodyParams.add("client_id", CLIENT_ID);
        bodyParams.add("client_secret", CLIENT_SECRET);
        bodyParams.add("grant_type", "refresh_token");
        bodyParams.add("refresh_token", REFRESH_TOKEN);

        try {
            HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(bodyParams, headers);
            ResponseEntity<String> response = restTemplate.exchange(tokenUrl, HttpMethod.POST, requestEntity, String.class);

            JSONObject responseObject = new JSONObject(response.getBody());

            if (responseObject.has("access_token")) {
                String newAccessToken = responseObject.getString("access_token");
                System.out.println("New access token: " + newAccessToken);

                if (responseObject.has("refresh_token")) {
                    REFRESH_TOKEN = responseObject.getString("refresh_token");
                    System.out.println("New refresh token: " + REFRESH_TOKEN);
                }

                TokenStore tokenstore = new FileStore("./zoho_oauth_tokens.txt");
                Token token = new OAuthToken.Builder()
                        .clientID(CLIENT_ID)
                        .clientSecret(CLIENT_SECRET)
                        .refreshToken(REFRESH_TOKEN)
                        .accessToken(newAccessToken)
                        .build();
                tokenstore.saveToken(token);
                Initializer.getInitializer().getStore().saveToken(token);

                // Extract expires_in value
                long expiresIn = responseObject.has("expires_in") ? responseObject.getLong("expires_in") : 3600;
                System.out.println("Token expires in: " + expiresIn + " seconds");

                return expiresIn;
            }
        } catch (Exception e) {
            System.out.println("Exception is: " + e);
            e.printStackTrace();
        }

        return 3600; // Default to 1 hour if expires_in is not available
    }


    public String getAccessToken() {
        String accesst="";
        try {
            if (Initializer.getInitializer() == null) {
                initialize();
            }
            List<Token> tokens = Initializer.getInitializer().getStore().getTokens();

            for (Token token : tokens) {
                if (token instanceof OAuthToken oauthToken) {  // Ensure the token is an OAuthToken

                    // Retrieve access token and refresh token
                    String accessToken = oauthToken.getAccessToken();
                    String refreshToken = oauthToken.getRefreshToken();

                    accesst=accessToken;

                }
            }
//            System.out.println("ACCESS TOKEN: " + accesst);
            return accesst;

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public Map<String, String> retrieveTokens() throws SDKException {
        // Initialize a map to store the access and refresh tokens
        Map<String, String> tokenMap = new HashMap<>();

        // Get the list of tokens from the token store
        List<Token> tokens = Initializer.getInitializer().getStore().getTokens();

        // Iterate over the tokens and retrieve the access and refresh tokens
        for (Token token : tokens) {
            if (token instanceof OAuthToken oauthToken) {  // Ensure the token is an OAuthToken

                // Retrieve access token and refresh token
                String accessToken = oauthToken.getAccessToken();
                String refreshToken = oauthToken.getRefreshToken();

                // Store them in the map
                tokenMap.put("accessToken", accessToken);
                tokenMap.put("refreshToken", refreshToken);
            }
        }

        // Return the map with access and refresh tokens
        return tokenMap;
    }

    // Method to handle Zoho API calls with automatic token refresh
    public ResponseEntity<String> makeZohoAPICall(String url) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(getAccessToken()); // Use the access token
            HttpEntity<String> entity = new HttpEntity<>(headers);

            // Make the API call
            return restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
        } catch (HttpClientErrorException e) {
            // Check if the response status is 401 Unauthorized
            if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                // Access token has expired, refresh the token
                refreshAccessToken();
                // Retry the API call with a new access token
                return makeZohoAPICall(url);
            } else {
                throw e; // For other exceptions, throw them further
            }
        }
    }

}