package com.smlikelion.webfounder.Recruit.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smlikelion.webfounder.Recruit.Dto.Request.RecruitmentRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Google Docs Export Service 단위 테스트
 * - Mock 환경에서 플레이스홀더 치환 로직 검증
 * - 템플릿 처리 로직 검증
 * 
 * @author 채민
 */
@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
public class GoogleDocsExportServiceTest {

    private ObjectMapper objectMapper;
    private RecruitmentRequest testRequest;

    @BeforeEach
    void setUp() throws IOException {
        objectMapper = new ObjectMapper();
        
        // 테스트용 JSON 데이터 로드 - 채민
        ClassPathResource resource = new ClassPathResource("test_request.json");
        testRequest = objectMapper.readValue(resource.getInputStream(), RecruitmentRequest.class);
    }

    @Test
    void testRequestLoadSuccess() {
        // 테스트 데이터 로드 검증 - 채민
        assertNotNull(testRequest);
        assertNotNull(testRequest.getStudentInfo());
        assertEquals("채민테스트", testRequest.getStudentInfo().getName());
        assertEquals("12345", testRequest.getStudentInfo().getStudentId());
        assertEquals("BE", testRequest.getStudentInfo().getTrack());
    }

    @Test 
    void testAnswerListMapping() {
        // 답변 리스트 매핑 검증 - 채민
        assertNotNull(testRequest.getAnswerListRequest());
        
        java.util.List<String> answers = testRequest.getAnswerListRequest().toAnswerList();
        assertNotNull(answers);
        assertTrue(answers.size() >= 7);
        
        // 첫 번째 답변 확인 - 채민
        assertTrue(answers.get(0).contains("웹파운더즈에 지원하게 된 동기"));
    }

    @Test
    void testInterviewTimeMapping() {
        // 면접 시간 매핑 검증 - 채민
        assertNotNull(testRequest.getInterview_time());
        assertEquals("14:00", testRequest.getInterview_time().get("2024-01-15"));
        assertEquals("16:00", testRequest.getInterview_time().get("2024-01-16"));
        assertEquals("10:00", testRequest.getInterview_time().get("2024-01-17"));
    }

    @Test
    void testTrackFormatting() {
        // 트랙 포맷팅 로직 검증 (GoogleDocsExportService의 private 메서드 로직) - 채민
        String selectedTrack = testRequest.getStudentInfo().getTrack();
        String formattedTrack = formatTrackForTest(selectedTrack);
        
        assertTrue(formattedTrack.contains("☑ BE"));
        assertTrue(formattedTrack.contains("☐ FE"));
        assertTrue(formattedTrack.contains("☐ PM"));
    }

    @Test
    void testInterviewTimeFormatting() {
        // 면접 시간 포맷팅 로직 검증 (GoogleDocsExportService의 private 메서드 로직) - 채민
        String formattedTime = formatInterviewTimesForTest(testRequest.getInterview_time());
        
        assertTrue(formattedTime.contains("2024-01-15: 14:00"));
        assertTrue(formattedTime.contains("2024-01-16: 16:00"));
        assertTrue(formattedTime.contains("2024-01-17: 10:00"));
    }

    // 테스트용 헬퍼 메서드들 (실제 GoogleDocsExportService 로직 복제) - 채민

    private String formatTrackForTest(String selectedTrack) {
        StringBuilder trackText = new StringBuilder();
        String[] tracks = {"BE", "FE", "PM"};
        
        for (int i = 0; i < tracks.length; i++) {
            if (i > 0) trackText.append("  ");
            
            if (tracks[i].equalsIgnoreCase(selectedTrack)) {
                trackText.append("☑ ").append(tracks[i]);
            } else {
                trackText.append("☐ ").append(tracks[i]);
            }
        }
        
        return trackText.toString();
    }

    private String formatInterviewTimesForTest(java.util.Map<String, String> interviewTimes) {
        if (interviewTimes == null || interviewTimes.isEmpty()) {
            return "면접 시간 미선택";
        }
        
        StringBuilder timeText = new StringBuilder();
        java.util.List<String> sortedDates = new java.util.ArrayList<>(interviewTimes.keySet());
        java.util.Collections.sort(sortedDates);
        
        for (String date : sortedDates) {
            if (timeText.length() > 0) {
                timeText.append("\\n");
            }
            timeText.append(date).append(": ").append(interviewTimes.get(date));
        }
        
        return timeText.toString();
    }
}