package io.shrouded.okara.migrations;

import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexOperations;

@ChangeUnit(id = "database-init-001", order = "001")
public class DatabaseChangeLog001 {

    @Execution
    public void createInitialIndexes(MongoTemplate mongoTemplate) {
        // Example: Create index on users collection
        IndexOperations userIndexOps = mongoTemplate.indexOps("users");
        userIndexOps.ensureIndex(new Index().on("email", org.springframework.data.domain.Sort.Direction.ASC).unique());
        userIndexOps.ensureIndex(new Index().on("createdAt", org.springframework.data.domain.Sort.Direction.DESC));
        
        // Example: Create index on sessions collection for Redis-like functionality
        IndexOperations sessionIndexOps = mongoTemplate.indexOps("sessions");
        sessionIndexOps.ensureIndex(new Index().on("sessionId", org.springframework.data.domain.Sort.Direction.ASC).unique());
        sessionIndexOps.ensureIndex(new Index().on("expiresAt", org.springframework.data.domain.Sort.Direction.ASC)
                .expire(0)); // TTL index
    }

    @RollbackExecution
    public void rollback(MongoTemplate mongoTemplate) {
        // Drop indexes if needed
        mongoTemplate.indexOps("users").dropIndex("email_1");
        mongoTemplate.indexOps("users").dropIndex("createdAt_-1");
        mongoTemplate.indexOps("sessions").dropIndex("sessionId_1");
        mongoTemplate.indexOps("sessions").dropIndex("expiresAt_1");
    }
}