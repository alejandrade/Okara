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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Authentication", description = "User authentication and account management endpoints")
public class AuthController {

    private final UserService userService;
    private final CurrentUserService currentUserService;
    private final UserMapper userMapper;
    private final UserDeleteService userDeleteService;

    @PostMapping("/login")
    @Operation(summary = "Login or register user", description = "Authenticates a user with Firebase token and creates account if needed")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Authentication successful",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserDto.class))),
        @ApiResponse(responseCode = "401", description = "Authentication failed",
                content = @Content)
    })
    public Mono<UserDto> loginOrRegister(
            @Parameter(description = "Firebase authentication token", required = true)
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader,
            @Parameter(description = "Login request with FCM token", required = true)
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
    @Operation(summary = "Get current user", description = "Retrieves the authenticated user's profile")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User profile retrieved successfully",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserDto.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized",
                content = @Content)
    })
    public Mono<UserDto> getCurrentUser() {
        return currentUserService.getCurrentUser()
                                 .map(userMapper::toUserDto);
    }

    @GetMapping("/users/{username}")
    @Operation(summary = "Get user by username", description = "Retrieves a user's public profile by username")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User found",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserDto.class))),
        @ApiResponse(responseCode = "404", description = "User not found",
                content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized",
                content = @Content)
    })
    public Mono<UserDto> getUserByUsername(
            @Parameter(description = "Username to search for", required = true)
            @PathVariable String username) {
        return userService.findByUsername(username)
                          .map(userMapper::toUserDto)
                          .switchIfEmpty(Mono.error(OkaraException.notFound("user")));
    }

    @PostMapping("/follow/{userId}")
    @Operation(summary = "Follow user", description = "Follow another user to see their posts in your feed")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User followed successfully",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserDto.class))),
        @ApiResponse(responseCode = "404", description = "User not found",
                content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized",
                content = @Content)
    })
    public Mono<UserDto> followUser(
            @Parameter(description = "ID of the user to follow", required = true)
            @PathVariable String userId) {
        return currentUserService.getCurrentUser()
                                 .flatMap(currentUser -> userService.followUser(
                                                                            currentUser.getId(),
                                                                            userId)
                                                                    .map(userMapper::toUserDto));
    }

    @PostMapping("/unfollow/{userId}")
    @Operation(summary = "Unfollow user", description = "Stop following a user")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User unfollowed successfully",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserDto.class))),
        @ApiResponse(responseCode = "404", description = "User not found",
                content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized",
                content = @Content)
    })
    public Mono<UserDto> unfollowUser(
            @Parameter(description = "ID of the user to unfollow", required = true)
            @PathVariable String userId) {
        return currentUserService.getCurrentUser()
                                 .flatMap(currentUser -> userService.unfollowUser(
                                                                            currentUser.getId(),
                                                                            userId)
                                                                    .map(userMapper::toUserDto));
    }

    @PutMapping("/profile")
    @Operation(summary = "Update user profile", description = "Update the authenticated user's profile information")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Profile updated successfully",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserDto.class))),
        @ApiResponse(responseCode = "400", description = "Invalid profile data",
                content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized",
                content = @Content)
    })
    public Mono<UserDto> updateProfile(
            @Parameter(description = "Profile data to update", required = true)
            @RequestBody Map<String, String> profileData) {
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
    @Operation(summary = "Delete user account", description = "Permanently delete a user account and all associated data")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User deleted successfully"),
        @ApiResponse(responseCode = "404", description = "User not found",
                content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized",
                content = @Content)
    })
    public Mono<Void> deleteUser(
            @Parameter(description = "Firebase UID of the user to delete", required = true)
            @PathVariable String firebaseUid) {
        log.info("🗑️ DELETE USER ENDPOINT HIT! firebaseUid={}", firebaseUid);
        
        return userDeleteService.deleteAllUserData(firebaseUid)
                .doOnSuccess(v -> log.info("🗑️ Successfully deleted user: {}", firebaseUid));
    }

    @PostMapping("/merge-accounts")
    @Operation(summary = "Merge user accounts", description = "Merge an anonymous account with a registered account")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Accounts merged successfully",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserDto.class))),
        @ApiResponse(responseCode = "400", description = "Invalid merge request",
                content = @Content),
        @ApiResponse(responseCode = "401", description = "Authentication failed",
                content = @Content)
    })
    public Mono<UserDto> mergeAccounts(
            @Parameter(description = "Account merge request", required = true)
            @RequestBody MergeAccountsRequest request) {
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