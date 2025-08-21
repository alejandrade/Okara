package io.shrouded.okara.service;

import com.google.cloud.Timestamp;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.spring.data.firestore.FirestoreReactiveOperations;
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

    public Mono<User> getOrCreateUser(String jwtToken, String fcmToken) {
        return Mono.fromCallable(() -> firebaseAuthService.verifyToken(jwtToken))
                   .flatMap(decodedToken -> {
                       String firebaseUid = decodedToken.getUid();
                       String picture = (String) decodedToken.getClaims().get("picture");

                       // Check if user exists by email
                       return userRepository.findByFirebaseUid(firebaseUid)
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
                                                newUser.setFirebaseUid(firebaseUid);
                                                newUser.setUsername("anon");
                                                newUser.setProfileImageUrl(picture);
                                                newUser.setFcmToken(fcmToken);
                                                newUser.setCreatedAt(Timestamp.now());
                                                newUser.setUpdatedAt(Timestamp.now());

                                                return userRepository.save(newUser)
                                                                     .publishOn(Schedulers.boundedElastic())
                                                                     .doOnSuccess(savedUser -> {
                                                                         log.info(
                                                                                 "New user created: {} with firebaseUid: {}",
                                                                                 firebaseUid);

                                                                         // Initialize personal feed for new user
                                                                         personalFeedService.initializeUserFeed(
                                                                                                    savedUser.getId())
                                                                                            .onErrorResume(e -> {
                                                                                                log.error(
                                                                                                        "Failed to initialize feeds for new user {}: {}",
                                                                                                        savedUser.getId(),
                                                                                                        e.getMessage());
                                                                                                return Mono.empty();
                                                                                            })
                                                                                            .subscribe();
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

    public Mono<User> findByFirebaseUid(String firebaseUid) {
        return userRepository.findByFirebaseUid(firebaseUid);
    }

    public Mono<User> saveUser(User newUser) {
        return userRepository.save(newUser);
    }

    public Mono<User> followUser(String followerId, String followeeId) {
        return Mono.zip(
                           userRepository.findById(followerId)
                                         .switchIfEmpty(Mono.error(new RuntimeException("Follower not found"))),
                           userRepository.findById(followeeId)
                                         .switchIfEmpty(Mono.error(new RuntimeException("User to follow not found")))
                   )
                   .flatMap(tuple -> {
                       User follower = tuple.getT1();
                       User followee = tuple.getT2();

                       // Add to following/followers lists
                       if (!follower.getFollowing().contains(followeeId)) {
                           follower.getFollowing().add(followeeId);
                           follower.setFollowingCount(follower.getFollowingCount() + 1);
                           follower.setUpdatedAt(Timestamp.now());

                           followee.getFollowers().add(followerId);
                           followee.setFollowersCount(followee.getFollowersCount() + 1);
                           followee.setUpdatedAt(Timestamp.now());

                           return Mono.zip(
                                              userRepository.save(follower),
                                              userRepository.save(followee)
                                      )
                                      .flatMap(savedUsers -> {
                                          // Publish follow event for feed fanout
                                          try {
                                              feedEventPublisher.publishUserFollowed(followerId, followeeId);
                                          } catch (Exception e) {
                                              log.error("Failed to publish user followed event: {}", e.getMessage());
                                          }
                                          return Mono.just(savedUsers.getT1());
                                      });
                       }

                       return Mono.just(follower);
                   });
    }

    public Mono<User> unfollowUser(String followerId, String followeeId) {
        return Mono.zip(
                           userRepository.findById(followerId)
                                         .switchIfEmpty(Mono.error(new RuntimeException("Follower not found"))),
                           userRepository.findById(followeeId)
                                         .switchIfEmpty(Mono.error(new RuntimeException("User to unfollow not found")))
                   )
                   .flatMap(tuple -> {
                       User follower = tuple.getT1();
                       User followee = tuple.getT2();

                       // Remove from following/followers lists
                       if (follower.getFollowing().contains(followeeId)) {
                           follower.getFollowing().remove(followeeId);
                           follower.setFollowingCount(follower.getFollowingCount() - 1);
                           follower.setUpdatedAt(Timestamp.now());

                           followee.getFollowers().remove(followerId);
                           followee.setFollowersCount(followee.getFollowersCount() - 1);
                           followee.setUpdatedAt(Timestamp.now());

                           return Mono.zip(
                                              userRepository.save(follower),
                                              userRepository.save(followee)
                                      )
                                      .flatMap(savedUsers -> {
                                          // Publish unfollow event for feed cleanup
                                          try {
                                              feedEventPublisher.publishUserUnfollowed(followerId, followeeId);
                                          } catch (Exception e) {
                                              log.error("Failed to publish user unfollowed event: {}", e.getMessage());
                                          }
                                          return Mono.just(savedUsers.getT1());
                                      });
                       }

                       return Mono.just(follower);
                   });
    }

    public Mono<User> updateProfile(String userId, String displayName, String bio, String location, String website) {
        return userRepository.findById(userId)
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


}