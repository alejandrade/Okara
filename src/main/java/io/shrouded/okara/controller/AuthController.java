package io.shrouded.okara.controller;

import io.shrouded.okara.dto.user.LoginRequest;
import io.shrouded.okara.dto.user.UserDto;
import io.shrouded.okara.dto.user.MergeAccountsRequest;
import io.shrouded.okara.exception.OkaraException;
import io.shrouded.okara.mapper.UserMapper;
import io.shrouded.okara.service.CurrentUserService;
import io.shrouded.okara.service.UserService;
import io.shrouded.okara.service.UserDeleteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.DeleteMapping;
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
    private final UserDeleteService userDeleteService;

    @PostMapping("/login")
    public Mono<UserDto> loginOrRegister(@RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader,
                                         @RequestBody LoginRequest loginRequest) {
        log.info("🔐 LOGIN/REGISTER ENDPOINT HIT!");
        String token = authHeader.substring(7);
        return userService.getOrCreateUser(token, loginRequest.fcmToken())
                          .doOnSuccess(user -> log.info(
                                  "🔐 Successfully logged in/registered user: {}",
                                  user.getEmail()))
                          .map(userMapper::toUserDto)
                          .onErrorMap(e -> {
                              log.error("🔐 Login/register failed: {}", e.getMessage(), e);
                              return OkaraException.unauthorized("Authentication failed: " + e.getMessage());
                          });
    }

    @GetMapping("/me")
    public Mono<UserDto> getCurrentUser() {
        return currentUserService.getCurrentUser()
                                 .map(userMapper::toUserDto);
    }

    @GetMapping("/users/{username}")
    public Mono<UserDto> getUserByUsername(@PathVariable String username) {
        return userService.findByUsername(username)
                          .map(userMapper::toUserDto)
                          .switchIfEmpty(Mono.error(OkaraException.notFound("user")));
    }

    @PostMapping("/follow/{userId}")
    public Mono<UserDto> followUser(@PathVariable String userId) {
        return currentUserService.getCurrentUser()
                                 .flatMap(currentUser -> userService.followUser(
                                                                            currentUser.getId(),
                                                                            userId)
                                                                    .map(userMapper::toUserDto));
    }

    @PostMapping("/unfollow/{userId}")
    public Mono<UserDto> unfollowUser(@PathVariable String userId) {
        return currentUserService.getCurrentUser()
                                 .flatMap(currentUser -> userService.unfollowUser(
                                                                            currentUser.getId(),
                                                                            userId)
                                                                    .map(userMapper::toUserDto));
    }

    @PutMapping("/profile")
    public Mono<UserDto> updateProfile(@RequestBody Map<String, String> profileData) {
        return currentUserService.getCurrentUser()
                                 .flatMap(currentUser -> userService.updateProfile(
                                                                            currentUser.getId(),
                                                                            profileData.get("displayName"),
                                                                            profileData.get("bio"),
                                                                            profileData.get("location"),
                                                                            profileData.get("website"))
                                                                    .map(userMapper::toUserDto));
    }

    @DeleteMapping("/users/{firebaseUid}")
    public Mono<Void> deleteUser(@PathVariable String firebaseUid) {
        log.info("🗑️ DELETE USER ENDPOINT HIT! firebaseUid={}", firebaseUid);
        
        return userDeleteService.deleteAllUserData(firebaseUid)
                .doOnSuccess(v -> log.info("🗑️ Successfully deleted user: {}", firebaseUid));
    }

    @PostMapping("/merge-accounts")
    public Mono<UserDto> mergeAccounts(@RequestBody MergeAccountsRequest request) {
        log.info("🔄 MERGE ACCOUNTS ENDPOINT HIT!");
        
        return userService.mergeAccounts(request.anonymousUserToken(), request.newUserToken())
                          .doOnSuccess(user -> log.info(
                                  "🔄 Successfully merged accounts for user: {}",
                                  user.getEmail() != null ? user.getEmail() : user.getId()))
                          .map(userMapper::toUserDto)
                          .onErrorMap(e -> {
                              log.error("🔄 Account merge failed: {}", e.getMessage(), e);
                              return OkaraException.badRequest("Account merge failed: " + e.getMessage());
                          });
    }
}