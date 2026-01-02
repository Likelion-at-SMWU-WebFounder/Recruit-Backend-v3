package com.smlikelion.webfounder.Recruit.Service;

import com.google.api.services.docs.v1.Docs;
import com.google.api.services.docs.v1.model.*;
import com.smlikelion.webfounder.Recruit.Dto.Request.RecruitmentRequest;
import com.smlikelion.webfounder.Recruit.Service.docs.DocsRequests;
import com.smlikelion.webfounder.Recruit.Service.docs.DocsTableWriter;
import lombok.RequiredArgsConstructor;
import  lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleDocsService {
    private final Docs docsService;
    private final DocsRequests docsRequests;
    private final DocsTableWriter docsTableWriter;

    @Value("${google.docs.document-id}")
    private String documentId;

//    public GoogleDocsService(@Value("${google.docs.document-id}") String documentId) throws IOException {
//        GoogleCredentials credentials = GoogleCredentials.fromStream(
//                        new ClassPathResource("credentials.json").getInputStream())
//                .createScoped(Collections.singleton(DocsScopes.DOCUMENTS));
//
//        this.docsService = new Docs.Builder(
//                new NetHttpTransport(),
//                JacksonFactory.getDefaultInstance(),
//                new HttpCredentialsAdapter(credentials))
//                .setApplicationName("Recruitment System")
//                .build();
//
//        this.documentId = documentId;
//    }

    /**
     * 📌 문서의 끝 위치(문자 개수) 가져오기
     */
    private int getDocumentEndIndex() throws IOException {
        try {
            Document document = docsService.documents().get(documentId).execute();
            List<StructuralElement> elements = document.getBody().getContent();
            if (elements.isEmpty()) return 1;
            return elements.get(elements.size() - 1).getEndIndex();
        } catch (GoogleJsonResponseException e) {
            log.error("Google Docs 문서를 찾을 수 없습니다. 문서 ID={} 에러 메시지={}", documentId, e.getDetails().getMessage());
            throw new RuntimeException("Google Docs 문서 ID가 존재하지 않습니다: " + documentId, e);
        }
    }

    /**
     * 📌 문서에 지원서 내용 삽입
     */
    public void appendOneApplication(String documentId, long applicationId, RecruitmentRequest request) throws IOException {
        int docLength = getDocumentEndIndex();
        log.info("Google Docs에 서류 업로드 중: 문서 ID={}, 현재 길이={}", documentId, docLength);

        List<Request> batch1 = buildBatch(applicationId, request);
        executeBatchUpdate(documentId, batch1);

        // 표에 내용 삽입
        appendTableAndFill(documentId, applicationId, request);
    }

    private List<Request> buildBatch(long applicationId, RecruitmentRequest request) throws IOException {
        List<Request> batch = new ArrayList<>();

        addTitleSection(batch, applicationId, request);   // 제목 + 이름 삽입
        addApplicantTablePlaceholder(batch);              // 표 생성
        addTrackSection(batch, request);                  // 트랙 정보 삽입
        addEssaySection(batch, request);                  // 자소서 문항 및 답변 삽입
        addInterviewTimeSection(batch, request);          // 면접 가능 시간 삽입
        addPageBreak(batch);                              // 페이지 띄우기

        return batch;
    }

    private void addTitleSection(List<Request> batch, long applicationId, RecruitmentRequest request) throws IOException {
        int start = getDocumentEndIndex() - 1;

        String title = "[지원번호 " + applicationId + "번]";
        String name = " " + docsRequests.safe(request.getStudentInfo().getName());
        String fullText = "\n" + title + name + "\n";

        batch.add(docsRequests.insertAtIndex(fullText, start));

        int titleStart = start + 1;
        int titleEnd = titleStart + title.length();

        batch.add(docsRequests.applyHeading(titleStart, titleEnd, "HEADING_2"));
        batch.add(docsRequests.applyParagraphAlign(titleStart, titleEnd, "CENTER"));
        batch.add(docsRequests.applyTextStyle(titleStart, titleEnd, true, 14.0, 0.12f, 0.35f, 0.75f));

        int nameStart = titleEnd;
        batch.add(docsRequests.applyTextStyle(nameStart, nameStart + name.length(), true, 14.0, 0f, 0f, 0f));
    }

    private void addApplicantTablePlaceholder(List<Request> batch) {
        batch.add(docsRequests.insertTableAtEnd(2, 8));
    }

    private void addTrackSection(List<Request> batch, RecruitmentRequest request) {
        String track = request.getStudentInfo().getTrack();

        log.info("트랙: {}", track);
        batch.add(docsRequests.insertAtEnd(
                "\n* 지원 파트\n" +
                        docsRequests.checkbox("기획/디자인", "PLANDESIGN".equals(track)) + "   " +
                        docsRequests.checkbox("프론트엔드", "FRONTEND".equals(track)) + "   " +
                        docsRequests.checkbox("백엔드", "BACKEND".equals(track)) + "\n"
        ));
    }

    private void addEssaySection(List<Request> batch, RecruitmentRequest request) throws IOException {
        batch.add(docsRequests.insertAtEnd("\n[자소서 문항]\n"));

        List<String> questions = List.of(
                "1. 멋사 선택한 이유 및 지원 동기 (600자)",
                "2-2. 파트 지원 이유, 해당 파트로 어떻게 성장할 것인지 (600자)",
                "3. 웹 서비스 아이디어 (600자)",
                "4. 지금까지 했던 일 중 가장 꾸준하게 한 일 (600자)",
                "5. 열정을 다해 도전 해본 경험 (600자)",
                "6. 타인과 협업 또는 의사소통하며 성공/어려움 극복 경험과 배운 점 (600자)",
                "7. 기술 블로그 or GitHub or 포트폴리오 링크"
        );

        List<String> answers = request.getAnswerList().toAnswerList();

        for (int i = 0; i < questions.size(); i++) {
            String q = questions.get(i);
            String a = docsRequests.safe(answers.get(i));

            int qStart = getDocumentEndIndex() - 1;
            String qText = "\n" + q + "  " + a.length() + "자\n";
            batch.add(docsRequests.insertAtEnd(qText));
            int qEnd = qStart + qText.length();
            batch.add(docsRequests.applyTextStyle(qStart, qEnd, true, 12.0, null, null, null));

            int aStart = getDocumentEndIndex() - 1;
            String aText = a + "\n";
            batch.add(docsRequests.insertAtEnd(aText));
            int aEnd = aStart + aText.length();
            batch.add(docsRequests.applyParagraphIndent(aStart, aEnd, 18.0));
        }
    }

    private void addInterviewTimeSection(List<Request> batch, RecruitmentRequest request) {
        batch.add(docsRequests.insertAtEnd("\n[면접 가능 시간]\n"));

        if (request.getInterview_time() == null) return;

        for (String date : request.getInterview_time().keySet()) {
            batch.add(docsRequests.insertAtEnd("- " + docsRequests.safe(request.getInterview_time().get(date)) + "\n"));
        }
    }

    private void addPageBreak(List<Request> batch) {
        batch.add(docsRequests.insertPageBreakAtEnd());
    }

    private void executeBatchUpdate(String documentId, List<Request> requests) {
        try {
            docsService.documents().batchUpdate(
                    documentId, new BatchUpdateDocumentRequest().setRequests(requests)
            ).execute();
        } catch (GoogleJsonResponseException e) {
            log.error("Google Docs API 오류 발생! status={}, message={}", e.getStatusCode(), e.getDetails().getMessage());
            if (e.getStatusCode() == 400) {
                log.error("잘못된 인덱스 혹은 요청 형식이 포함되었습니다. 요청 내용을 확인하세요.");
            }
            throw new RuntimeException("Google Docs 업데이트 실패: " + e.getDetails().getMessage(), e);
        } catch (IOException e) {
            log.error("Google Docs 통신 오류: {}", e.getMessage());
            throw new RuntimeException("Google Docs 통신 오류", e);
        }
    }

    private void appendTableAndFill(String documentId, long applicationId, RecruitmentRequest request) throws IOException {
        Document doc = docsService.documents().get(documentId).execute();
        DocsTableWriter.TableRef tableRef = docsTableWriter.findLastTableRef(documentId);
        log.info("생성된 표의 Start Index: {}", tableRef.startIndex);

        docsTableWriter.fillAndStyleTable(documentId, tableRef, applicationId, request);
    }
}