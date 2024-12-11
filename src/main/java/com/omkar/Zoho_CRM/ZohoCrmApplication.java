package com.omkar.Zoho_CRM;

import com.omkar.Zoho_CRM.component.DynamicAccessTokenRefresher;
import com.omkar.Zoho_CRM.service.Initialize;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
@EnableScheduling
public class ZohoCrmApplication {

	public static void main(String[] args) {
		ApplicationContext context = SpringApplication.run(ZohoCrmApplication.class, args);

		// Retrieve the DynamicAccessTokenRefresher bean from the context
		DynamicAccessTokenRefresher refresher = context.getBean(DynamicAccessTokenRefresher.class);

		// Call scheduleTokenRefresh or other methods
		refresher.scheduleTokenRefresh(0);	// Initial call to refresh immediately
	}

}
