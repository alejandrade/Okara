package io.shrouded.okara.service;

import io.shrouded.okara.exception.OkaraException;
import io.shrouded.okara.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserDeleteService {

    private final UserRepository userRepository;
    private final FeedRepository feedRepository;
    private final UserFeedRepository userFeedRepository;
    private final ViewEventRepository viewEventRepository;
    private final MessageRepository messageRepository;
    private final ChatroomRepository chatroomRepository;
    private final FirebaseAuthService firebaseAuthService;

    public Mono<Void> deleteAllUserData(String firebaseUid) {
        log.info("🗑️ Starting complete data deletion for user: {}", firebaseUid);
        
        return userRepository.findById(firebaseUid)
                .switchIfEmpty(Mono.error(OkaraException.notFound("user")))
                .flatMap(user -> {
                    log.info("🗑️ User found, proceeding with data deletion for: {}", firebaseUid);
                    
                    return Mono.when(
                            // Delete user's posts and comments
                            deleteUserFeedData(firebaseUid),
                            
                            // Delete user's personal feed entries
                            deleteUserPersonalFeed(firebaseUid),
                            
                            // Delete user's view events
                            deleteUserViewEvents(firebaseUid),
                            
                            // Delete user's messages
                            deleteUserMessages(firebaseUid),
                            
                            // Remove user from chatrooms and clean up
                            removeUserFromChatrooms(firebaseUid)
                    )
                    .then(deleteUserRecord(firebaseUid))
                    .then(deleteFirebaseUser(firebaseUid))
                    .doOnSuccess(v -> log.info("🗑️ Successfully deleted all data and Firebase user: {}", firebaseUid))
                    .onErrorResume(e -> {
                        log.error("🗑️ Failed to delete user data for {}: {}", firebaseUid, e.getMessage(), e);
                        return Mono.error(OkaraException.internalError("Failed to delete user data"));
                    });
                });
    }

    private Mono<Void> deleteUserFeedData(String firebaseUid) {
        log.debug("🗑️ Deleting feed data for user: {}", firebaseUid);
        return feedRepository.findByAuthorIdAndParentIdIsNull(firebaseUid)
                .flatMap(feedRepository::delete)
                .then()
                .doOnSuccess(v -> log.debug("🗑️ Feed data deleted for user: {}", firebaseUid));
    }

    private Mono<Void> deleteUserPersonalFeed(String firebaseUid) {
        log.debug("🗑️ Deleting personal feed for user: {}", firebaseUid);
        return userFeedRepository.findByUserId(firebaseUid)
                .flatMap(userFeedRepository::delete)
                .then()
                .doOnSuccess(v -> log.debug("🗑️ Personal feed deleted for user: {}", firebaseUid));
    }

    private Mono<Void> deleteUserViewEvents(String firebaseUid) {
        log.debug("🗑️ Deleting view events for user: {}", firebaseUid);
        return viewEventRepository.findByUserId(firebaseUid)
                .flatMap(viewEventRepository::delete)
                .then()
                .doOnSuccess(v -> log.debug("🗑️ View events deleted for user: {}", firebaseUid));
    }

    private Mono<Void> deleteUserMessages(String firebaseUid) {
        log.debug("🗑️ Deleting messages for user: {}", firebaseUid);
        return Mono.when(
                messageRepository.findBySenderIdOrderBySentAtDesc(firebaseUid)
                        .flatMap(messageRepository::delete),
                messageRepository.findByReceiverIdOrderBySentAtDesc(firebaseUid)
                        .flatMap(messageRepository::delete)
        ).doOnSuccess(v -> log.debug("🗑️ Messages deleted for user: {}", firebaseUid));
    }

    private Mono<Void> removeUserFromChatrooms(String firebaseUid) {
        log.debug("🗑️ Removing user from chatrooms: {}", firebaseUid);
        
        return chatroomRepository.findByParticipantsContaining(firebaseUid)
                .flatMap(chatroom -> {
                    // Remove user from participants list
                    chatroom.getParticipants().remove(firebaseUid);
                    chatroom.setParticipantCount(chatroom.getParticipants().size());
                    
                    // If chatroom becomes empty and it's not a system chatroom, we could delete it
                    // For now, we'll just update the participant count
                    return chatroomRepository.save(chatroom);
                })
                .then()
                .doOnSuccess(v -> log.debug("🗑️ User removed from chatrooms: {}", firebaseUid));
    }

    private Mono<Void> deleteUserRecord(String firebaseUid) {
        log.debug("🗑️ Deleting user record: {}", firebaseUid);
        return userRepository.findById(firebaseUid)
                .flatMap(userRepository::delete)
                .doOnSuccess(v -> log.debug("🗑️ User record deleted: {}", firebaseUid));
    }

    private Mono<Void> deleteFirebaseUser(String firebaseUid) {
        log.debug("🗑️ Deleting Firebase user: {}", firebaseUid);
        return firebaseAuthService.deleteUser(firebaseUid)
                .doOnSuccess(v -> log.debug("🗑️ Firebase user deleted: {}", firebaseUid))
                .onErrorResume(e -> {
                    log.error("🗑️ Failed to delete Firebase user {}: {}", firebaseUid, e.getMessage(), e);
                    // Don't fail the entire deletion process if Firebase deletion fails
                    // The user data is already deleted from our database
                    return Mono.empty();
                });
    }
}