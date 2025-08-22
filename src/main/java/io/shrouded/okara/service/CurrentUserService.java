package io.shrouded.okara.service;

import com.google.cloud.Timestamp;
import io.shrouded.okara.model.User;
import io.shrouded.okara.security.FirebaseAuthenticationToken;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class CurrentUserService {

    private final UserService userService;

    /**
     * Get or create the current user (reactive).
     */
    public Mono<User> getCurrentUser() {
        // Try reactive context first (for WebFlux)
        return ReactiveSecurityContextHolder.getContext()
                                            .map(SecurityContext::getAuthentication)
                                            .switchIfEmpty(Mono.defer(() -> {
                                                // Fall back to regular context (for Spring MVC)
                                                Authentication auth = SecurityContextHolder.getContext()
                                                                                           .getAuthentication();
                                                if (auth != null) {
                                                    log.info(
                                                            "Using regular SecurityContextHolder for authentication: {}",
                                                            auth.getClass().getSimpleName());
                                                    return Mono.just(auth);
                                                }
                                                return Mono.error(new RuntimeException("User not authenticated"));
                                            }))
                                            .flatMap(this::resolveUserFromAuthentication);
    }

    /**
     * Get current user's ID (reactive).
     */
    public Mono<String> getCurrentUserId() {
        return getCurrentUser().map(User::getId);
    }

    /* -------------------- Helpers -------------------- */

    private Mono<User> resolveUserFromAuthentication(Authentication authentication) {
        log.info("Resolving user from authentication: {} (type: {})",
                 authentication.getPrincipal(), authentication.getClass().getSimpleName());

        if (authentication instanceof FirebaseAuthenticationToken firebaseAuth) {
            final String firebaseUid = firebaseAuth.getPrincipal().toString();
            log.info("Firebase auth token - UID: {}, Name: {}", firebaseUid, firebaseAuth.getName());

            if (firebaseUid == null || firebaseUid.isBlank()) {
                log.error("Firebase token missing UID");
                return Mono.error(new RuntimeException("Authenticated user has no UID"));
            }

            log.info("Looking up user by Firebase UID: {}", firebaseUid);

            // First try to find user by Firebase UID
            return userService.findById(firebaseUid)
                              .doOnSuccess(user -> {
                                  if (user != null) {
                                      log.info("Found existing user by UID: {} ",
                                               firebaseUid);
                                  } else {
                                      log.warn("User lookup by UID returned null for UID: {}", firebaseUid);
                                  }
                              })
                              .switchIfEmpty(Mono.defer(() -> {
                                  log.info("User not found by UID: {}", firebaseUid);
                                  // If not found by UID, try by email (for backward compatibility)
                                  return userService.findById(firebaseUid)
                                                    .doOnSuccess(user -> {
                                                        if (user != null) {
                                                            log.info("Found existing user by firebaseUid: {} with ID: {}",
                                                                     firebaseUid,
                                                                     user.getId());
                                                        } else {
                                                            log.warn("User lookup by email returned null for firebaseUid: {}",
                                                                     firebaseUid);
                                                        }
                                                    })
                                                    .switchIfEmpty(Mono.defer(() -> {
                                                        log.info(
                                                                "Creating new user from Firebase auth - UID: {}",
                                                                firebaseUid);
                                                        User newUser = new User();
                                                        newUser.setId(firebaseUid); // Set the Firebase UID
                                                        newUser.setDisplayName(firebaseAuth.getName());
                                                        newUser.setCreatedAt(Timestamp.now());
                                                        newUser.setUpdatedAt(Timestamp.now());

                                                        log.info(
                                                                "New user object created - ID: {}, Email: {}, DisplayName: {}",
                                                                newUser.getId(),
                                                                newUser.getEmail(),
                                                                newUser.getDisplayName());

                                                        // Try to save the user
                                                        return userService.saveUser(newUser)
                                                                          .doOnSuccess(savedUser -> {
                                                                              if (savedUser != null) {
                                                                                  log.info(
                                                                                          "User saved successfully with ID: {}",
                                                                                          savedUser.getId());
                                                                                  log.info(
                                                                                          "Saved user details: Email: {}",
                                                                                          savedUser.getEmail());
                                                                              } else {
                                                                                  log.warn("User save returned null");
                                                                              }
                                                                          })
                                                                          .doOnError(error -> {
                                                                              log.error("Error saving user: {}",
                                                                                        error.getMessage());
                                                                              log.error("Error details:", error);
                                                                          });
                                                    }));
                              }))
                              .doOnError(error -> {
                                  log.error("Error in user resolution process: {}", error.getMessage());
                                  log.error("Error details:", error);
                              });
        }

        log.warn("Authentication failed - user not authenticated or invalid authentication type: {}",
                 authentication.getClass().getName());
        return Mono.error(new RuntimeException("User not authenticated"));
    }

}
