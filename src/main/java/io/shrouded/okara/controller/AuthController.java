package io.shrouded.okara.controller;

import io.shrouded.okara.dto.UserDto;
import io.shrouded.okara.exception.OkaraException;
import io.shrouded.okara.service.CurrentUserService;
import io.shrouded.okara.service.DtoMappingService;
import io.shrouded.okara.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {
    
    private final UserService userService;
    private final CurrentUserService currentUserService;
    private final DtoMappingService dtoMappingService;
    
    @PostMapping("/login")
    public Mono<ResponseEntity<UserDto>> loginOrRegister(@RequestBody LoginRequest request) {
        log.info("🔐 LOGIN/REGISTER ENDPOINT HIT!");
        
        if (request.idToken == null || request.idToken.trim().isEmpty()) {
            return Mono.just(ResponseEntity.badRequest().body(null));
        }
        
        return userService.getOrCreateUser(request.idToken)
            .doOnSuccess(user -> log.info("🔐 Successfully logged in/registered user: {}", user.getEmail()))
            .map(user -> ResponseEntity.ok(dtoMappingService.toUserDto(user)))
            .onErrorResume(e -> {
                log.error("🔐 Login/register failed: {}", e.getMessage());
                return Mono.just(ResponseEntity.status(401).build());
            });
    }
    
    @GetMapping("/me")
    public Mono<ResponseEntity<UserDto>> getCurrentUser() {
        return currentUserService.getCurrentUser()
            .map(user -> ResponseEntity.ok(dtoMappingService.toUserDto(user)));
    }
    
    @GetMapping("/users/{username}")
    public Mono<ResponseEntity<UserDto>> getUserByUsername(@PathVariable String username) {
        return userService.findByUsername(username)
                .map(user -> ResponseEntity.ok(dtoMappingService.toUserDto(user)))
                .switchIfEmpty(Mono.error(OkaraException.notFound("user")));
    }
    
    @PostMapping("/follow/{userId}")
    public Mono<ResponseEntity<UserDto>> followUser(@PathVariable String userId) {
        return currentUserService.getCurrentUser()
            .flatMap(currentUser -> 
                userService.followUser(currentUser.getId(), userId)
                    .map(updatedUser -> ResponseEntity.ok(dtoMappingService.toUserDto(updatedUser)))
            );
    }
    
    @PostMapping("/unfollow/{userId}")
    public Mono<ResponseEntity<UserDto>> unfollowUser(@PathVariable String userId) {
        return currentUserService.getCurrentUser()
            .flatMap(currentUser -> 
                userService.unfollowUser(currentUser.getId(), userId)
                    .map(updatedUser -> ResponseEntity.ok(dtoMappingService.toUserDto(updatedUser)))
            );
    }
    
    @PutMapping("/profile")
    public Mono<ResponseEntity<UserDto>> updateProfile(@RequestBody Map<String, String> profileData) {
        return currentUserService.getCurrentUser()
            .flatMap(currentUser -> 
                userService.updateProfile(
                    currentUser.getId(),
                    profileData.get("displayName"),
                    profileData.get("bio"),
                    profileData.get("location"),
                    profileData.get("website")
                )
                .map(updatedUser -> ResponseEntity.ok(dtoMappingService.toUserDto(updatedUser)))
            );
    }
    
    // Request DTO
    public static class LoginRequest {
        public String idToken;
    }
}