package io.shrouded.okara.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

import java.io.IOException;

@RequiredArgsConstructor
@Configuration
@Slf4j
public class FirebaseConfig  {

    @Value("${spring.cloud.gcp.project-id}")
    private String projectId;

    private final Firestore firestore;
    private final ApplicationContext context;

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
            throw new IllegalStateException("Firebase initialization failed: " + e.getMessage(), e);
        }
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        try {
            log.info("Verifying Firestore permissions for project: {}", projectId);
            firestore.listCollections().iterator();
            log.info("Firestore permissions verified successfully");

        } catch (Exception e) {
            String errorMessage = buildPermissionErrorMessage(e);
            log.error(errorMessage);
            SpringApplication.exit(context, () -> 3);
        }
    }

    private String buildPermissionErrorMessage(Exception e) {
        StringBuilder message = new StringBuilder();
        message.append("❌ FIRESTORE PERMISSION ERROR ❌\n");
        message.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        message.append("Failed to verify Firestore permissions for project: ").append(projectId).append("\n\n");
        message.append("🔍 CAUSE: ").append(e.getMessage()).append("\n\n");
        message.append("🛠️  POSSIBLE SOLUTIONS:\n");
        message.append("   1. Check if you're authenticated with Google Cloud:\n");
        message.append("      → gcloud auth list\n");
        message.append("      → gcloud auth application-default login\n\n");
        message.append("   2. Verify the project ID is correct: ").append(projectId).append("\n");
        message.append("      → gcloud config get-value project\n");
        message.append("      → gcloud config set project YOUR_PROJECT_ID\n\n");
        message.append("   3. Ensure your account has Firestore permissions:\n");
        message.append("      → Go to: https://console.cloud.google.com/iam-admin/iam?project=").append(projectId).append("\n");
        message.append("      → Add role: 'Cloud Datastore User' or 'Firebase Admin'\n\n");
        message.append("   4. Enable Firestore API if not already enabled:\n");
        message.append("      → https://console.cloud.google.com/apis/library/firestore.googleapis.com?project=").append(projectId).append("\n\n");
        message.append("   5. Check if Firestore is properly initialized in your project:\n");
        message.append("      → https://console.firebase.google.com/project/").append(projectId).append("/firestore\n");
        message.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        
        return message.toString();
    }

    @Bean
    public FirebaseAuth firebaseAuth() {
        return FirebaseAuth.getInstance();
    }
}