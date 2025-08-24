package io.shrouded.okara.controller;

import io.shrouded.okara.dto.message.MessageDto;
import io.shrouded.okara.dto.message.SendMessageRequest;
import io.shrouded.okara.service.CurrentUserService;
import io.shrouded.okara.service.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

import java.util.List;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Messages", description = "End-to-end encrypted direct messaging endpoints")
@SecurityRequirement(name = "bearerAuth")
public class MessageController {

    private final MessageService messageService;
    private final CurrentUserService currentUserService;

    @PostMapping("/send")
    @Operation(summary = "Send encrypted message", description = "Sends an end-to-end encrypted direct message to another user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Message sent successfully",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = MessageDto.class))),
        @ApiResponse(responseCode = "400", description = "Invalid message data",
                content = @Content),
        @ApiResponse(responseCode = "404", description = "Recipient not found",
                content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized",
                content = @Content)
    })
    public Mono<MessageDto> sendMessage(
            @Parameter(description = "Encrypted message request", required = true)
            @RequestBody SendMessageRequest request) {
        return currentUserService.getCurrentUser()
                .flatMap(currentUser ->
                        messageService.sendMessage(
                                currentUser.getId(),
                                request.receiverId(),
                                request.encryptedContent(),
                                request.signalPreKeyId(),
                                request.signalSessionId(),
                                request.signalMessage()
                        ).map(MessageDto::fromMessage)
                );
    }

    @GetMapping("/conversation/{userId}")
    @Operation(summary = "Get conversation", description = "Retrieves encrypted messages from a conversation with another user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Conversation retrieved successfully",
                content = @Content(mediaType = "application/json", schema = @Schema(type = "array", implementation = MessageDto.class))),
        @ApiResponse(responseCode = "404", description = "User not found",
                content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized",
                content = @Content)
    })
    public Mono<List<MessageDto>> getConversation(
            @Parameter(description = "ID of the other user in the conversation", required = true)
            @PathVariable String userId,
            @Parameter(description = "Maximum number of messages to return", example = "20")
            @RequestParam(defaultValue = "20") int limit,
            @Parameter(description = "ID of the last message for pagination")
            @RequestParam(required = false) String lastMessageId) {

        return currentUserService.getCurrentUser()
                .flatMap(currentUser ->
                        messageService.getConversation(currentUser.getId(), userId, limit, lastMessageId)
                                .map(MessageDto::fromMessage)
                                .collectList()
                );
    }

    @PutMapping("/{messageId}/delivered")
    @Operation(summary = "Mark message as delivered", description = "Updates message status to delivered")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Message marked as delivered",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = MessageDto.class))),
        @ApiResponse(responseCode = "404", description = "Message not found",
                content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized",
                content = @Content)
    })
    public Mono<MessageDto> markAsDelivered(
            @Parameter(description = "ID of the message", required = true)
            @PathVariable String messageId) {
        return currentUserService.getCurrentUser()
                .flatMap(currentUser ->
                        messageService.markAsDelivered(messageId, currentUser.getId())
                                .map(MessageDto::fromMessage)
                );
    }

    @PutMapping("/{messageId}/read")
    @Operation(summary = "Mark message as read", description = "Updates message status to read")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Message marked as read",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = MessageDto.class))),
        @ApiResponse(responseCode = "404", description = "Message not found",
                content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized",
                content = @Content)
    })
    public Mono<MessageDto> markAsRead(
            @Parameter(description = "ID of the message", required = true)
            @PathVariable String messageId) {
        return currentUserService.getCurrentUser()
                .flatMap(currentUser ->
                        messageService.markAsRead(messageId, currentUser.getId())
                                .map(MessageDto::fromMessage)
                );
    }

    @GetMapping("/conversations")
    @Operation(summary = "Get recent conversations", description = "Retrieves list of recent conversation user IDs")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Recent conversations retrieved successfully",
                content = @Content(mediaType = "application/json", schema = @Schema(type = "array", implementation = String.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized",
                content = @Content)
    })
    public Mono<List<String>> getRecentConversations(
            @Parameter(description = "Maximum number of conversations to return", example = "20")
            @RequestParam(defaultValue = "20") int limit) {
        
        return currentUserService.getCurrentUser()
                .flatMap(currentUser ->
                        messageService.getRecentConversations(currentUser.getId(), limit)
                                .collectList()
                );
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Get unread message count", description = "Returns the total number of unread messages for the user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Unread count retrieved successfully",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = Long.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized",
                content = @Content)
    })
    public Mono<Long> getUnreadCount() {
        return currentUserService.getCurrentUser()
                .flatMap(currentUser ->
                        messageService.getUnreadCount(currentUser.getId())
                );
    }
}