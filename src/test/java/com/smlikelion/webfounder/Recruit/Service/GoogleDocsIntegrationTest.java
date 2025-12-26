package com.smlikelion.webfounder.Recruit.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smlikelion.webfounder.Recruit.Dto.Request.RecruitmentRequest;
import com.smlikelion.webfounder.Recruit.Dto.Response.BatchResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Google Docs 통합 테스트
 * - 실제 Google API를 호출하여 문서 생성 테스트
 * - 데이터베이스 없이 Google Docs 기능만 독립적으로 테스트
 * 
 * @author 채민
 */
public class GoogleDocsIntegrationTest {

    private GoogleDocsExportService googleDocsExportService;
    private GoogleAppsScriptService googleAppsScriptService;
    private RecruitmentRequest testRequest;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() throws IOException {
        objectMapper = new ObjectMapper();
        
        // 테스트용 JSON 데이터 로드 - 채민
        ClassPathResource resource = new ClassPathResource("test_request.json");
        testRequest = objectMapper.readValue(resource.getInputStream(), RecruitmentRequest.class);
        
        // 환경변수에서 Google API 설정 읽기 - 채민
        String templateId = System.getenv("GOOGLE_DOCS_TEMPLATE_ID");
        String individualFolderId = System.getenv("GOOGLE_DRIVE_INDIVIDUAL_FOLDER_ID");
        String webAppUrl = System.getenv("GOOGLE_APPS_SCRIPT_WEB_APP_URL");
        
        assertNotNull(templateId, "GOOGLE_DOCS_TEMPLATE_ID 환경변수가 설정되어야 합니다");
        assertNotNull(individualFolderId, "GOOGLE_DRIVE_INDIVIDUAL_FOLDER_ID 환경변수가 설정되어야 합니다");
        assertNotNull(webAppUrl, "GOOGLE_APPS_SCRIPT_WEB_APP_URL 환경변수가 설정되어야 합니다");
        
        // Google API 서비스 초기화 - 채민
        try {
            googleDocsExportService = new GoogleDocsExportService(templateId, individualFolderId);
            googleAppsScriptService = new GoogleAppsScriptService(webAppUrl, new org.springframework.web.client.RestTemplate());
            
            System.out.println("✅ Google API 서비스 초기화 성공");
            System.out.println("📄 템플릿 ID: " + templateId);
            System.out.println("📁 개별 폴더 ID: " + individualFolderId);
            System.out.println("🔗 Apps Script URL: " + webAppUrl);
            
        } catch (Exception e) {
            System.err.println("❌ Google API 서비스 초기화 실패: " + e.getMessage());
            throw e;
        }
    }

    @Test
    void testGoogleDocsDocumentCreation() throws IOException {
        System.out.println("\\n🧪 Google Docs 개별 문서 생성 테스트 시작");
        
        // 개별 문서 생성 테스트 - 채민
        String documentId = googleDocsExportService.exportSingle(testRequest);
        
        assertNotNull(documentId, "문서 ID가 생성되어야 합니다");
        assertFalse(documentId.trim().isEmpty(), "문서 ID가 비어있지 않아야 합니다");
        
        System.out.println("✅ 개별 문서 생성 성공!");
        System.out.println("📄 생성된 문서 ID: " + documentId);
        System.out.println("🔗 문서 URL: https://docs.google.com/document/d/" + documentId + "/edit");
    }

    @Test 
    void testGoogleAppsScriptBatchProcess() throws IOException {
        System.out.println("\\n🧪 Google Apps Script 묶음 처리 테스트 시작");
        
        // 먼저 개별 문서 생성 - 채민
        String documentId = googleDocsExportService.exportSingle(testRequest);
        System.out.println("📄 개별 문서 생성 완료: " + documentId);
        
        // Apps Script를 통한 묶음 처리 - 채민
        BatchResult batchResult = googleAppsScriptService.addApplicationToBatch(
            testRequest.getStudentInfo().getStudentId(), 
            documentId
        );
        
        assertNotNull(batchResult, "배치 결과가 반환되어야 합니다");
        assertTrue(batchResult.isSuccess(), "배치 처리가 성공해야 합니다: " + batchResult.getErrorMessage());
        assertNotNull(batchResult.getBatchDocId(), "배치 문서 ID가 있어야 합니다");
        
        System.out.println("✅ Apps Script 묶음 처리 성공!");
        System.out.println("📦 배치 문서 ID: " + batchResult.getBatchDocId());
        System.out.println("📊 배치 번호: " + batchResult.getBatchNumber());
        System.out.println("🔗 배치 문서 URL: " + batchResult.getUrl());
    }

    @Test
    void testCompleteWorkflow() throws IOException {
        System.out.println("\\n🧪 전체 워크플로우 테스트 시작");
        System.out.println("👤 테스트 지원자: " + testRequest.getStudentInfo().getName());
        System.out.println("🆔 학번: " + testRequest.getStudentInfo().getStudentId());
        System.out.println("🎯 트랙: " + testRequest.getStudentInfo().getTrack());
        
        // 1단계: 개별 문서 생성 - 채민
        System.out.println("\\n1️⃣ 개별 문서 생성 중...");
        String documentId = googleDocsExportService.exportSingle(testRequest);
        System.out.println("✅ 개별 문서 생성 성공: " + documentId);
        
        // 2단계: 묶음 문서에 추가 - 채민  
        System.out.println("\\n2️⃣ 묶음 문서에 추가 중...");
        BatchResult batchResult = googleAppsScriptService.addApplicationToBatch(
            testRequest.getStudentInfo().getStudentId(), 
            documentId
        );
        
        // 결과 검증 - 채민
        assertNotNull(documentId);
        assertNotNull(batchResult);
        assertTrue(batchResult.isSuccess(), "배치 처리 실패: " + batchResult.getErrorMessage());
        
        System.out.println("\\n🎉 전체 워크플로우 테스트 성공!");
        System.out.println("📄 개별 문서: https://docs.google.com/document/d/" + documentId + "/edit");
        System.out.println("📦 묶음 문서: " + batchResult.getUrl());
        System.out.println("\\n📋 결과 요약:");
        System.out.println("  - 개별 문서 ID: " + documentId);
        System.out.println("  - 배치 문서 ID: " + batchResult.getBatchDocId()); 
        System.out.println("  - 배치 번호: " + batchResult.getBatchNumber());
        System.out.println("  - 처리 상태: " + (batchResult.isSuccess() ? "성공" : "실패"));
    }

    @Test 
    void testEnvironmentVariables() {
        System.out.println("\\n🧪 환경변수 설정 테스트");
        
        String templateId = System.getenv("GOOGLE_DOCS_TEMPLATE_ID");
        String individualFolderId = System.getenv("GOOGLE_DRIVE_INDIVIDUAL_FOLDER_ID"); 
        String outputFolderId = System.getenv("GOOGLE_DRIVE_OUTPUT_FOLDER_ID");
        String webAppUrl = System.getenv("GOOGLE_APPS_SCRIPT_WEB_APP_URL");
        
        System.out.println("📄 템플릿 ID: " + (templateId != null ? "✅ 설정됨" : "❌ 미설정"));
        System.out.println("📁 개별 폴더 ID: " + (individualFolderId != null ? "✅ 설정됨" : "❌ 미설정"));
        System.out.println("📦 묶음 폴더 ID: " + (outputFolderId != null ? "✅ 설정됨" : "❌ 미설정"));
        System.out.println("🔗 Apps Script URL: " + (webAppUrl != null ? "✅ 설정됨" : "❌ 미설정"));
        
        assertNotNull(templateId, "GOOGLE_DOCS_TEMPLATE_ID 환경변수 필수");
        assertNotNull(individualFolderId, "GOOGLE_DRIVE_INDIVIDUAL_FOLDER_ID 환경변수 필수");
        assertNotNull(outputFolderId, "GOOGLE_DRIVE_OUTPUT_FOLDER_ID 환경변수 필수");
        assertNotNull(webAppUrl, "GOOGLE_APPS_SCRIPT_WEB_APP_URL 환경변수 필수");
    }
}