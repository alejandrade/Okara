package io.shrouded.okara.service;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class FirebaseAuthService {

    private final FirebaseAuth firebaseAuth;

    public FirebaseToken verifyToken(String idToken) throws FirebaseAuthException {
        try {
            return firebaseAuth.verifyIdToken(idToken);
        } catch (FirebaseAuthException e) {
            log.error("Failed to verify Firebase token: {}", e.getMessage());
            throw e;
        }
    }

    public Mono<Void> deleteUser(String firebaseUid) {
        return Mono.fromCallable(() -> {
            try {
                firebaseAuth.deleteUser(firebaseUid);
                log.info("🗑️ Successfully deleted Firebase user: {}", firebaseUid);
                return null;
            } catch (FirebaseAuthException e) {
                log.error("Failed to delete Firebase user {}: {}", firebaseUid, e.getMessage());
                throw new RuntimeException("Failed to delete Firebase user: " + e.getMessage(), e);
            }
        });
    }
}