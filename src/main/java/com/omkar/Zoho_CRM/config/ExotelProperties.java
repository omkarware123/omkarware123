package com.omkar.Zoho_CRM.config;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "exotel")
@Data
@Getter
@Setter
public class ExotelProperties {

    private String apiKey;
    private String apiToken;
    private String subDomain;
    private String sid;
    private String appid;

}

