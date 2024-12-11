package com.omkar.Zoho_CRM.controller;

import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.omkar.Zoho_CRM.service.CallHistoryUpdater;
import com.omkar.Zoho_CRM.service.ExotelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController
@CrossOrigin(origins = "*")
public class Controller {

    @Autowired
    private final ExotelService exotelService;

    @Autowired
    private final FirebaseApp firebaseApp;

    @Autowired
    private final CallHistoryUpdater callHistoryUpdater;

    public Controller(ExotelService exotelService, FirebaseApp firebaseApp, CallHistoryUpdater callHistoryUpdater) {
        this.exotelService = exotelService;
        this.firebaseApp = firebaseApp;
        this.callHistoryUpdater = callHistoryUpdater;
    }

    @GetMapping("/connect")
    public ResponseEntity<?> makeCall(@RequestParam String from, @RequestParam String to) throws IOException {
        return exotelService.connectCall(from, to);
    }

    @PostMapping("/statusCallback")
    public ResponseEntity<?> handleStatusCallback(
            @RequestParam("CallSid") String callSid,
            @RequestParam("From") String callFrom,
            @RequestParam("To") String callTo,
            @RequestParam("DateCreated") String dateCreated,
            @RequestParam("RecordingUrl") String recordingUrl,
            @RequestParam("Direction") String direction,
            @RequestParam("Status") String status) {
        try {
            String name = "Outgoing Call";
            callHistoryUpdater.insertRecord(name, callSid, callFrom, callTo, recordingUrl, dateCreated, status, direction);

            // Prepare the callback data
            Map<String, String> callbackData = new HashMap<>();
            callbackData.put("CallSid", callSid);
            callbackData.put("From", callFrom);
            callbackData.put("To", callTo);
            callbackData.put("DateCreated", dateCreated);
            callbackData.put("RecordingUrl", recordingUrl);
            callbackData.put("Direction", direction);
            callbackData.put("Status", status);

            return ResponseEntity.ok("Status callback handled successfully." + storeCallDetailsInFirebase(callbackData));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error handling status callback: " + e.getMessage());
        }
    }

    public ResponseEntity<String> storeCallDetailsInFirebase(Map<String, String> params) {
        try {
            System.out.println("in firebase");
            String id = params.get("CallSid");

            // Sanitize the CallSid to ensure it's a valid Firebase key
            if (id != null && !id.isEmpty()) {
                id = id.replaceAll("[.$#\\[\\]/]", ""); // Remove invalid characters

                // Sanitize the keys in the params map
                Map<String, Object> sanitizedParams = new HashMap<>();
                for (Map.Entry<String, String> entry : params.entrySet()) {
                    String sanitizedKey = entry.getKey().replaceAll("[.$#\\[\\]/]", "");
                    sanitizedParams.put(sanitizedKey, entry.getValue());
                }

                FirebaseDatabase database = FirebaseDatabase.getInstance(firebaseApp);
                DatabaseReference ref = database.getReference().child("outgoing-click-to-calls").child(id);
                ref.updateChildrenAsync(sanitizedParams);
                System.out.println("Added to Firebase successfully");
                return ResponseEntity.ok("Added to Firebase successfully\n");
            } else {
                return ResponseEntity.badRequest().body("Invalid CallSid");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred: " + e.getMessage());
        }
    }
}
