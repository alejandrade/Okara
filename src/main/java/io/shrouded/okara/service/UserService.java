package io.shrouded.okara.service;

import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import io.shrouded.okara.model.User;
import io.shrouded.okara.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {
    
    private final UserRepository userRepository;
    private final FirebaseAuthService firebaseAuthService;
    
    public User getOrCreateUser(String idToken) throws FirebaseAuthException {
        FirebaseToken decodedToken = firebaseAuthService.verifyToken(idToken);
        
        String firebaseUid = decodedToken.getUid();
        String email = decodedToken.getEmail();
        String name = decodedToken.getName();
        String picture = (String) decodedToken.getClaims().get("picture");
        
        // Check if user exists by email
        Optional<User> existingUser = userRepository.findByEmail(email);
        if (existingUser.isPresent()) {
            log.info("User found: {}", email);
            return existingUser.get();
        }
        
        // Create new user
        String username = generateUniqueUsername(email, name);
        User newUser = new User(username, email, name != null ? name : username);
        newUser.setProfileImageUrl(picture);
        newUser.setCreatedAt(LocalDateTime.now());
        newUser.setUpdatedAt(LocalDateTime.now());
        
        User savedUser = userRepository.save(newUser);
        log.info("New user created: {} with username: {}", email, username);
        
        return savedUser;
    }
    
    public Optional<User> findById(String id) {
        return userRepository.findById(id);
    }
    
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }
    
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }
    
    public User saveUser(User user) {
        return userRepository.save(user);
    }
    
    public User followUser(String followerId, String followeeId) {
        User follower = userRepository.findById(followerId)
                .orElseThrow(() -> new RuntimeException("Follower not found"));
        User followee = userRepository.findById(followeeId)
                .orElseThrow(() -> new RuntimeException("User to follow not found"));
        
        // Add to following/followers lists
        if (!follower.getFollowing().contains(followeeId)) {
            follower.getFollowing().add(followeeId);
            follower.setFollowingCount(follower.getFollowingCount() + 1);
            follower.setUpdatedAt(LocalDateTime.now());
            
            followee.getFollowers().add(followerId);
            followee.setFollowersCount(followee.getFollowersCount() + 1);
            followee.setUpdatedAt(LocalDateTime.now());
            
            userRepository.save(follower);
            userRepository.save(followee);
        }
        
        return follower;
    }
    
    public User unfollowUser(String followerId, String followeeId) {
        User follower = userRepository.findById(followerId)
                .orElseThrow(() -> new RuntimeException("Follower not found"));
        User followee = userRepository.findById(followeeId)
                .orElseThrow(() -> new RuntimeException("User to unfollow not found"));
        
        // Remove from following/followers lists
        if (follower.getFollowing().contains(followeeId)) {
            follower.getFollowing().remove(followeeId);
            follower.setFollowingCount(follower.getFollowingCount() - 1);
            follower.setUpdatedAt(LocalDateTime.now());
            
            followee.getFollowers().remove(followerId);
            followee.setFollowersCount(followee.getFollowersCount() - 1);
            followee.setUpdatedAt(LocalDateTime.now());
            
            userRepository.save(follower);
            userRepository.save(followee);
        }
        
        return follower;
    }
    
    public User updateProfile(String userId, String displayName, String bio, String location, String website) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        if (displayName != null) user.setDisplayName(displayName);
        if (bio != null) user.setBio(bio);
        if (location != null) user.setLocation(location);
        if (website != null) user.setWebsite(website);
        user.setUpdatedAt(LocalDateTime.now());
        
        return userRepository.save(user);
    }
    
    private String generateUniqueUsername(String email, String name) {
        String baseUsername;
        
        if (name != null && !name.trim().isEmpty()) {
            baseUsername = name.toLowerCase()
                    .replaceAll("[^a-z0-9]", "")
                    .substring(0, Math.min(name.length(), 15));
        } else {
            baseUsername = email.split("@")[0]
                    .toLowerCase()
                    .replaceAll("[^a-z0-9]", "");
        }
        
        if (baseUsername.length() < 3) {
            baseUsername = "user" + baseUsername;
        }
        
        String username = baseUsername;
        int counter = 1;
        
        while (userRepository.existsByUsername(username)) {
            username = baseUsername + counter;
            counter++;
        }
        
        return username;
    }
}