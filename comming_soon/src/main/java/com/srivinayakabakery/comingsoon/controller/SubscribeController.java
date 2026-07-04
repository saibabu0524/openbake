package com.srivinayakabakery.comingsoon.controller;

import com.srivinayakabakery.comingsoon.dto.SubscribeRequest;
import com.srivinayakabakery.comingsoon.service.EmailNotificationService;
import com.srivinayakabakery.comingsoon.service.SubscriberService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class SubscribeController {

    private final SubscriberService subscriberService;
    private final EmailNotificationService emailNotificationService;

    public SubscribeController(SubscriberService subscriberService,
                               EmailNotificationService emailNotificationService) {
        this.subscriberService = subscriberService;
        this.emailNotificationService = emailNotificationService;
    }

    @PostMapping("/subscribe")
    public ResponseEntity<Map<String, String>> subscribe(@Valid @RequestBody SubscribeRequest request) {
        boolean added = subscriberService.addSubscriber(request.email());
        if (!added) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "You are already on our list!"));
        }
        emailNotificationService.notifyNewSubscriber(request.email());
        return ResponseEntity.ok(Map.of("message", "Thank you! We will notify you when we launch."));
    }
}
