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

import java.util.List;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
@Slf4j
public class MessageController {

    private final MessageService messageService;
    private final CurrentUserService currentUserService;

    @PostMapping("/send")
    public Mono<MessageDto> sendMessage(@RequestBody SendMessageRequest request) {
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
    public Mono<List<MessageDto>> getConversation(
            @PathVariable String userId,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) String lastMessageId) {

        return currentUserService.getCurrentUser()
                .flatMap(currentUser ->
                        messageService.getConversation(currentUser.getId(), userId, limit, lastMessageId)
                                .map(MessageDto::fromMessage)
                                .collectList()
                );
    }

    @PutMapping("/{messageId}/delivered")
    public Mono<MessageDto> markAsDelivered(@PathVariable String messageId) {
        return currentUserService.getCurrentUser()
                .flatMap(currentUser ->
                        messageService.markAsDelivered(messageId, currentUser.getId())
                                .map(MessageDto::fromMessage)
                );
    }

    @PutMapping("/{messageId}/read")
    public Mono<MessageDto> markAsRead(@PathVariable String messageId) {
        return currentUserService.getCurrentUser()
                .flatMap(currentUser ->
                        messageService.markAsRead(messageId, currentUser.getId())
                                .map(MessageDto::fromMessage)
                );
    }

    @GetMapping("/conversations")
    public Mono<List<String>> getRecentConversations(
            @RequestParam(defaultValue = "20") int limit) {
        
        return currentUserService.getCurrentUser()
                .flatMap(currentUser ->
                        messageService.getRecentConversations(currentUser.getId(), limit)
                                .collectList()
                );
    }

    @GetMapping("/unread-count")
    public Mono<Long> getUnreadCount() {
        return currentUserService.getCurrentUser()
                .flatMap(currentUser ->
                        messageService.getUnreadCount(currentUser.getId())
                );
    }
}