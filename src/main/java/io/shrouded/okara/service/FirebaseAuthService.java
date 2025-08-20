package io.shrouded.okara.service;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class FirebaseAuthService {

    public FirebaseToken verifyToken(String idToken) throws FirebaseAuthException {
        try {
            FirebaseAuth instance = FirebaseAuth.getInstance();
            return instance.verifyIdToken(idToken);
        } catch (FirebaseAuthException e) {
            log.error("Failed to verify Firebase token: {}", e.getMessage());
            throw e;
        }
    }
}