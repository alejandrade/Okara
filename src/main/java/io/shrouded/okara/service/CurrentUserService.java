package io.shrouded.okara.service;

import io.shrouded.okara.model.User;
import io.shrouded.okara.security.FirebaseAuthenticationToken;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CurrentUserService {
    
    private final UserService userService;
    
    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication instanceof FirebaseAuthenticationToken firebaseAuth) {
            String email = firebaseAuth.getEmail();
            
            // Get or create user based on Firebase info
            return userService.findByEmail(email)
                    .orElseGet(() -> {
                        // Create user from Firebase auth data
                        User newUser = new User();
                        newUser.setEmail(firebaseAuth.getEmail());
                        newUser.setDisplayName(firebaseAuth.getName());
                        newUser.setUsername(generateUsernameFromEmail(firebaseAuth.getEmail()));
                        return userService.saveUser(newUser);
                    });
        }
        
        throw new RuntimeException("User not authenticated");
    }
    
    public String getCurrentUserId() {
        return getCurrentUser().getId();
    }
    
    private String generateUsernameFromEmail(String email) {
        String baseUsername = email.split("@")[0]
                .toLowerCase()
                .replaceAll("[^a-z0-9]", "");
        
        if (baseUsername.length() < 3) {
            baseUsername = "user" + baseUsername;
        }
        
        return baseUsername;
    }
}