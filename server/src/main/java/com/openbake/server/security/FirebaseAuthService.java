package com.openbake.server.security;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import com.openbake.server.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Lazy Firebase Admin SDK init, mirroring backend/app/utils/firebase.py. */
@Component
public class FirebaseAuthService {

    private static final Logger log = LoggerFactory.getLogger(FirebaseAuthService.class);

    private final AppProperties appProperties;
    private volatile boolean initialized = false;

    public FirebaseAuthService(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    public record GoogleUser(String uid, String email, String name, String picture) {}

    private synchronized void ensureInitialized() {
        if (initialized || !FirebaseApp.getApps().isEmpty()) {
            initialized = true;
            return;
        }
        Path credPath = Path.of(appProperties.getFirebaseCredentialsPath());
        if (!Files.exists(credPath)) {
            log.warn("Firebase credentials not found at {}. Google auth / push notifications unavailable.", credPath);
            return;
        }
        try (FileInputStream in = new FileInputStream(credPath.toFile())) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(in))
                    .build();
            FirebaseApp.initializeApp(options);
            initialized = true;
        } catch (IOException e) {
            log.warn("Failed to initialize Firebase: {}", e.getMessage());
        }
    }

    public GoogleUser verifyIdToken(String idToken) {
        ensureInitialized();
        if (!initialized) {
            throw new IllegalStateException("Firebase is not configured on this server");
        }
        try {
            FirebaseToken decoded = FirebaseAuth.getInstance().verifyIdToken(idToken);
            return new GoogleUser(
                    decoded.getUid(),
                    decoded.getEmail(),
                    decoded.getName() != null ? decoded.getName() : "",
                    decoded.getPicture() != null ? decoded.getPicture() : ""
            );
        } catch (Exception e) {
            throw new IllegalStateException("Invalid Google token", e);
        }
    }
}
