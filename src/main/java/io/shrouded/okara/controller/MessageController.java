package io.shrouded.okara.controller;

import io.shrouded.okara.dto.message.MessageDto;
import io.shrouded.okara.dto.message.SendMessageRequest;
import io.shrouded.okara.service.CurrentUserService;
import io.shrouded.okara.service.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
@Slf4j
public class MessageController {

    private final MessageService messageService;
    private final CurrentUserService currentUserService;

    @PostMapping("/send")
    public Mono<ResponseEntity<MessageDto>> sendMessage(@RequestBody SendMessageRequest request) {
        return currentUserService.getCurrentUser()
                .flatMap(currentUser ->
                        messageService.sendMessage(
                                currentUser.getId(),
                                request.receiverId(),
                                request.encryptedContent(),
                                request.signalPreKeyId(),
                                request.signalSessionId(),
                                request.signalMessage()
                        ).map(message -> ResponseEntity.ok(MessageDto.fromMessage(message)))
                );
    }

    @GetMapping("/conversation/{userId}")
    public Mono<ResponseEntity<List<MessageDto>>> getConversation(
            @PathVariable String userId,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) String lastMessageId) {

        return currentUserService.getCurrentUser()
                .flatMap(currentUser ->
                        messageService.getConversation(currentUser.getId(), userId, limit, lastMessageId)
                                .map(MessageDto::fromMessage)
                                .collectList()
                                .map(ResponseEntity::ok)
                );
    }

    @PutMapping("/{messageId}/delivered")
    public Mono<ResponseEntity<MessageDto>> markAsDelivered(@PathVariable String messageId) {
        return currentUserService.getCurrentUser()
                .flatMap(currentUser ->
                        messageService.markAsDelivered(messageId, currentUser.getId())
                                .map(message -> ResponseEntity.ok(MessageDto.fromMessage(message)))
                );
    }

    @PutMapping("/{messageId}/read")
    public Mono<ResponseEntity<MessageDto>> markAsRead(@PathVariable String messageId) {
        return currentUserService.getCurrentUser()
                .flatMap(currentUser ->
                        messageService.markAsRead(messageId, currentUser.getId())
                                .map(message -> ResponseEntity.ok(MessageDto.fromMessage(message)))
                );
    }

    @GetMapping("/conversations")
    public Mono<ResponseEntity<List<String>>> getRecentConversations(
            @RequestParam(defaultValue = "20") int limit) {
        
        return currentUserService.getCurrentUser()
                .flatMap(currentUser ->
                        messageService.getRecentConversations(currentUser.getId(), limit)
                                .collectList()
                                .map(ResponseEntity::ok)
                );
    }

    @GetMapping("/unread-count")
    public Mono<ResponseEntity<Long>> getUnreadCount() {
        return currentUserService.getCurrentUser()
                .flatMap(currentUser ->
                        messageService.getUnreadCount(currentUser.getId())
                                .map(count -> ResponseEntity.ok(count))
                );
    }
}