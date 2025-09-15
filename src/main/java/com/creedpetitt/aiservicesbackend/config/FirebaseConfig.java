package com.creedpetitt.aiservicesbackend.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.InputStream;

@Configuration
public class FirebaseConfig {

    @Value("${GOOGLE_APPLICATION_CREDENTIALS:firebase-service-account.json}")
    private String serviceAccountPath;

    @Bean
    public FirebaseApp firebaseApp() throws IOException {
        InputStream serviceAccount;
        
        try {
            Resource resource = new ClassPathResource(serviceAccountPath);
            serviceAccount = resource.getInputStream();
        } catch (Exception e) {
            try {
                serviceAccount = new java.io.FileInputStream("src/main/resources/" + serviceAccountPath);
            } catch (Exception ex) {
                throw new IOException("Could not load Firebase service account file: " + serviceAccountPath, ex);
            }
        }

        FirebaseOptions options = new FirebaseOptions.Builder()
                .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                .build();

        return FirebaseApp.initializeApp(options);
    }

    @Bean
    public FirebaseAuth firebaseAuth(FirebaseApp firebaseApp) {
        return FirebaseAuth.getInstance(firebaseApp);
    }
}
