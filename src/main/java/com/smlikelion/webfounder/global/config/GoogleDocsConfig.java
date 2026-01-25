package com.smlikelion.webfounder.global.config;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.docs.v1.Docs;
import com.google.api.services.docs.v1.DocsScopes;
import com.google.api.services.docs.v1.model.*;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.docs.v1.DocsScopes;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.*;
import java.security.GeneralSecurityException;
import java.util.Collections;

@Slf4j
@Configuration
public class GoogleDocsConfig {

    @Bean
    public Docs docsService() throws IOException, GeneralSecurityException {
        InputStream credentialsStream = getCredentialsStream();

        GoogleCredentials credentials = GoogleCredentials
                .fromStream(credentialsStream)
                .createScoped(Collections.singletonList(DocsScopes.DOCUMENTS));

        HttpRequestInitializer requestInitializer = new HttpCredentialsAdapter(credentials);

        return new Docs.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                requestInitializer)
                .setApplicationName("Webfounder")
                .build();
    }

    private InputStream getCredentialsStream() throws FileNotFoundException {
        // 1. EC2 환경: /app/credentials.json 파일 확인
        File prodFile = new File("/app/credentials.json");
        if (prodFile.exists()) {
            return new FileInputStream(prodFile);
        }

        // 2. 로컬 환경: classpath의 credentials.json 사용
        InputStream resourceStream = getClass().getClassLoader()
                .getResourceAsStream("credentials.json");

        if (resourceStream != null) {
            return resourceStream;
        }

        throw new FileNotFoundException("credentials.json 파일을 찾을 수 없습니다.");
    }
}