package com.omkar.Zoho_CRM.service;

import com.omkar.Zoho_CRM.util.PhoneNumberUtil;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
public class CallHistoryUpdater {
    private final ZohoCrmService zohoCRMService;
    private final Initialize init;
    private final RestTemplate restTemplate;

    private static final String ZOHO_CRM_BASE_URL = "https://www.zohoapis.in/crm/v5";

    @Autowired
    public CallHistoryUpdater(ZohoCrmService zohoCRMService, Initialize init, RestTemplate restTemplate) {
        this.zohoCRMService = zohoCRMService;
        this.init = init;
        this.restTemplate = restTemplate;
    }

    // Method to insert a record into Zoho CRM
    public void insertRecord(String name, String callSid, String from, String to, String recording, String created, String status, String direction) {
        try {
            String insertUrl = ZOHO_CRM_BASE_URL + "/Call_History";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(init.getAccessToken()); // Get access token using SDK

            // Build JSON payload for inserting record
            JSONObject record = new JSONObject();
            record.put("Name", name);
            record.put("CallSid", callSid);
            record.put("From1", from);
            record.put("To", to);
            record.put("Date_Time", created);
            record.put("Recording1", recording);
            record.put("Direction", direction);
            record.put("Status", status);

            JSONObject payload = new JSONObject();
            payload.put("data", Collections.singletonList(record));
            System.out.println("\npayload: " + payload + "\n");

            HttpEntity<String> entity = new HttpEntity<>(payload.toString(), headers);

            ResponseEntity<String> response = restTemplate.exchange(insertUrl, HttpMethod.POST, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful() || response.getStatusCode() == HttpStatus.CREATED || response.getStatusCode() == HttpStatus.OK) {
                System.out.println(response.getStatusCode() + ":Record inserted successfully\nResponse = " + response.getBody() + "\n");
                updateCallHistory();
            }
        } catch (Exception e) {
            System.out.println("Exception while inserting record: " + e + "\n");
        }
    }

    public void updateCallHistory() {
        System.out.println("In updateCallHistory");

        // Fetch records from Call History, Leads, and Deals modules
        List<Map<String, Object>> callHistoryRecords = zohoCRMService.fetchModuleRecords("Call_History");
        List<Map<String, Object>> leadRecords = zohoCRMService.fetchModuleRecords("Leads");
        List<Map<String, Object>> dealRecords = zohoCRMService.fetchModuleRecords("Deals");

        // Sort call history records by Created-Time in descending order
        callHistoryRecords.sort((record1, record2) -> {
            String date1 = (String) record1.get("Created_Time");
            String date2 = (String) record2.get("Created_Time");
            return date2.compareTo(date1);
        });

        // Iterate through Call History and update either Lead or Deal Name
        for (Map<String, Object> callHistory : callHistoryRecords) {
            String fromNumber = PhoneNumberUtil.normalizeNumber((String) callHistory.get("From1"));
            String toNumber = PhoneNumberUtil.normalizeNumber((String) callHistory.get("To"));
            String recordId = (String) callHistory.get("id");
            String dealName = (String) callHistory.get("Deal_Name");

            // If no match found in Leads, check in Deals module
            if (!isLeadUpdated(leadRecords, callHistory)) {
                if (dealName == null) {
                    dealRecords.stream()
                            .filter(deal -> {
                                String mobileNumber = PhoneNumberUtil.normalizeNumber((String) deal.get("Mobile"));
                                String phoneNumber = PhoneNumberUtil.normalizeNumber((String) deal.get("Phone"));
                                return matchesCallHistory(fromNumber, toNumber, mobileNumber, phoneNumber, (String) callHistory.get("Name"));
                            })
                            .findFirst()
                            .ifPresent(deal -> {
                                String dealId = (String) deal.get("id");
                                System.out.println("Updating Call History with Deal Name: recordId = " + recordId + ", dealId = " + dealId);
                                zohoCRMService.updateCallHistoryDealName(recordId, dealId);
                            });
                    break;
                }
            } else {
                break;
            }
        }

        if(isLeadUpdated(leadRecords, callHistoryRecords.get(0))) {
            leadRecords.forEach(lead -> {
                String leadId = (String) lead.get("id");
                callHistoryRecords.stream()
                        .filter(history -> {
                            String mobileNumber = PhoneNumberUtil.normalizeNumber((String) lead.get("Mobile"));
                            String phoneNumber = PhoneNumberUtil.normalizeNumber((String) lead.get("Phone"));
                            String fromNumber = PhoneNumberUtil.normalizeNumber((String) history.get("From1"));
                            String toNumber = PhoneNumberUtil.normalizeNumber((String) history.get("To"));
                            return matchesCallHistory(fromNumber, toNumber, mobileNumber, phoneNumber, (String) history.get("Name"));
                        })
                        .findFirst()
                        .ifPresent(history -> {
                            String callHistoryId = (String) history.get("id");
                            zohoCRMService.updateCallHistoryId("Leads", leadId, callHistoryId);
                        });

            });

        } else {
            dealRecords.forEach(deal -> {
                String dealId = (String) deal.get("id");
                callHistoryRecords.stream()
                        .filter(history -> {
                            String mobileNumber = PhoneNumberUtil.normalizeNumber((String) deal.get("Mobile"));
                            String phoneNumber = PhoneNumberUtil.normalizeNumber((String) deal.get("Phone"));
                            String fromNumber = PhoneNumberUtil.normalizeNumber((String) history.get("From1"));
                            String toNumber = PhoneNumberUtil.normalizeNumber((String) history.get("To"));
                            return matchesCallHistory(fromNumber, toNumber, mobileNumber, phoneNumber, (String) history.get("Name"));
                        })
                        .findFirst()
                        .ifPresent(history -> {
                            String callHistoryId = (String) history.get("id");
                            zohoCRMService.updateCallHistoryId("Deals", dealId, callHistoryId);
                        });
            });
        }

        System.out.println("Call History processing completed.\n");
    }

    private boolean isLeadUpdated(List<Map<String, Object>> leadRecords, Map<String, Object> callHistory) {
        String fromNumber = PhoneNumberUtil.normalizeNumber((String) callHistory.get("From1"));
        String toNumber = PhoneNumberUtil.normalizeNumber((String) callHistory.get("To"));
        String recordId = (String) callHistory.get("id");
        String callTYpe = (String) callHistory.get("Name");
        String leadName = (String) callHistory.get("Lead_Name");
        if(leadName == null) {
            return leadRecords.stream()
                    .filter(lead -> {
                        String mobileNumber = PhoneNumberUtil.normalizeNumber((String) lead.get("Mobile"));
                        String phoneNumber = PhoneNumberUtil.normalizeNumber((String) lead.get("Phone"));
                        return matchesCallHistory(fromNumber, toNumber, mobileNumber, phoneNumber, callTYpe);
                    })
                    .findFirst()
                    .map(lead -> {
                        String leadId = (String) lead.get("id");
                        System.out.println("Updating Call History with Lead Name: recordId = " + recordId + ", leadId = " + leadId);
                        zohoCRMService.updateCallHistoryLeadName(recordId, leadId);
                        return true;
                    })
                    .orElse(false);
        }
        return false;
    }

    private boolean matchesCallHistory(String fromNumber, String toNumber, String mobileNumber, String phoneNumber, String callType) {
        if ("Outgoing Call".equals(callType)) {
            return toNumber != null && (toNumber.equals(mobileNumber) || toNumber.equals(phoneNumber));
        } else if ("Incoming Call".equals(callType)) {
            return fromNumber != null && (fromNumber.equals(mobileNumber) || fromNumber.equals(phoneNumber));
        }
        return false;
    }
}
