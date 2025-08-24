package io.shrouded.okara.controller;

import io.shrouded.okara.dto.chatroom.ChatroomDto;
import io.shrouded.okara.dto.chatroom.ChatroomListResponse;
import io.shrouded.okara.dto.chatroom.CreateChatroomRequest;
import io.shrouded.okara.exception.OkaraException;
import io.shrouded.okara.service.ChatroomService;
import io.shrouded.okara.service.CurrentUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@RestController
@RequestMapping("/api/chatrooms")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Chatrooms", description = "Chatroom management and participation endpoints")
@SecurityRequirement(name = "bearerAuth")
public class ChatroomController {

    private final ChatroomService chatroomService;
    private final CurrentUserService currentUserService;

    @GetMapping
    @Operation(summary = "Get user chatrooms", description = "Retrieves chatrooms that the authenticated user has joined")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Chatrooms retrieved successfully",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = ChatroomListResponse.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized",
                content = @Content)
    })
    public Mono<ChatroomListResponse> getChatrooms(
            @Parameter(description = "Maximum number of chatrooms to return", example = "20")
            @RequestParam(defaultValue = "20") int limit,
            @Parameter(description = "Cursor for pagination")
            @RequestParam(required = false) String cursor) {
        
        log.info("🏠 GET CHATROOMS ENDPOINT HIT! limit={}, cursor={}", limit, cursor);
        
        return currentUserService.getCurrentUser()
                .flatMap(user -> chatroomService.getUserChatrooms(user.getId(), limit, cursor))
                .doOnSuccess(response -> log.info("🏠 Successfully retrieved {} chatrooms", 
                        response.chatrooms().size()));
    }

    @PostMapping
    @Operation(summary = "Create chatroom", description = "Creates a new chatroom with the authenticated user as owner")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Chatroom created successfully",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = ChatroomDto.class))),
        @ApiResponse(responseCode = "400", description = "Invalid chatroom data",
                content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized",
                content = @Content)
    })
    public Mono<ChatroomDto> createChatroom(
            @Parameter(description = "Chatroom creation request", required = true)
            @RequestBody CreateChatroomRequest request) {
        log.info("🏠 CREATE CHATROOM ENDPOINT HIT! name={}, type={}", request.name(), request.type());
        
        return currentUserService.getCurrentUser()
                .flatMap(user -> chatroomService.createChatroom(user.getId(), request))
                .doOnSuccess(chatroom -> log.info("🏠 Successfully created chatroom: {}", chatroom.name()));
    }

    @GetMapping("/{chatroomId}")
    @Operation(summary = "Get chatroom by ID", description = "Retrieves detailed information about a specific chatroom")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Chatroom found",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = ChatroomDto.class))),
        @ApiResponse(responseCode = "404", description = "Chatroom not found",
                content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized",
                content = @Content)
    })
    public Mono<ChatroomDto> getChatroomById(
            @Parameter(description = "ID of the chatroom", required = true)
            @PathVariable String chatroomId) {
        log.info("🏠 GET CHATROOM BY ID ENDPOINT HIT! chatroomId={}", chatroomId);
        
        return chatroomService.getChatroomById(chatroomId)
                .doOnSuccess(chatroom -> log.info("🏠 Successfully retrieved chatroom: {}", chatroom.name()));
    }

    @PostMapping("/{chatroomId}/join")
    @Operation(summary = "Join chatroom", description = "Join a chatroom to participate in conversations")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully joined chatroom",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = ChatroomDto.class))),
        @ApiResponse(responseCode = "404", description = "Chatroom not found",
                content = @Content),
        @ApiResponse(responseCode = "409", description = "Already a member",
                content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized",
                content = @Content)
    })
    public Mono<ChatroomDto> joinChatroom(
            @Parameter(description = "ID of the chatroom to join", required = true)
            @PathVariable String chatroomId) {
        log.info("🏠 JOIN CHATROOM ENDPOINT HIT! chatroomId={}", chatroomId);
        
        return currentUserService.getCurrentUser()
                .flatMap(user -> chatroomService.joinChatroom(user.getId(), chatroomId))
                .doOnSuccess(chatroom -> log.info("🏠 Successfully joined chatroom: {}", chatroom.name()));
    }

    @PostMapping("/{chatroomId}/leave")
    @Operation(summary = "Leave chatroom", description = "Leave a chatroom and stop receiving its messages")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully left chatroom"),
        @ApiResponse(responseCode = "404", description = "Chatroom not found",
                content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized",
                content = @Content)
    })
    public Mono<Void> leaveChatroom(
            @Parameter(description = "ID of the chatroom to leave", required = true)
            @PathVariable String chatroomId) {
        log.info("🏠 LEAVE CHATROOM ENDPOINT HIT! chatroomId={}", chatroomId);
        
        return currentUserService.getCurrentUser()
                .flatMap(user -> chatroomService.leaveChatroom(user.getId(), chatroomId))
                .doOnSuccess(v -> log.info("🏠 Successfully left chatroom: {}", chatroomId));
    }

    @GetMapping("/global")
    @Operation(summary = "Get global chatrooms", description = "Retrieves all public/global chatrooms available to join")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Global chatrooms retrieved successfully",
                content = @Content(mediaType = "application/json", schema = @Schema(type = "array", implementation = ChatroomDto.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized",
                content = @Content)
    })
    public Mono<List<ChatroomDto>> getAllGlobalChatrooms(
            @Parameter(description = "Maximum number of chatrooms to return", example = "100")
            @RequestParam(defaultValue = "100") int limit) {
        log.info("🏠 GET ALL GLOBAL CHATROOMS ENDPOINT HIT! limit={}", limit);
        
        return chatroomService.getAllGlobalChatrooms(limit)
                .collectList()
                .doOnSuccess(chatrooms -> log.info("🏠 Successfully retrieved {} global chatrooms", chatrooms.size()));
    }

    @GetMapping("/search")
    @Operation(summary = "Search chatrooms", description = "Search for chatrooms by name or description")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Search results retrieved successfully",
                content = @Content(mediaType = "application/json", schema = @Schema(type = "array", implementation = ChatroomDto.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized",
                content = @Content)
    })
    public Mono<List<ChatroomDto>> searchChatrooms(
            @Parameter(description = "Search query", required = true)
            @RequestParam String query) {
        log.info("🏠 SEARCH CHATROOMS ENDPOINT HIT! query={}", query);
        
        return chatroomService.searchChatrooms(query)
                .collectList()
                .doOnSuccess(chatrooms -> log.info("🏠 Successfully found {} chatrooms", chatrooms.size()));
    }
}