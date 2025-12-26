package com.smlikelion.webfounder.Recruit.Service;

import com.google.api.services.docs.v1.Docs;
import com.google.api.services.docs.v1.DocsScopes;
import com.google.api.services.docs.v1.model.*;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import com.smlikelion.webfounder.Recruit.Dto.Request.RecruitmentRequest;
import com.smlikelion.webfounder.Recruit.Entity.Track;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;

import java.io.IOException;
import java.util.*;

/**
 * Google Docs 지원서 개별 문서 생성 서비스
 * - 템플릿 기반 개별 지원서 문서 생성
 * - 플레이스홀더 치환을 통한 지원자 정보 삽입
 * 
 * @author 채민
 */
@Slf4j
@Service
public class GoogleDocsExportService {
    
    private final Docs docsService;
    private final Drive driveService;
    private final String templateId;
    private final String individualFolderId;
    
    /**
     * Google API 서비스 초기화 및 설정값 주입
     * - Google Docs API와 Drive API 인증 설정
     * - 템플릿 ID와 폴더 ID 환경변수 주입
     * 
     * @author 채민
     */
    public GoogleDocsExportService(
            @Value("${google.docs.template-id}") String templateId,
            @Value("${google.drive.individual-folder-id}") String individualFolderId) throws IOException {
        
        // Google API 인증 설정 - Docs와 Drive 스코프 포함 - 채민
        GoogleCredentials credentials = GoogleCredentials.fromStream(
                        new ClassPathResource("credentials.json").getInputStream())
                .createScoped(Arrays.asList(DocsScopes.DOCUMENTS, DriveScopes.DRIVE));

        // Google Docs 서비스 초기화 - 채민
        this.docsService = new Docs.Builder(
                new NetHttpTransport(),
                JacksonFactory.getDefaultInstance(),
                new HttpCredentialsAdapter(credentials))
                .setApplicationName("Recruitment System")
                .build();
        
        // Google Drive 서비스 초기화 - 채민
        this.driveService = new Drive.Builder(
                new NetHttpTransport(),
                JacksonFactory.getDefaultInstance(),
                new HttpCredentialsAdapter(credentials))
                .setApplicationName("Recruitment System")
                .build();

        this.templateId = templateId;
        this.individualFolderId = individualFolderId;
    }

    /**
     * 지원자 1명의 개별 문서 생성 메인 메서드
     * - 템플릿 복사 → 플레이스홀더 치환 → 문서 ID 반환
     * 
     * @param request 지원자 정보가 담긴 요청 객체
     * @return 생성된 문서의 ID
     * @author 채민
     */
    public String exportSingle(RecruitmentRequest request) throws IOException {
        log.info("개별 지원서 문서 생성 시작: 학번={}", request.getStudentInfo().getStudentId());
        
        // 1단계: 템플릿 복사하여 새 문서 생성 - 채민
        String documentId = copyTemplate(request);
        
        // 2단계: 플레이스홀더를 실제 데이터로 치환 - 채민
        replaceAllPlaceholders(documentId, request);
        
        log.info("개별 지원서 문서 생성 완료: 문서ID={}", documentId);
        return documentId;
    }

    /**
     * Google Docs 템플릿 복사하여 새 문서 생성
     * - 파일명: "지원서_{학번}_{이름}"
     * - 개별 폴더에 저장
     * 
     * @param request 지원자 정보
     * @return 복사된 문서의 ID
     * @author 채민
     */
    private String copyTemplate(RecruitmentRequest request) throws IOException {
        String fileName = String.format("지원서_%s_%s", 
            request.getStudentInfo().getStudentId(), 
            request.getStudentInfo().getName());
        
        // Drive API로 템플릿 파일 복사 - 채민
        File copiedFile = new File()
                .setName(fileName)
                .setParents(Collections.singletonList(individualFolderId));
        
        File result = driveService.files().copy(templateId, copiedFile).execute();
        log.info("템플릿 복사 완료: 파일명={}, 문서ID={}", fileName, result.getId());
        
        return result.getId();
    }

    /**
     * 문서 내 모든 플레이스홀더를 실제 데이터로 치환
     * - 지원자 기본 정보, 답변, 트랙 선택, 면접 시간 등
     * 
     * @param documentId 대상 문서 ID
     * @param request 지원자 정보
     * @author 채민
     */
    private void replaceAllPlaceholders(String documentId, RecruitmentRequest request) throws IOException {
        java.util.List<Request> requests = new ArrayList<>();
        
        // 지원자 기본 정보 플레이스홀더 치환 - 채민
        requests.add(createReplaceTextRequest("{{applicationNumber}}", String.valueOf(request.getStudentInfo().getStudentId())));
        requests.add(createReplaceTextRequest("{{num}}", String.valueOf(request.getStudentInfo().getStudentId())));
        requests.add(createReplaceTextRequest("{{sid}}", String.valueOf(request.getStudentInfo().getStudentId())));
        requests.add(createReplaceTextRequest("{{email}}", request.getStudentInfo().getEmail()));
        requests.add(createReplaceTextRequest("{{major}}", request.getStudentInfo().getMajor()));
        requests.add(createReplaceTextRequest("{{sem}}", String.valueOf(request.getStudentInfo().getCompletedSem())));
        requests.add(createReplaceTextRequest("{{status}}", request.getStudentInfo().getSchoolStatus()));
        requests.add(createReplaceTextRequest("{{gradYr}}", request.getStudentInfo().getGraduatedYear()));
        requests.add(createReplaceTextRequest("{{prog}}", request.getStudentInfo().getProgrammers()));
        
        // 답변 플레이스홀더 치환 (A1-A7) - 채민
        java.util.List<String> answers = request.getAnswerListRequest().toAnswerList();
        for (int i = 0; i < answers.size(); i++) {
            String placeholder = "{{answer" + (i + 1) + "}}";
            String answer = answers.get(i) != null ? answers.get(i) : "";
            requests.add(createReplaceTextRequest(placeholder, answer));
        }
        
        // 트랙 선택 체크박스 형태로 치환 - 채민
        requests.add(createReplaceTextRequest("{{trackSelection}}", formatTrack(request.getStudentInfo().getTrack())));
        
        // 포트폴리오 링크 - 채민
        requests.add(createReplaceTextRequest("{{portfolio}}", request.getStudentInfo().getPortfolio()));
        
        // 면접 시간 포맷팅 후 치환 - 채민
        requests.add(createReplaceTextRequest("{{interviewTimes}}", formatInterviewTimes(request.getInterview_time())));
        
        // 일괄 치환 실행 - 채민
        if (!requests.isEmpty()) {
            BatchUpdateDocumentRequest batchRequest = new BatchUpdateDocumentRequest().setRequests(requests);
            docsService.documents().batchUpdate(documentId, batchRequest).execute();
            log.info("플레이스홀더 치환 완료: 총 {}개", requests.size());
        }
    }

    /**
     * 텍스트 치환 요청 객체 생성 헬퍼 메서드
     * 
     * @param placeholder 치환할 플레이스홀더
     * @param replacement 치환될 텍스트
     * @return Google Docs API Request 객체
     * @author 채민
     */
    private Request createReplaceTextRequest(String placeholder, String replacement) {
        return new Request().setReplaceAllText(
                new ReplaceAllTextRequest()
                        .setContainsText(new SubstringMatchCriteria()
                                .setText(placeholder)
                                .setMatchCase(false))
                        .setReplaceText(replacement != null ? replacement : "")
        );
    }

    /**
     * 트랙 선택을 체크박스 형태로 포맷팅
     * 예: "BE" → "☑ BE  ☐ FE  ☐ PM"
     * 
     * @param selectedTrack 선택된 트랙
     * @return 체크박스 형태의 트랙 선택 문자열
     * @author 채민
     */
    private String formatTrack(String selectedTrack) {
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

    /**
     * 면접 시간 Map을 보기 좋은 형태로 포맷팅
     * 예: {2024-01-15: 14:00, 2024-01-16: 16:00} → "2024-01-15: 14:00\n2024-01-16: 16:00"
     * 
     * @param interviewTimes 면접 시간 Map (날짜 → 시간)
     * @return 포맷팅된 면접 시간 문자열
     * @author 채민
     */
    private String formatInterviewTimes(Map<String, String> interviewTimes) {
        if (interviewTimes == null || interviewTimes.isEmpty()) {
            return "면접 시간 미선택";
        }
        
        StringBuilder timeText = new StringBuilder();
        java.util.List<String> sortedDates = new ArrayList<>(interviewTimes.keySet());
        Collections.sort(sortedDates);
        
        for (String date : sortedDates) {
            if (timeText.length() > 0) {
                timeText.append("\n");
            }
            timeText.append(date).append(": ").append(interviewTimes.get(date));
        }
        
        return timeText.toString();
    }
}