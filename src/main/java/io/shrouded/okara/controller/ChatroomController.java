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

import java.util.List;

@RestController
@RequestMapping("/api/chatrooms")
@RequiredArgsConstructor
@Slf4j
public class ChatroomController {

    private final ChatroomService chatroomService;
    private final CurrentUserService currentUserService;

    @GetMapping
    public Mono<ChatroomListResponse> getChatrooms(
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) String cursor) {
        
        log.info("🏠 GET CHATROOMS ENDPOINT HIT! limit={}, cursor={}", limit, cursor);
        
        return currentUserService.getCurrentUser()
                .flatMap(user -> chatroomService.getUserChatrooms(user.getId(), limit, cursor))
                .doOnSuccess(response -> log.info("🏠 Successfully retrieved {} chatrooms", 
                        response.chatrooms().size()));
    }

    @PostMapping
    public Mono<ChatroomDto> createChatroom(@RequestBody CreateChatroomRequest request) {
        log.info("🏠 CREATE CHATROOM ENDPOINT HIT! name={}, type={}", request.name(), request.type());
        
        return currentUserService.getCurrentUser()
                .flatMap(user -> chatroomService.createChatroom(user.getId(), request))
                .doOnSuccess(chatroom -> log.info("🏠 Successfully created chatroom: {}", chatroom.name()));
    }

    @GetMapping("/{chatroomId}")
    public Mono<ChatroomDto> getChatroomById(@PathVariable String chatroomId) {
        log.info("🏠 GET CHATROOM BY ID ENDPOINT HIT! chatroomId={}", chatroomId);
        
        return chatroomService.getChatroomById(chatroomId)
                .doOnSuccess(chatroom -> log.info("🏠 Successfully retrieved chatroom: {}", chatroom.name()));
    }

    @PostMapping("/{chatroomId}/join")
    public Mono<ChatroomDto> joinChatroom(@PathVariable String chatroomId) {
        log.info("🏠 JOIN CHATROOM ENDPOINT HIT! chatroomId={}", chatroomId);
        
        return currentUserService.getCurrentUser()
                .flatMap(user -> chatroomService.joinChatroom(user.getId(), chatroomId))
                .doOnSuccess(chatroom -> log.info("🏠 Successfully joined chatroom: {}", chatroom.name()));
    }

    @PostMapping("/{chatroomId}/leave")
    public Mono<Void> leaveChatroom(@PathVariable String chatroomId) {
        log.info("🏠 LEAVE CHATROOM ENDPOINT HIT! chatroomId={}", chatroomId);
        
        return currentUserService.getCurrentUser()
                .flatMap(user -> chatroomService.leaveChatroom(user.getId(), chatroomId))
                .doOnSuccess(v -> log.info("🏠 Successfully left chatroom: {}", chatroomId));
    }

    @GetMapping("/global")
    public Mono<List<ChatroomDto>> getAllGlobalChatrooms(
            @RequestParam(defaultValue = "100") int limit) {
        log.info("🏠 GET ALL GLOBAL CHATROOMS ENDPOINT HIT! limit={}", limit);
        
        return chatroomService.getAllGlobalChatrooms(limit)
                .collectList()
                .doOnSuccess(chatrooms -> log.info("🏠 Successfully retrieved {} global chatrooms", chatrooms.size()));
    }

    @GetMapping("/search")
    public Mono<List<ChatroomDto>> searchChatrooms(@RequestParam String query) {
        log.info("🏠 SEARCH CHATROOMS ENDPOINT HIT! query={}", query);
        
        return chatroomService.searchChatrooms(query)
                .collectList()
                .doOnSuccess(chatrooms -> log.info("🏠 Successfully found {} chatrooms", chatrooms.size()));
    }
}