package com.omkar.Zoho_CRM.controller;

import com.omkar.Zoho_CRM.service.CallHistoryUpdater;
import com.omkar.Zoho_CRM.service.FCMService;
import com.omkar.Zoho_CRM.service.Initialize;
import com.omkar.Zoho_CRM.service.ZohoCrmService;
import com.zoho.crm.api.exception.SDKException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@CrossOrigin(origins = "*")   // Allow your extension's origin
public class ExotelCallbackController {

    @Autowired
    private FCMService fcmService;

    @Autowired
    private ZohoCrmService zohoCrmService;

    @Autowired
    private Initialize init;

    @Autowired
    private CallHistoryUpdater callHistoryUpdater;

    // Store the token (for example, in memory). In production, use a database or proper storage.
    String token = "";

    @PostMapping("/token")
    public String updateToken(@RequestParam String newToken) {
        this.token = newToken;
        System.out.println("\nfcm token: " + token + "\n");
        return token;
    }

//    @GetMapping("/before")
//    public void beforeCall(@RequestParam Map<String, String> allParams) {
//        System.out.println("before: " + allParams + "\n");
//    }

    @GetMapping("/during")
    public ResponseEntity<String> showPopup(@RequestParam Map<String, String> allParams) {
        System.out.println("during: " + allParams + "\n");
        String title = "Incoming Call";
        Map<String, String> body = new HashMap<>();

        body.put("CallSid", allParams.get("CallSid"));
        body.put("CallFrom", allParams.get("From"));
        body.put("CallTo", allParams.get("DialWhomNumber"));
        body.put("Created", allParams.get("Created"));
        body.put("User", "Existing");

        // Send notification using FCM service
        try {
            if (!zohoCrmService.recordExists(allParams.get("From"))) {
                body.put("User", "New");
            }
            fcmService.sendFcmNotification(token, title, body);
            System.out.println("popup done.\n");
            return ResponseEntity.ok("Background message sent and details updated in zoho\n");
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

//    @GetMapping("/after")
//    public void afterCall(@RequestParam Map<String, String> allParams) {
//        System.out.println("after: " + allParams + "\n");
//        zohoCrmService.insertRecord("Incoming Call", allParams.get("CallSid"), allParams.get("From"), allParams.get("DialWhomNumber"), allParams.get("RecordingUrl"), allParams.get("Created"), allParams.get("Status"), allParams.get("Direction"));
//        callHistoryUpdater.updateCallHistoryWithLeadName();
//        callHistoryUpdater.updateCallHistoryWithDealName();
//    }

    @GetMapping("/nobody")
    public ResponseEntity<?> nobodyCall(@RequestParam Map<String, String> allParams) {
        System.out.println("nobody: " + allParams + "\n");
        try {
            callHistoryUpdater.insertRecord("Incoming Call", allParams.get("CallSid"), allParams.get("From"), allParams.get("DialWhomNumber"), null, allParams.get("Created"), "Nobody answered", allParams.get("Direction"));
            return ResponseEntity.ok("Call History module updated\n");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @GetMapping("/retrieveTokens")
    public Map<String, String> retrieveToken() throws SDKException {
        return init.retrieveTokens();
    }
}
