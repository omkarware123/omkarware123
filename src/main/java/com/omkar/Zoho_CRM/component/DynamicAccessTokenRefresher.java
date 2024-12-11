package com.omkar.Zoho_CRM.component;

import com.omkar.Zoho_CRM.service.Initialize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
public class DynamicAccessTokenRefresher {

    private final Initialize init;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    @Autowired
    public DynamicAccessTokenRefresher(Initialize init) {
        this.init = init;
    }


    public void scheduleTokenRefresh(long delayInSeconds) {
        scheduler.schedule(() -> {
            try {
                long expiresIn = init.refreshAccessToken();

                // Re-schedule the refresh slightly before the token expires
                long nextRefreshDelay = Math.max(1, expiresIn - 300); // Refresh 5 minutes before expiration

                // Get the current time
                LocalTime now = LocalTime.now();

                // Add 55 minutes
                LocalTime updatedTime = now.plusMinutes(55);

                // Format the time for printing
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
                String formattedTime = updatedTime.format(formatter);
                System.out.println("Next refresh scheduled at: " + formattedTime + "\n");

                scheduleTokenRefresh(nextRefreshDelay);
            } catch (Exception e) {
                System.err.println("\nError refreshing access token: " + e.getMessage());
                // Retry after a fallback delay in case of failure
                scheduleTokenRefresh(300); // Retry after 5 minutes
            }
        }, delayInSeconds, TimeUnit.SECONDS);
    }

    public void shutdownScheduler() {
        scheduler.shutdown();
    }
}

