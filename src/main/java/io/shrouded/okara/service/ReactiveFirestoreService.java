package io.shrouded.okara.service;

import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.WriteResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.BiConsumer;
import com.google.api.core.ApiFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReactiveFirestoreService {

    private final Firestore firestore;

    /**
     * Generic save operation for any entity
     */
    public <T> Mono<T> save(String collectionName, T entity, String id, BiConsumer<T, String> idSetter) {
        DocumentReference docRef;
        if (id == null) {
            // Create new document
            docRef = firestore.collection(collectionName).document();
            idSetter.accept(entity, docRef.getId());
        } else {
            // Update existing document
            docRef = firestore.collection(collectionName).document(id);
        }
        
        ApiFuture<WriteResult> apiFuture = docRef.set(entity);
        CompletableFuture<WriteResult> future = toCompletableFuture(apiFuture);
        
        return Mono.fromFuture(future)
                .map(writeResult -> entity)
                .onErrorResume(throwable -> {
                    log.error("Failed to save to collection " + collectionName + ": " + throwable.getMessage(), throwable);
                    return Mono.error(new RuntimeException("Failed to save to collection " + collectionName, throwable));
                });
    }

    /**
     * Generic findById operation for any entity
     */
    public <T> Mono<T> findById(String collectionName, String id,
                               Class<T> entityClass, BiConsumer<T, String> idSetter) {
        ApiFuture<DocumentSnapshot> apiFuture = firestore.collection(collectionName).document(id).get();
        CompletableFuture<DocumentSnapshot> future = toCompletableFuture(apiFuture);
        
        return Mono.fromFuture(future)
                .mapNotNull(doc -> {
                    if (doc.exists()) {
                        T entity = doc.toObject(entityClass);
                        if (entity != null) {
                            idSetter.accept(entity, doc.getId());
                        }
                        return entity;
                    }
                    return null;
                })
                .onErrorResume(throwable -> {
                    log.error("Find operation failed for document " + id + " in collection " + collectionName + ": " + throwable.getMessage(), throwable);
                    return Mono.empty();
                });
    }

    /**
     * Generic delete by id operation
     */
    public Mono<Void> deleteById(String collectionName, String id) {
        ApiFuture<WriteResult> apiFuture = firestore.collection(collectionName).document(id).delete();
        CompletableFuture<WriteResult> future = toCompletableFuture(apiFuture);
        
        return Mono.fromFuture(future)
                .then()
                .onErrorResume(throwable -> {
                    log.error("Failed to delete document by id in collection " + collectionName + ": " + throwable.getMessage(), throwable);
                    return Mono.error(new RuntimeException("Failed to delete document by id in collection " + collectionName, throwable));
                });
    }

    /**
     * Generic single field query operation
     */
    public <T> Flux<T> findByField(String collectionName, String fieldName, Object fieldValue,
                                   Class<T> entityClass, BiConsumer<T, String> idSetter) {
        return findByFieldWithLimit(collectionName, fieldName, fieldValue, 
                                   entityClass, idSetter, null);
    }

    /**
     * Generic single field query with limit
     */
    public <T> Flux<T> findByFieldWithLimit(String collectionName, String fieldName, Object fieldValue,
                                           Class<T> entityClass, BiConsumer<T, String> idSetter,
                                           Integer limit) {
        Query query = firestore.collection(collectionName)
            .whereEqualTo(fieldName, fieldValue);
        
        if (limit != null) {
            query = query.limit(limit);
        }
        
        ApiFuture<QuerySnapshot> apiFuture = query.get();
        CompletableFuture<QuerySnapshot> future = toCompletableFuture(apiFuture);
        
        return Mono.fromFuture(future)
                .map(QuerySnapshot::getDocuments)
                .flatMapMany(Flux::fromIterable)
                .map(doc -> mapDocument(doc, entityClass, idSetter))
                .onErrorResume(throwable -> {
                    log.error("Query operation failed for field " + fieldName + " in collection " + collectionName + ": " + throwable.getMessage(), throwable);
                    return Flux.empty();
                });
    }

    /**
     * Generic single field query that returns first result as Mono
     */
    public <T> Mono<T> findFirstByField(String collectionName, String fieldName, Object fieldValue,
                                       Class<T> entityClass, BiConsumer<T, String> idSetter) {
        return findByFieldWithLimit(collectionName, fieldName, fieldValue, 
                                   entityClass, idSetter, 1)
                .next();
    }

    /**
     * Generic query with ordering
     */
    public <T> Flux<T> findByFieldOrderBy(String collectionName, String fieldName, Object fieldValue,
                                         String orderByField, Query.Direction direction,
                                         Class<T> entityClass, BiConsumer<T, String> idSetter) {
        ApiFuture<QuerySnapshot> apiFuture = firestore.collection(collectionName)
            .whereEqualTo(fieldName, fieldValue)
            .orderBy(orderByField, direction)
            .get();
        CompletableFuture<QuerySnapshot> future = toCompletableFuture(apiFuture);
        
        return Mono.fromFuture(future)
                .map(QuerySnapshot::getDocuments)
                .flatMapMany(Flux::fromIterable)
                .map(doc -> mapDocument(doc, entityClass, idSetter))
                .onErrorResume(throwable -> {
                    log.error("Failed to find documents by " + fieldName + " ordered by " + orderByField + " in collection " + collectionName + ": " + throwable.getMessage(), throwable);
                    return Flux.empty();
                });
    }

    /**
     * Generic query with ordering and limit
     */
    public <T> Flux<T> findByFieldOrderByWithLimit(String collectionName, String fieldName, Object fieldValue,
                                                   String orderByField, Query.Direction direction, int limit,
                                                   Class<T> entityClass, BiConsumer<T, String> idSetter) {
        ApiFuture<QuerySnapshot> apiFuture = firestore.collection(collectionName)
            .whereEqualTo(fieldName, fieldValue)
            .orderBy(orderByField, direction)
            .limit(limit)
            .get();
        CompletableFuture<QuerySnapshot> future = toCompletableFuture(apiFuture);
        
        return Mono.fromFuture(future)
                .map(QuerySnapshot::getDocuments)
                .flatMapMany(Flux::fromIterable)
                .map(doc -> mapDocument(doc, entityClass, idSetter))
                .onErrorResume(throwable -> {
                    log.error("Failed to find documents by " + fieldName + " ordered by " + orderByField + " with limit in collection " + collectionName + ": " + throwable.getMessage(), throwable);
                    return Flux.empty();
                });
    }

    /**
     * Generic compound query for two fields
     */
    public <T> Flux<T> findByTwoFields(String collectionName, 
                                      String field1Name, Object field1Value,
                                      String field2Name, Object field2Value,
                                      Class<T> entityClass, BiConsumer<T, String> idSetter) {
        ApiFuture<QuerySnapshot> apiFuture = firestore.collection(collectionName)
            .whereEqualTo(field1Name, field1Value)
            .whereEqualTo(field2Name, field2Value)
            .get();
        CompletableFuture<QuerySnapshot> future = toCompletableFuture(apiFuture);
        
        return Mono.fromFuture(future)
                .map(QuerySnapshot::getDocuments)
                .flatMapMany(Flux::fromIterable)
                .map(doc -> mapDocument(doc, entityClass, idSetter))
                .onErrorResume(throwable -> {
                    log.error("Failed to find documents by " + field1Name + " and " + field2Name + " in collection " + collectionName + ": " + throwable.getMessage(), throwable);
                    return Flux.empty();
                });
    }

    /**
     * Generic compound query for two fields with ordering
     */
    public <T> Flux<T> findByTwoFieldsOrderBy(String collectionName, 
                                             String field1Name, Object field1Value,
                                             String field2Name, Object field2Value,
                                             String orderByField, Query.Direction direction,
                                             Class<T> entityClass, BiConsumer<T, String> idSetter) {
        ApiFuture<QuerySnapshot> apiFuture = firestore.collection(collectionName)
            .whereEqualTo(field1Name, field1Value)
            .whereEqualTo(field2Name, field2Value)
            .orderBy(orderByField, direction)
            .get();
        CompletableFuture<QuerySnapshot> future = toCompletableFuture(apiFuture);
        
        return Mono.fromFuture(future)
                .map(QuerySnapshot::getDocuments)
                .flatMapMany(Flux::fromIterable)
                .map(doc -> mapDocument(doc, entityClass, idSetter))
                .onErrorResume(throwable -> {
                    log.error("Failed to find documents by " + field1Name + " and " + field2Name + " ordered by " + orderByField + " in collection " + collectionName + ": " + throwable.getMessage(), throwable);
                    return Flux.empty();
                });
    }

    /**
     * Generic exists check
     */
    public Mono<Boolean> existsByField(String collectionName, String fieldName, Object fieldValue) {
        ApiFuture<QuerySnapshot> apiFuture = firestore.collection(collectionName)
            .whereEqualTo(fieldName, fieldValue)
            .limit(1)
            .get();
        CompletableFuture<QuerySnapshot> future = toCompletableFuture(apiFuture);
        
        return Mono.fromFuture(future)
                .map(querySnapshot -> !querySnapshot.isEmpty())
                .onErrorResume(throwable -> {
                    log.error("Failed to check if documents exist by " + fieldName + " in collection " + collectionName + ": " + throwable.getMessage(), throwable);
                    return Mono.just(false);
                });
    }

    /**
     * Generic count operation
     */
    public Mono<Long> countByField(String collectionName, String fieldName, Object fieldValue) {
        ApiFuture<QuerySnapshot> apiFuture = firestore.collection(collectionName)
            .whereEqualTo(fieldName, fieldValue)
            .get();
        CompletableFuture<QuerySnapshot> future = toCompletableFuture(apiFuture);
        
        return Mono.fromFuture(future)
                .map(querySnapshot -> (long) querySnapshot.size())
                .onErrorResume(throwable -> {
                    log.error("Failed to count documents by " + fieldName + " in collection " + collectionName + ": " + throwable.getMessage(), throwable);
                    return Mono.just(0L);
                });
    }

    /**
     * Generic batch get by IDs
     */
    public <T> Flux<T> findByIdsIn(String collectionName, List<String> ids,
                                   Class<T> entityClass, BiConsumer<T, String> idSetter) {
        List<DocumentReference> refs = ids.stream()
            .map(id -> firestore.collection(collectionName).document(id))
            .toList();
        
        ApiFuture<List<DocumentSnapshot>> apiFuture = firestore.getAll(refs.toArray(new DocumentReference[0]));
        CompletableFuture<List<DocumentSnapshot>> future = toCompletableFuture(apiFuture);
        
        return Mono.fromFuture(future)
                .flatMapMany(Flux::fromIterable)
                .filter(DocumentSnapshot::exists)
                .map(doc -> mapDocument(doc, entityClass, idSetter))
                .onErrorResume(throwable -> {
                    log.error("Failed to batch get documents from collection " + collectionName + ": " + throwable.getMessage(), throwable);
                    return Flux.empty();
                });
    }

    /**
     * Generic find all operation
     */
    public <T> Flux<T> findAll(String collectionName, Class<T> entityClass, BiConsumer<T, String> idSetter) {
        ApiFuture<QuerySnapshot> apiFuture = firestore.collection(collectionName).get();
        CompletableFuture<QuerySnapshot> future = toCompletableFuture(apiFuture);
        
        return Mono.fromFuture(future)
                .map(QuerySnapshot::getDocuments)
                .flatMapMany(Flux::fromIterable)
                .map(doc -> mapDocument(doc, entityClass, idSetter))
                .onErrorResume(throwable -> {
                    log.error("Failed to find all documents in collection " + collectionName + ": " + throwable.getMessage(), throwable);
                    return Flux.empty();
                });
    }

    /**
     * Generic array contains query
     */
    public <T> Flux<T> findByArrayContains(String collectionName, String fieldName, Object value,
                                          Class<T> entityClass, BiConsumer<T, String> idSetter) {
        ApiFuture<QuerySnapshot> apiFuture = firestore.collection(collectionName)
            .whereArrayContains(fieldName, value)
            .get();
        CompletableFuture<QuerySnapshot> future = toCompletableFuture(apiFuture);
        
        return Mono.fromFuture(future)
                .map(QuerySnapshot::getDocuments)
                .flatMapMany(Flux::fromIterable)
                .map(doc -> mapDocument(doc, entityClass, idSetter))
                .onErrorResume(throwable -> {
                    log.error("Failed to find documents by array contains " + fieldName + " in collection " + collectionName + ": " + throwable.getMessage(), throwable);
                    return Flux.empty();
                });
    }

    /**
     * Generic array contains query with ordering
     */
    public <T> Flux<T> findByArrayContainsOrderBy(String collectionName, String fieldName, Object value,
                                                  String orderByField, Query.Direction direction,
                                                  Class<T> entityClass, BiConsumer<T, String> idSetter) {
        ApiFuture<QuerySnapshot> apiFuture = firestore.collection(collectionName)
            .whereArrayContains(fieldName, value)
            .orderBy(orderByField, direction)
            .get();
        CompletableFuture<QuerySnapshot> future = toCompletableFuture(apiFuture);
        
        return Mono.fromFuture(future)
                .map(QuerySnapshot::getDocuments)
                .flatMapMany(Flux::fromIterable)
                .map(doc -> mapDocument(doc, entityClass, idSetter))
                .onErrorResume(throwable -> {
                    log.error("Failed to find documents by array contains " + fieldName + " ordered by " + orderByField + " in collection " + collectionName + ": " + throwable.getMessage(), throwable);
                    return Flux.empty();
                });
    }

    /**
     * Generic delete by field operation
     */
    public Mono<Void> deleteByField(String collectionName, String fieldName, Object fieldValue) {
        ApiFuture<QuerySnapshot> queryFuture = firestore.collection(collectionName)
            .whereEqualTo(fieldName, fieldValue)
            .get();
        CompletableFuture<QuerySnapshot> future = toCompletableFuture(queryFuture);
        
        return Mono.fromFuture(future)
                .flatMap(querySnapshot -> {
                    List<CompletableFuture<WriteResult>> deleteFutures = querySnapshot.getDocuments()
                        .stream()
                        .map(doc -> toCompletableFuture(doc.getReference().delete()))
                        .toList();
                    
                    return Mono.fromFuture(CompletableFuture.allOf(deleteFutures.toArray(new CompletableFuture[0])));
                })
                .then()
                .onErrorResume(throwable -> {
                    log.error("Failed to delete documents by " + fieldName + " in collection " + collectionName + ": " + throwable.getMessage(), throwable);
                    return Mono.error(new RuntimeException("Failed to delete documents by " + fieldName + " in collection " + collectionName, throwable));
                });
    }

    /**
     * Helper method to map DocumentSnapshot to entity
     */
    private <T> T mapDocument(DocumentSnapshot doc, Class<T> entityClass, BiConsumer<T, String> idSetter) {
        T entity = doc.toObject(entityClass);
        if (entity != null) {
            idSetter.accept(entity, doc.getId());
        } else {
            log.warn("Failed to deserialize document {} to class {}: document exists but toObject returned null", 
                    doc.getId(), entityClass.getSimpleName());
        }
        return entity;
    }
    
    /**
     * Convert ApiFuture to CompletableFuture to work with Reactor
     */
    private <T> CompletableFuture<T> toCompletableFuture(ApiFuture<T> apiFuture) {
        CompletableFuture<T> completableFuture = new CompletableFuture<>();
        
        apiFuture.addListener(() -> {
            try {
                completableFuture.complete(apiFuture.get());
            } catch (Exception e) {
                completableFuture.completeExceptionally(e);
            }
        }, Runnable::run);
        
        return completableFuture;
    }
    
    
}