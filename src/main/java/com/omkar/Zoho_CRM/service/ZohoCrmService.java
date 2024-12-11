package com.omkar.Zoho_CRM.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class ZohoCrmService {

    private static final String ZOHO_CRM_BASE_URL = "https://www.zohoapis.in/crm/v5";

    @Autowired
    private RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Autowired
    private Initialize init;

    public ZohoCrmService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    // Method to check if a record exists in Zoho CRM
    public boolean recordExists(String from) {
        try {
            String searchUrl = ZOHO_CRM_BASE_URL + "/Call_History/search?criteria=(From1:equals:" + from + ")";

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(init.getAccessToken()); // Get access token using SDK
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(searchUrl, HttpMethod.GET, entity, String.class);
            if (response.getBody() != null) {
                JSONObject responseObject = new JSONObject(response.getBody());
                System.out.println(response.getStatusCode() + ":The number " + from + " already exists in call history.\n");
                return responseObject.has("data") && !responseObject.getJSONArray("data").isEmpty();
            } else {
                return false;
            }
        } catch (HttpClientErrorException e) {
            System.out.println("\nException while checking record existence: " + e);
        }
        return false;
    }

    public List<Map<String, Object>> fetchModuleRecords(String moduleName) {
        String url1 = ZOHO_CRM_BASE_URL + "/" + moduleName + "?fields=Mobile,Phone,Lead_Name,id,Call_History_Id";
        String url2 = ZOHO_CRM_BASE_URL + "/" + moduleName + "?fields=Deal_Name,Amount,Stage,Account_Name,Closing_Date,Call_History_Id,id,Mobile,Phone";
        String url3 = ZOHO_CRM_BASE_URL + "/" + moduleName + "?fields=From1,To,Lead_Name,Deal_Name,id,Name,Date_Time,Recording1,Direction,Status,CallSid,Created_Time";

        HttpHeaders headers = new HttpHeaders();
        try {
            headers.setBearerAuth(init.getAccessToken());
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            String url = "";
            if (Objects.equals(moduleName, "Leads")) {
                url = url1;
            } else if (Objects.equals(moduleName, "Deals")) {
                url = url2;
            } else if (Objects.equals(moduleName, "Call_History")) {
                url = url3;
            }

            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                return (List<Map<String, Object>>) response.getBody().get("data");
            }
            System.out.println(moduleName + " is null");
            return null;
        } catch (RestClientException e) {
            System.out.println("\nException while fetching records: " + e);
            return null;
        }
    }

    public void updateCallHistoryLeadName(String recordId, String leadId) {
        String url = ZOHO_CRM_BASE_URL + "/Call_History";
        HttpHeaders headers = new HttpHeaders();
        try {
            headers.setBearerAuth(init.getAccessToken());
            headers.setContentType(MediaType.APPLICATION_JSON);

            String updateFields = """
                    {
                      "data": [
                        {
                          "id": "%s",
                          "Lead_Name": "%s"
                        }
                      ]
                    }""".formatted(recordId, leadId);

            HttpEntity<String> entity = new HttpEntity<>(updateFields, headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.PUT, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                System.out.println(response.getStatusCode() + ":Lead Name Updated\nResponse = " + response.getBody() + "\n");
            }
        } catch (RestClientException e) {
            System.out.println("\nException while updating lead name: " + e);
        }
    }

    public void updateCallHistoryDealName(String recordId, String dealId) {
        String url = ZOHO_CRM_BASE_URL + "/Call_History/" + recordId;
        HttpHeaders headers = new HttpHeaders();
        try {
            headers.setBearerAuth(init.getAccessToken());
            headers.setContentType(MediaType.APPLICATION_JSON);

            String updatePayload = """
                    {
                      "data": [
                        {
                          "id": "%s",
                          "Deal_Name": "%s"
                        }
                      ]
                    }""".formatted(recordId, dealId);

            HttpEntity<String> entity = new HttpEntity<>(updatePayload, headers);

            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.PUT, entity, Map.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                System.out.println(response.getStatusCode() + ":Deal Name Updated\nResponse = " + response.getBody() + "\n");
            }
        } catch (RestClientException e) {
            System.out.println("\nException while updating deal name: " + e);
            e.printStackTrace();
        }
    }

    public void updateCallHistoryId(String moduleName, String recordId, String callHistoryId) {
        String url = ZOHO_CRM_BASE_URL + "/" + moduleName + "/" + recordId;
        HttpHeaders headers = new HttpHeaders();
        try {
            headers.setBearerAuth(init.getAccessToken());
            headers.setContentType(MediaType.APPLICATION_JSON);

            String updatePayload = getString(moduleName, recordId, callHistoryId);

            HttpEntity<String> entity = new HttpEntity<>(updatePayload, headers);

            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.PUT, entity, Map.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                System.out.println(response.getStatusCode() + ":Call History Id Updated in " + moduleName + "\nResponse: " + response.getBody() + "\n");
            }
        } catch (RestClientException e) {
            System.out.println("\nException while updating call history id: " + e);
            e.printStackTrace();
        }
    }

    private String getString(String moduleName, String recordId, String callHistoryId) {
        String updatePayload = "";
        if (Objects.equals(moduleName, "Leads")) {
            updatePayload = """
                    {
                      "data": [
                        {
                          "id": "%s",
                          "Call_History_Id": "%s"
                        }
                      ]
                    }""".formatted(recordId, callHistoryId);
        } else if (Objects.equals(moduleName, "Deals")) {
            updatePayload = """
                    {
                      "data": [
                        {
                          "id": "%s",
                          "Call_History_Id": {
                            "id": "%s"
                          }
                        }
                      ]
                    }""".formatted(recordId, callHistoryId);
        }
        return updatePayload;
    }
}