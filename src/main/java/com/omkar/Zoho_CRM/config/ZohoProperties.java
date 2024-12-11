package com.omkar.Zoho_CRM.config;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "zoho")
@Data
@Getter
@Setter
public class ZohoProperties {
    private String clientId;
    private String clientSecret;
    private String redirectUri;
    private String refreshToken;
    private String accessToken;
}
