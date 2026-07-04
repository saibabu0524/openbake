package com.srivinayakabakery.comingsoon.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stores subscriber emails in a local file so no database is needed
 * for the coming-soon phase.
 */
@Service
public class SubscriberService {

    private static final Logger log = LoggerFactory.getLogger(SubscriberService.class);

    private final Set<String> subscribers = ConcurrentHashMap.newKeySet();
    private final Path storageFile;

    public SubscriberService(@Value("${app.subscribers-file:${user.home}/vinayaka-subscribers.csv}") String storageFile) {
        this.storageFile = Path.of(storageFile);
    }

    @PostConstruct
    void loadExistingSubscribers() {
        if (!Files.exists(storageFile)) {
            return;
        }
        try {
            Files.readAllLines(storageFile, StandardCharsets.UTF_8).stream()
                    .map(line -> line.split(",", 2)[0].trim().toLowerCase(Locale.ROOT))
                    .filter(email -> !email.isEmpty())
                    .forEach(subscribers::add);
            log.info("Loaded {} existing subscribers from {}", subscribers.size(), storageFile);
        } catch (IOException e) {
            log.warn("Could not read subscribers file {}", storageFile, e);
        }
    }

    /**
     * @return true if the email was newly added, false if it was already subscribed
     */
    public synchronized boolean addSubscriber(String email) {
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        if (!subscribers.add(normalized)) {
            return false;
        }
        try {
            Files.writeString(storageFile,
                    normalized + "," + Instant.now() + System.lineSeparator(),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            log.error("Failed to persist subscriber {}", normalized, e);
        }
        log.info("New subscriber: {}", normalized);
        return true;
    }
}
