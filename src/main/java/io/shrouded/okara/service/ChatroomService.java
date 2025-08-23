package io.shrouded.okara.service;

import com.google.cloud.Timestamp;
import io.shrouded.okara.dto.chatroom.ChatroomDto;
import io.shrouded.okara.dto.chatroom.ChatroomListResponse;
import io.shrouded.okara.dto.chatroom.CreateChatroomRequest;
import io.shrouded.okara.exception.OkaraException;
import io.shrouded.okara.model.Chatroom;
import io.shrouded.okara.model.User;
import io.shrouded.okara.model.UserChatroom;
import io.shrouded.okara.repository.ChatroomRepository;
import io.shrouded.okara.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatroomService {

    private final ChatroomRepository chatroomRepository;
    private final UserRepository userRepository;
    
    private static final String DEFAULT_CHATROOM_NAME = "General Chat";

    public Mono<ChatroomListResponse> getUserChatrooms(String firebaseUid, int limit, String cursor) {
        return userRepository.findById(firebaseUid)
                .switchIfEmpty(Mono.error(OkaraException.notFound("user")))
                .flatMap(user -> {
                    List<UserChatroom> userChatrooms = user.getChatrooms();
                    if (userChatrooms == null || userChatrooms.isEmpty()) {
                        return Mono.just(ChatroomListResponse.of(new ArrayList<>(), false, null, 0));
                    }

                    // Sort by last activity (most recent first)
                    userChatrooms.sort((a, b) -> {
                        if (a.getLastReadAt() == null && b.getLastReadAt() == null) return 0;
                        if (a.getLastReadAt() == null) return 1;
                        if (b.getLastReadAt() == null) return -1;
                        return b.getLastReadAt().compareTo(a.getLastReadAt());
                    });

                    // Apply cursor-based pagination
                    int startIndex = 0;
                    if (cursor != null && !cursor.isEmpty()) {
                        for (int i = 0; i < userChatrooms.size(); i++) {
                            if (userChatrooms.get(i).getChatroomId().equals(cursor)) {
                                startIndex = i + 1;
                                break;
                            }
                        }
                    }

                    int endIndex = Math.min(startIndex + limit, userChatrooms.size());
                    List<UserChatroom> paginatedChatrooms = userChatrooms.subList(startIndex, endIndex);
                    
                    if (paginatedChatrooms.isEmpty()) {
                        return Mono.just(ChatroomListResponse.of(new ArrayList<>(), false, null, userChatrooms.size()));
                    }

                    // Get chatroom IDs to fetch
                    List<String> chatroomIds = paginatedChatrooms.stream()
                            .map(UserChatroom::getChatroomId)
                            .collect(Collectors.toList());

                    // Create map of userChatroom by chatroomId for quick lookup
                    Map<String, UserChatroom> userChatroomMap = paginatedChatrooms.stream()
                            .collect(Collectors.toMap(UserChatroom::getChatroomId, uc -> uc));

                    // Fetch chatrooms from global collection
                    return Flux.fromIterable(chatroomIds)
                            .flatMap(chatroomRepository::findById)
                            .collectList()
                            .map(chatrooms -> {
                                List<ChatroomDto> chatroomDtos = chatrooms.stream()
                                        .map(chatroom -> ChatroomDto.fromChatroom(chatroom, userChatroomMap.get(chatroom.getId())))
                                        .collect(Collectors.toList());

                                boolean hasMore = endIndex < userChatrooms.size();
                                String nextCursor = hasMore && !paginatedChatrooms.isEmpty() 
                                        ? paginatedChatrooms.get(paginatedChatrooms.size() - 1).getChatroomId()
                                        : null;

                                return ChatroomListResponse.of(chatroomDtos, hasMore, nextCursor, userChatrooms.size());
                            });
                });
    }

    public Mono<ChatroomDto> createChatroom(String creatorFirebaseUid, CreateChatroomRequest request) {
        return userRepository.findById(creatorFirebaseUid)
                .switchIfEmpty(Mono.error(OkaraException.notFound("user")))
                .flatMap(creator -> {
                    Chatroom chatroom = new Chatroom();
                    chatroom.setName(request.name());
                    chatroom.setDescription(request.description());
                    chatroom.setImageUrl(request.imageUrl());
                    chatroom.setType(Chatroom.ChatroomType.valueOf(request.type().toUpperCase()));
                    chatroom.setCreatedBy(creatorFirebaseUid);
                    chatroom.setCreatedAt(Timestamp.now());
                    chatroom.setLastActivity(Timestamp.now());
                    chatroom.setParticipants(new ArrayList<>());
                    chatroom.getParticipants().add(creatorFirebaseUid);
                    
                    if (request.initialParticipants() != null) {
                        request.initialParticipants().forEach(participantId -> {
                            if (!chatroom.getParticipants().contains(participantId)) {
                                chatroom.getParticipants().add(participantId);
                            }
                        });
                    }
                    
                    chatroom.setParticipantCount(chatroom.getParticipants().size());

                    return chatroomRepository.save(chatroom)
                            .flatMap(savedChatroom -> {
                                // Add chatroom to all participants' user collections
                                return addChatroomToUsers(savedChatroom.getParticipants(), savedChatroom.getId())
                                        .then(Mono.just(ChatroomDto.fromChatroom(savedChatroom, null)));
                            });
                });
    }

    public Mono<ChatroomDto> joinChatroom(String firebaseUid, String chatroomId) {
        return Mono.zip(
                userRepository.findById(firebaseUid)
                        .switchIfEmpty(Mono.error(OkaraException.notFound("user"))),
                chatroomRepository.findById(chatroomId)
                        .switchIfEmpty(Mono.error(OkaraException.notFound("chatroom")))
        ).flatMap(tuple -> {
            User user = tuple.getT1();
            Chatroom chatroom = tuple.getT2();

            // Check if user is already in chatroom
            boolean alreadyInChatroom = user.getChatrooms().stream()
                    .anyMatch(uc -> uc.getChatroomId().equals(chatroomId));

            if (alreadyInChatroom) {
                return Mono.just(ChatroomDto.fromChatroom(chatroom, 
                        user.getChatrooms().stream()
                                .filter(uc -> uc.getChatroomId().equals(chatroomId))
                                .findFirst()
                                .orElse(null)));
            }

            // Add user to chatroom participants
            if (!chatroom.getParticipants().contains(firebaseUid)) {
                chatroom.getParticipants().add(firebaseUid);
                chatroom.setParticipantCount(chatroom.getParticipants().size());
                chatroom.setLastActivity(Timestamp.now());
            }

            // Add chatroom to user's collection
            UserChatroom userChatroom = new UserChatroom(chatroomId, Timestamp.now());
            user.getChatrooms().add(userChatroom);
            user.setUpdatedAt(Timestamp.now());

            return Mono.zip(
                    chatroomRepository.save(chatroom),
                    userRepository.save(user)
            ).map(saved -> ChatroomDto.fromChatroom(saved.getT1(), userChatroom));
        });
    }

    public Mono<Void> leaveChatroom(String firebaseUid, String chatroomId) {
        return Mono.zip(
                userRepository.findById(firebaseUid)
                        .switchIfEmpty(Mono.error(OkaraException.notFound("user"))),
                chatroomRepository.findById(chatroomId)
                        .switchIfEmpty(Mono.error(OkaraException.notFound("chatroom")))
        ).flatMap(tuple -> {
            User user = tuple.getT1();
            Chatroom chatroom = tuple.getT2();

            // Remove chatroom from user's collection
            user.getChatrooms().removeIf(uc -> uc.getChatroomId().equals(chatroomId));
            user.setUpdatedAt(Timestamp.now());

            // Remove user from chatroom participants
            chatroom.getParticipants().remove(firebaseUid);
            chatroom.setParticipantCount(chatroom.getParticipants().size());
            chatroom.setLastActivity(Timestamp.now());

            return Mono.zip(
                    userRepository.save(user),
                    chatroomRepository.save(chatroom)
            ).then();
        });
    }

    public Flux<ChatroomDto> getAllGlobalChatrooms(int limit) {
        return chatroomRepository.findByType(Chatroom.ChatroomType.PUBLIC)
                .filter(Chatroom::isActive)
                .sort((a, b) -> {
                    // Sort by participant count (most active first)
                    int countCompare = Integer.compare(b.getParticipantCount(), a.getParticipantCount());
                    if (countCompare != 0) return countCompare;
                    
                    // Then by creation date (newest first)
                    if (a.getCreatedAt() == null && b.getCreatedAt() == null) return 0;
                    if (a.getCreatedAt() == null) return 1;
                    if (b.getCreatedAt() == null) return -1;
                    return b.getCreatedAt().compareTo(a.getCreatedAt());
                })
                .take(limit)
                .map(chatroom -> ChatroomDto.fromChatroom(chatroom, null));
    }

    public Flux<ChatroomDto> searchChatrooms(String query) {
        return chatroomRepository.searchChatroomsByName(query)
                .filter(chatroom -> chatroom.getType() == Chatroom.ChatroomType.PUBLIC)
                .filter(Chatroom::isActive)
                .map(chatroom -> ChatroomDto.fromChatroom(chatroom, null));
    }

    public Mono<ChatroomDto> getChatroomById(String chatroomId) {
        return chatroomRepository.findById(chatroomId)
                .switchIfEmpty(Mono.error(OkaraException.notFound("chatroom")))
                .map(chatroom -> ChatroomDto.fromChatroom(chatroom, null));
    }

    public Mono<Void> validateChatroomsExist(List<String> chatroomIds) {
        log.debug("Batch validating existence of {} chatrooms", chatroomIds.size());
        
        return chatroomRepository.findByIdsIn(chatroomIds)
                                 .collectList()
                                 .flatMap(foundChatrooms -> {
                                     if (foundChatrooms.size() != chatroomIds.size()) {
                                         log.warn("Expected {} chatrooms, found {}", chatroomIds.size(), foundChatrooms.size());
                                         return Mono.error(OkaraException.badRequest("One or more chatrooms do not exist"));
                                     }
                                     log.debug("All {} chatrooms validated successfully", chatroomIds.size());
                                     return Mono.empty();
                                 });
    }

    private Mono<Void> addChatroomToUsers(List<String> participantIds, String chatroomId) {
        return Flux.fromIterable(participantIds)
                .flatMap(participantId -> 
                    userRepository.findById(participantId)
                            .flatMap(user -> {
                                // Check if user already has this chatroom
                                boolean alreadyHasChatroom = user.getChatrooms().stream()
                                        .anyMatch(uc -> uc.getChatroomId().equals(chatroomId));
                                
                                if (!alreadyHasChatroom) {
                                    UserChatroom userChatroom = new UserChatroom(chatroomId, Timestamp.now());
                                    user.getChatrooms().add(userChatroom);
                                    user.setUpdatedAt(Timestamp.now());
                                    return userRepository.save(user);
                                }
                                return Mono.just(user);
                            })
                            .onErrorResume(e -> {
                                log.warn("Failed to add chatroom to user {}: {}", participantId, e.getMessage());
                                return Mono.empty();
                            })
                )
                .then();
    }

    public Mono<Void> addUserToDefaultChatrooms(String firebaseUid) {
        log.info("🏠 Adding user {} to default chatrooms", firebaseUid);
        
        // Add user to both general chat and their personal chatroom
        Mono<Void> addToGeneralChat = addUserToGeneralChatroom(firebaseUid);
        Mono<Void> createPersonalChatroom = createUserPersonalChatroom(firebaseUid);
        
        return Mono.when(addToGeneralChat, createPersonalChatroom)
                .doOnSuccess(v -> log.info("🏠 Successfully added user {} to default chatrooms", firebaseUid))
                .onErrorResume(e -> {
                    log.error("🏠 Failed to add user {} to default chatrooms: {}", firebaseUid, e.getMessage(), e);
                    return Mono.empty(); // Don't fail user creation if default chatroom fails
                });
    }

    private Mono<Void> addUserToGeneralChatroom(String firebaseUid) {
        return getOrCreateDefaultChatroom()
                .flatMap(defaultChatroom -> {
                    return userRepository.findById(firebaseUid)
                            .switchIfEmpty(Mono.error(OkaraException.notFound("user")))
                            .flatMap(user -> {
                                // Check if user is already in the default chatroom
                                boolean alreadyInChatroom = user.getChatrooms().stream()
                                        .anyMatch(uc -> uc.getChatroomId().equals(defaultChatroom.getId()));

                                if (alreadyInChatroom) {
                                    log.debug("🏠 User {} already in default chatroom", firebaseUid);
                                    return Mono.<Void>empty();
                                }

                                // Add user to default chatroom participants
                                if (!defaultChatroom.getParticipants().contains(firebaseUid)) {
                                    defaultChatroom.getParticipants().add(firebaseUid);
                                    defaultChatroom.setParticipantCount(defaultChatroom.getParticipants().size());
                                    defaultChatroom.setLastActivity(Timestamp.now());
                                }

                                // Add chatroom to user's collection
                                UserChatroom userChatroom = new UserChatroom(defaultChatroom.getId(), Timestamp.now());
                                user.getChatrooms().add(userChatroom);
                                user.setUpdatedAt(Timestamp.now());

                                return Mono.zip(
                                        chatroomRepository.save(defaultChatroom),
                                        userRepository.save(user)
                                ).then();
                            });
                });
    }

    private Mono<Void> createUserPersonalChatroom(String firebaseUid) {
        log.info("👤 Creating personal chatroom for user {}", firebaseUid);
        
        return userRepository.findById(firebaseUid)
                .switchIfEmpty(Mono.error(OkaraException.notFound("user")))
                .flatMap(user -> {
                    // Create personal chatroom
                    Chatroom personalChatroom = new Chatroom();
                    personalChatroom.setName("My chatroom");
                    personalChatroom.setDescription("Your personal space where your followers can see your content");
                    personalChatroom.setType(Chatroom.ChatroomType.PUBLIC); // Public so others can follow
                    personalChatroom.setCreatedBy(firebaseUid);
                    personalChatroom.setCreatedAt(Timestamp.now());
                    personalChatroom.setLastActivity(Timestamp.now());
                    personalChatroom.setParticipants(new ArrayList<>());
                    personalChatroom.getParticipants().add(firebaseUid); // User is the first participant
                    personalChatroom.setParticipantCount(1);

                    return chatroomRepository.save(personalChatroom)
                            .flatMap(savedChatroom -> {
                                // Add personal chatroom to user's collection
                                UserChatroom userChatroom = new UserChatroom(savedChatroom.getId(), Timestamp.now());
                                user.getChatrooms().add(userChatroom);
                                user.setUpdatedAt(Timestamp.now());

                                return userRepository.save(user).then();
                            })
                            .doOnSuccess(v -> log.info("👤 Successfully created personal chatroom for user {}", firebaseUid));
                });
    }

    private Mono<Chatroom> getOrCreateDefaultChatroom() {
        return chatroomRepository.findByNameIgnoreCase(DEFAULT_CHATROOM_NAME)
                .next()
                .switchIfEmpty(createDefaultChatroom());
    }

    private Mono<Chatroom> createDefaultChatroom() {
        log.info("🏠 Creating default chatroom: {}", DEFAULT_CHATROOM_NAME);
        
        Chatroom defaultChatroom = new Chatroom();
        defaultChatroom.setName(DEFAULT_CHATROOM_NAME);
        defaultChatroom.setDescription("Welcome to the general chat! Connect with everyone here.");
        defaultChatroom.setType(Chatroom.ChatroomType.PUBLIC);
        defaultChatroom.setCreatedBy("system"); // System-created chatroom
        defaultChatroom.setCreatedAt(Timestamp.now());
        defaultChatroom.setLastActivity(Timestamp.now());
        defaultChatroom.setParticipants(new ArrayList<>());
        defaultChatroom.setParticipantCount(0);

        return chatroomRepository.save(defaultChatroom)
                .doOnSuccess(chatroom -> log.info("🏠 Successfully created default chatroom: {}", chatroom.getName()));
    }

}