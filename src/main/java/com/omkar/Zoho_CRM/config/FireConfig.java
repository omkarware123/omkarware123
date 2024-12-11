package com.omkar.Zoho_CRM.config;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;

@Configuration
public class FireConfig {
    private static final String CREDENTIALS_FILE_PATH = "/fireCreds.json";


    @Bean
    public FirebaseApp firebaseApp() throws IOException {
        InputStream in = FireConfig.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
        FirebaseOptions options = FirebaseOptions.builder()

                .setCredentials(GoogleCredentials.fromStream(in))
                .setDatabaseUrl("https://seismic-box-432211-p1-default-rtdb.firebaseio.com/")
                .build();
        return FirebaseApp.initializeApp(options);
    }

    @Bean
    public NetHttpTransport netHttpTransport() throws GeneralSecurityException, IOException {
        return GoogleNetHttpTransport.newTrustedTransport();
    }
}
