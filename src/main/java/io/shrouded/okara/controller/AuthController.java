package io.shrouded.okara.controller;

import io.shrouded.okara.model.User;
import io.shrouded.okara.service.CurrentUserService;
import io.shrouded.okara.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {
    
    private final UserService userService;
    private final CurrentUserService currentUserService;
    
    @PostMapping("/login")
    public ResponseEntity<?> loginOrRegister() {
        try {
            User user = currentUserService.getCurrentUser();
            
            return ResponseEntity.ok(Map.of(
                "user", user,
                "message", "Authentication successful"
            ));
            
        } catch (Exception e) {
            log.error("Login/register failed: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                "error", "Internal server error",
                "message", e.getMessage()
            ));
        }
    }
    
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser() {
        try {
            User user = currentUserService.getCurrentUser();
            return ResponseEntity.ok(user);
            
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                "error", "Internal server error"
            ));
        }
    }
    
    @GetMapping("/users/{username}")
    public ResponseEntity<?> getUserByUsername(@PathVariable String username) {
        return userService.findByUsername(username)
                .map(user -> ResponseEntity.ok(user))
                .orElse(ResponseEntity.notFound().build());
    }
    
    @PostMapping("/follow/{userId}")
    public ResponseEntity<?> followUser(@PathVariable String userId) {
        try {
            User currentUser = currentUserService.getCurrentUser();
            User updatedUser = userService.followUser(currentUser.getId(), userId);
            
            return ResponseEntity.ok(Map.of(
                "message", "Successfully followed user",
                "user", updatedUser
            ));
            
        } catch (Exception e) {
            return ResponseEntity.status(400).body(Map.of(
                "error", e.getMessage()
            ));
        }
    }
    
    @PostMapping("/unfollow/{userId}")
    public ResponseEntity<?> unfollowUser(@PathVariable String userId) {
        try {
            User currentUser = currentUserService.getCurrentUser();
            User updatedUser = userService.unfollowUser(currentUser.getId(), userId);
            
            return ResponseEntity.ok(Map.of(
                "message", "Successfully unfollowed user",
                "user", updatedUser
            ));
            
        } catch (Exception e) {
            return ResponseEntity.status(400).body(Map.of(
                "error", e.getMessage()
            ));
        }
    }
    
    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(@RequestBody Map<String, String> profileData) {
        try {
            User currentUser = currentUserService.getCurrentUser();
            
            User updatedUser = userService.updateProfile(
                    currentUser.getId(),
                    profileData.get("displayName"),
                    profileData.get("bio"),
                    profileData.get("location"),
                    profileData.get("website")
            );
            
            return ResponseEntity.ok(updatedUser);
            
        } catch (Exception e) {
            return ResponseEntity.status(400).body(Map.of(
                "error", e.getMessage()
            ));
        }
    }
}