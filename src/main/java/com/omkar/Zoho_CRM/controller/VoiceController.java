package com.omkar.Zoho_CRM.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/voice")
public class VoiceController {

    @GetMapping("/greet")
    public String greet(){
        return "Welcome to UMS Tech Labs!";
    }

    @GetMapping("/ivr")
    public String ivrMenu(){
        return "Press 1 to connect with sales, press 2 to connect with support";
    }

    @GetMapping("/ivr/invalid")
    public String invalidInput(){
        System.out.println("Sorry! you entered wrong digit");
        return "Sorry! you entered wrong digit";
    }

    @GetMapping("/ivr/noDial")
    public String noDial(){
        System.out.println("Sorry! we are unable to connect you");
        return "Sorry! we are unable to connect you";
    }

    @GetMapping("/connect/queued")
    public String queued(){
        System.out.println("Please wait for some time!");
        return "Please wait for some time!";
    }

    @GetMapping("/connect/noResponse")
    public String noResponse(){
        System.out.println("No one responded");
        return "No one responded";
    }

    @GetMapping("/terminal")
    public String endCall(){
        return "Thanks for calling!";
    }

    @GetMapping("/passthru/error")
    public String passthruError(){
        return "Something went wrong!";
    }

    @GetMapping("/endCall")
    public String endCallWithError(){
        return "Call ended";
    }
}
