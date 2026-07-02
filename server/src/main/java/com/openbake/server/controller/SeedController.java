package com.openbake.server.controller;

import com.openbake.server.config.DataSeeder;
import com.openbake.server.dto.auth.MessageResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/** Mirrors main.py's POST /seed manual trigger. */
@RestController
public class SeedController {

    private final DataSeeder dataSeeder;

    public SeedController(DataSeeder dataSeeder) {
        this.dataSeeder = dataSeeder;
    }

    @PostMapping("/seed")
    public MessageResponse seed() {
        return new MessageResponse(dataSeeder.seed());
    }
}
