package io.shrouded.okara.service;

import com.google.cloud.Timestamp;
import com.google.firebase.auth.FirebaseAuthException;
import io.shrouded.okara.model.User;
import io.shrouded.okara.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final FirebaseAuthService firebaseAuthService;
    private final FeedEventPublisher feedEventPublisher;
    private final PersonalFeedService personalFeedService;
    private final ChatroomService chatroomService;

    public Mono<User> getOrCreateUser(String jwtToken, String fcmToken) {
        return Mono.fromCallable(() -> firebaseAuthService.verifyToken(jwtToken))
                   .flatMap(decodedToken -> {
                       String firebaseUid = decodedToken.getUid();
                       String picture = (String) decodedToken.getClaims().get("picture");

                       // Check if user exists by Firebase UID
                       return userRepository.findById(firebaseUid)
                                            .flatMap(existingUser -> {
                                                // Update FCM token for existing user
                                                if (fcmToken != null && fcmToken.equals(existingUser.getFcmToken())) {
                                                    return Mono.just(existingUser) ;
                                                }

                                                existingUser.setFcmToken(fcmToken);
                                                existingUser.setUpdatedAt(Timestamp.now());
                                                return userRepository.save(existingUser);
                                            })
                                            .switchIfEmpty(Mono.defer(() -> {
                                                // Create new user

                                                User newUser = new User();
                                                newUser.setId(firebaseUid);
                                                newUser.setProfileImageUrl(picture);
                                                newUser.setFcmToken(fcmToken);
                                                newUser.setCreatedAt(Timestamp.now());
                                                newUser.setUpdatedAt(Timestamp.now());

                                                return userRepository.save(newUser)
                                                                     .publishOn(Schedulers.boundedElastic())
                                                                     .flatMap(savedUser -> {
                                                                         log.info(
                                                                                 "New user created with firebaseUid: {}",
                                                                                 firebaseUid);

                                                                         // Initialize personal feed for new user and wait for completion
                                                                         Mono<Void> feedInit = personalFeedService.initializeUserFeed(
                                                                                                    savedUser.getId())
                                                                                            .onErrorResume(e -> {
                                                                                                log.error(
                                                                                                        "Failed to initialize feeds for new user {}: {}",
                                                                                                        savedUser.getId(),
                                                                                                        e.getMessage());
                                                                                                return Mono.empty();
                                                                                            })
                                                                                            .then();

                                                                         // Add user to default chatrooms and wait for completion
                                                                         Mono<Void> chatroomSetup = chatroomService.addUserToDefaultChatrooms(
                                                                                                    savedUser.getId())
                                                                                        .then();

                                                                         // Wait for both operations to complete before returning the user
                                                                         return feedInit
                                                                                  .then(chatroomSetup)
                                                                                  .thenReturn(savedUser);
                                                                     });
                                            }));
                   })
                   .onErrorResume(FirebaseAuthException.class, e -> {
                       log.error("Firebase auth error: {}", e.getMessage());
                       return Mono.error(e);
                   });
    }

    public Mono<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public Mono<User> findById(String firebaseUid) {
        return userRepository.findById(firebaseUid);
    }

    public Mono<User> saveUser(User newUser) {
        return userRepository.save(newUser);
    }

    public Mono<User> followUser(String followerFirebaseUid, String followeeFirebaseUid) {
        return Mono.zip(
                           userRepository.findById(followerFirebaseUid)
                                         .switchIfEmpty(Mono.error(new RuntimeException("Follower not found"))),
                           userRepository.findById(followeeFirebaseUid)
                                         .switchIfEmpty(Mono.error(new RuntimeException("User to follow not found")))
                   )
                   .flatMap(tuple -> {
                       User follower = tuple.getT1();
                       User followee = tuple.getT2();

                       // Add to following/followers lists
                       if (!follower.getFollowing().contains(followeeFirebaseUid)) {
                           follower.getFollowing().add(followeeFirebaseUid);
                           follower.setFollowingCount(follower.getFollowingCount() + 1);
                           follower.setUpdatedAt(Timestamp.now());

                           followee.getFollowers().add(followerFirebaseUid);
                           followee.setFollowersCount(followee.getFollowersCount() + 1);
                           followee.setUpdatedAt(Timestamp.now());

                           return Mono.zip(
                                              userRepository.save(follower),
                                              userRepository.save(followee)
                                      )
                                      .flatMap(savedUsers -> {
                                          // Publish follow event for feed fanout
                                          try {
                                              feedEventPublisher.publishUserFollowed(followerFirebaseUid, followeeFirebaseUid);
                                          } catch (Exception e) {
                                              log.error("Failed to publish user followed event: {}", e.getMessage());
                                          }
                                          return Mono.just(savedUsers.getT1());
                                      });
                       }

                       return Mono.just(follower);
                   });
    }

    public Mono<User> unfollowUser(String followerFirebaseUid, String followeeFirebaseUid) {
        return Mono.zip(
                           userRepository.findById(followerFirebaseUid)
                                         .switchIfEmpty(Mono.error(new RuntimeException("Follower not found"))),
                           userRepository.findById(followeeFirebaseUid)
                                         .switchIfEmpty(Mono.error(new RuntimeException("User to unfollow not found")))
                   )
                   .flatMap(tuple -> {
                       User follower = tuple.getT1();
                       User followee = tuple.getT2();

                       // Remove from following/followers lists
                       if (follower.getFollowing().contains(followeeFirebaseUid)) {
                           follower.getFollowing().remove(followeeFirebaseUid);
                           follower.setFollowingCount(follower.getFollowingCount() - 1);
                           follower.setUpdatedAt(Timestamp.now());

                           followee.getFollowers().remove(followerFirebaseUid);
                           followee.setFollowersCount(followee.getFollowersCount() - 1);
                           followee.setUpdatedAt(Timestamp.now());

                           return Mono.zip(
                                              userRepository.save(follower),
                                              userRepository.save(followee)
                                      )
                                      .flatMap(savedUsers -> {
                                          // Publish unfollow event for feed cleanup
                                          try {
                                              feedEventPublisher.publishUserUnfollowed(followerFirebaseUid, followeeFirebaseUid);
                                          } catch (Exception e) {
                                              log.error("Failed to publish user unfollowed event: {}", e.getMessage());
                                          }
                                          return Mono.just(savedUsers.getT1());
                                      });
                       }

                       return Mono.just(follower);
                   });
    }

    public Mono<User> updateProfile(String firebaseUid, String displayName, String bio, String location, String website) {
        return userRepository.findById(firebaseUid)
                             .switchIfEmpty(Mono.error(new RuntimeException("User not found")))
                             .flatMap(user -> {
                                 if (displayName != null) {
                                     user.setDisplayName(displayName);
                                 }
                                 if (bio != null) {
                                     user.setBio(bio);
                                 }
                                 if (location != null) {
                                     user.setLocation(location);
                                 }
                                 if (website != null) {
                                     user.setWebsite(website);
                                 }
                                 user.setUpdatedAt(Timestamp.now());

                                 return userRepository.save(user);
                             });
    }

    public Mono<User> mergeAccounts(String anonymousUserToken, String newUserToken) {
        return Mono.fromCallable(() -> firebaseAuthService.verifyToken(anonymousUserToken))
                   .flatMap(anonymousToken -> {
                       String anonymousFirebaseUid = anonymousToken.getUid();
                       
                       return Mono.fromCallable(() -> firebaseAuthService.verifyToken(newUserToken))
                                  .flatMap(newUserTokenData -> {
                                      String newUserFirebaseUid = newUserTokenData.getUid();
                                      
                                      // Find the anonymous user
                                      return userRepository.findById(anonymousFirebaseUid)
                                                           .switchIfEmpty(Mono.error(new RuntimeException("Anonymous user not found")))
                                                           .flatMap(anonymousUser -> {
                                                               // Update the anonymous user's Firebase UID to the new one
                                                               anonymousUser.setId(newUserFirebaseUid);
                                                               // Save the updated user
                                                               return userRepository.save(anonymousUser)
                                                                                    .flatMap(mergedUser -> {
                                                                                        log.info("Successfully merged anonymous user {} with new user {}", 
                                                                                                anonymousFirebaseUid, newUserFirebaseUid);
                                                                                        
                                                                                        // Delete the anonymous user's Firebase account after successful merge
                                                                                        return firebaseAuthService.deleteUser(anonymousFirebaseUid)
                                                                                                    .doOnSuccess(v -> log.info("Successfully deleted anonymous Firebase account: {}", anonymousFirebaseUid))
                                                                                                    .onErrorResume(e -> {
                                                                                        log.warn("Failed to delete anonymous Firebase account {}: {}", anonymousFirebaseUid, e.getMessage());
                                                                                        // Don't fail the merge if Firebase deletion fails
                                                                                        return Mono.empty();
                                                                                    })
                                                                                                    .then(Mono.just(mergedUser));
                                                                                    });
                                                           });
                                  });
                   })
                   .onErrorResume(FirebaseAuthException.class, e -> {
                       log.error("Firebase auth error during account merge: {}", e.getMessage());
                       return Mono.error(e);
                   });
    }

}