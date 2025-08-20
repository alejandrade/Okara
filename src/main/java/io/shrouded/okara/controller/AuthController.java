package io.shrouded.okara.controller;

import io.shrouded.okara.dto.user.UserDto;
import io.shrouded.okara.exception.OkaraException;
import io.shrouded.okara.mapper.UserMapper;
import io.shrouded.okara.service.CurrentUserService;
import io.shrouded.okara.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.security.oauth2.resource.OAuth2ResourceServerProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final UserService userService;
    private final CurrentUserService currentUserService;
    private final UserMapper userMapper;

    @PostMapping("/login")
    public Mono<ResponseEntity<UserDto>> loginOrRegister(@RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader) {
        log.info("🔐 LOGIN/REGISTER ENDPOINT HIT!");
        String token = authHeader.substring(7);
        return userService.getOrCreateUser(token)
                          .doOnSuccess(user -> log.info(
                                  "🔐 Successfully logged in/registered user: {}",
                                  user.getEmail()))
                          .map(user -> ResponseEntity.ok(userMapper.toUserDto(user)))
                          .onErrorResume(e -> {
                              log.error("🔐 Login/register failed: {}", e.getMessage(), e);
                              return Mono.just(ResponseEntity.status(401).build());
                          });
    }

    @GetMapping("/me")
    public Mono<ResponseEntity<UserDto>> getCurrentUser() {
        return currentUserService.getCurrentUser()
                                 .map(user -> ResponseEntity.ok(userMapper.toUserDto(user)));
    }

    @GetMapping("/users/{username}")
    public Mono<ResponseEntity<UserDto>> getUserByUsername(@PathVariable String username) {
        return userService.findByUsername(username)
                          .map(user -> ResponseEntity.ok(userMapper.toUserDto(user)))
                          .switchIfEmpty(Mono.error(OkaraException.notFound("user")));
    }

    @PostMapping("/follow/{userId}")
    public Mono<ResponseEntity<UserDto>> followUser(@PathVariable String userId) {
        return currentUserService.getCurrentUser()
                                 .flatMap(currentUser -> userService.followUser(
                                                                            currentUser.getId(),
                                                                            userId)
                                                                    .map(updatedUser -> ResponseEntity.ok(
                                                                            userMapper.toUserDto(
                                                                                    updatedUser))));
    }

    @PostMapping("/unfollow/{userId}")
    public Mono<ResponseEntity<UserDto>> unfollowUser(@PathVariable String userId) {
        return currentUserService.getCurrentUser()
                                 .flatMap(currentUser -> userService.unfollowUser(
                                                                            currentUser.getId(),
                                                                            userId)
                                                                    .map(updatedUser -> ResponseEntity.ok(
                                                                            userMapper.toUserDto(
                                                                                    updatedUser))));
    }

    @PutMapping("/profile")
    public Mono<ResponseEntity<UserDto>> updateProfile(@RequestBody Map<String, String> profileData) {
        return currentUserService.getCurrentUser()
                                 .flatMap(currentUser -> userService.updateProfile(
                                                                            currentUser.getId(),
                                                                            profileData.get("displayName"),
                                                                            profileData.get("bio"),
                                                                            profileData.get("location"),
                                                                            profileData.get("website"))
                                                                    .map(updatedUser -> ResponseEntity.ok(
                                                                            userMapper.toUserDto(
                                                                                    updatedUser))));
    }
}