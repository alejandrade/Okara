package io.shrouded.okara.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
@Slf4j
public class FirebaseConfig {

    @Value("${spring.cloud.gcp.project-id}")
    private String projectId;

    @PostConstruct
    public void initialize() {
        try {
            if (FirebaseApp.getApps().isEmpty()) {
                // Use Application Default Credentials from gcloud
                GoogleCredentials credentials = GoogleCredentials.getApplicationDefault();
                
                // Set the quota project if not already set
                if (credentials.getQuotaProjectId() == null) {
                    credentials = credentials.toBuilder()
                            .setQuotaProjectId(projectId)
                            .build();
                }
                
                FirebaseOptions options = FirebaseOptions.builder()
                                                         .setCredentials(credentials)
                                                         .setProjectId(projectId)
                                                         .build();

                FirebaseApp.initializeApp(options);
                log.info("Firebase initialized successfully for project: {}", projectId);
            }
        } catch (IOException e) {
            log.error("Failed to initialize Firebase. Make sure you're logged into gcloud: {}", e.getMessage());
            log.error("Run: gcloud auth application-default login");
        }
    }

    @Bean
    public FirebaseAuth firebaseAuth() {
        return FirebaseAuth.getInstance();
    }
}