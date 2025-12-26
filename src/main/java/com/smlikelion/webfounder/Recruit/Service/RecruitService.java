package com.smlikelion.webfounder.Recruit.Service;

import com.smlikelion.webfounder.Recruit.Dto.Request.RecruitmentRequest;
import com.smlikelion.webfounder.Recruit.Dto.Response.BatchResult;
import com.smlikelion.webfounder.Recruit.Dto.Response.RecruitmentResponse;
import com.smlikelion.webfounder.Recruit.Dto.Response.StudentInfoResponse;
import com.smlikelion.webfounder.Recruit.Entity.*;
import com.smlikelion.webfounder.Recruit.Repository.JoinerRepository;
import com.smlikelion.webfounder.Recruit.exception.DuplicateStudentIdException;
import com.smlikelion.webfounder.manage.entity.Candidate;
import com.smlikelion.webfounder.manage.repository.CandidateRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;


import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


@Slf4j
@Service
@AllArgsConstructor
public class RecruitService {

    private final JoinerRepository joinerRepository;
    private final CandidateRepository candidateRepository;
    private final MailService mailService;
    private final GoogleDocsService googleDocsService;
    // 새로운 Google Docs 내보내기 서비스 추가 - 채민
    private final GoogleDocsExportService googleDocsExportService;
    private final GoogleAppsScriptService googleAppsScriptService;

    public RecruitmentResponse registerRecruitment(RecruitmentRequest request) {

        String studentId = request.getStudentInfo().getStudentId();

        // 동일한 학번을 가진 지원자가 이미 존재하는지 확인
        if (joinerRepository.existsByStudentId(studentId)) {
            throw new DuplicateStudentIdException("동일한 학번으로 중복된 지원서가 이미 제출되었습니다.");
        }

        Joiner joiner = request.getStudentInfo().toJoiner();
        joiner.setInterviewTime(request.getInterview_time());

        List<String> answerList = request.getAnswerListRequest().toAnswerList();
        joiner.setAnswerList(answerList);

        joiner = joinerRepository.save(joiner);
        if (joiner != null) {
            mailService.sendApplyStatusMail(joiner.getEmail());
        }
        StudentInfoResponse studentInfoResponse = joiner.toStudentInfoResponse();

        // cadidate entity 생성 시 서류합 란을 reject로 초기 설정
        Candidate candidate = new Candidate(joiner, "REJECT", "REJECT");
        candidateRepository.save(candidate);

        Set<String> interviewTime = request.getInterview_time().values().stream().collect(Collectors.toSet());

        return RecruitmentResponse.builder()
                .id(joiner.getId())
                .studentInfo(studentInfoResponse)
                .answerList(joiner.toAnswerListResponse())
                .interviewTime(interviewTime)
                .build();
    }

    public String uploadToGoogleDocs(String documentId, RecruitmentRequest request) {
        if (request == null || request.getStudentInfo() == null || request.getAnswerListRequest() == null) {
            throw new IllegalArgumentException("필수 요청 데이터가 누락되었습니다.");
        }

        try {
            googleDocsService.uploadRecruitmentToGoogleDocs(documentId, request);
            return documentId;
        } catch (IOException e) {
            throw new RuntimeException("Google Docs 업로드 실패", e);
        }
    }
    
    /**
     * 지원서를 개별 문서로 생성한 후 묶음 문서에 추가하는 통합 메서드
     * - 1단계: 개별 지원서 문서 생성 (템플릿 기반)
     * - 2단계: 생성된 문서를 20개 단위 묶음 문서에 추가
     * 
     * @param request 지원자 정보
     * @return 묶음 문서 작업 결과
     * @author 채민
     */
    public BatchResult exportToGoogleDocsWithBatch(RecruitmentRequest request) {
        try {
            // 1단계: 개별 지원서 문서 생성 - 채민
            String individualDocId = googleDocsExportService.exportSingle(request);
            
            // 2단계: 묶음 문서에 추가 - 채민
            BatchResult batchResult = googleAppsScriptService.addApplicationToBatch(
                request.getStudentInfo().getStudentId(), 
                individualDocId
            );
            
            return batchResult;
            
        } catch (Exception e) {
            // 오류 발생 시 BatchResult로 반환 - 채민
            return BatchResult.builder()
                    .success(false)
                    .errorMessage("지원서 내보내기 실패: " + e.getMessage())
                    .build();
        }
    }
    
    /**
     * 지원서 제출 및 Google Docs 내보내기 통합 메서드
     * - 기존 지원서 등록 로직 + 새로운 Google Docs 내보내기
     * - 트랜잭션 내에서 DB 저장 후 문서 생성
     * 
     * @param request 지원자 정보
     * @return 지원서 등록 결과 (묶음 문서 정보 포함)
     * @author 채민
     */
    public RecruitmentResponse registerRecruitmentWithDocsExport(RecruitmentRequest request) {
        // 1단계: 기존 지원서 등록 로직 - 채민
        RecruitmentResponse response = registerRecruitment(request);
        
        try {
            // 2단계: Google Docs 내보내기 - 채민
            BatchResult batchResult = exportToGoogleDocsWithBatch(request);
            
            // 로그 출력 (성공/실패 상관없이) - 채민
            if (batchResult.isSuccess()) {
                log.info("지원서 Google Docs 내보내기 성공: 학번={}, 묶음번호={}, 문서ID={}", 
                        request.getStudentInfo().getStudentId(), 
                        batchResult.getBatchNumber(), 
                        batchResult.getBatchDocId());
            } else {
                log.error("지원서 Google Docs 내보내기 실패: 학번={}, 오류={}", 
                        request.getStudentInfo().getStudentId(), 
                        batchResult.getErrorMessage());
            }
            
        } catch (Exception e) {
            // Google Docs 내보내기 실패해도 지원서 등록은 유지 - 채민
            log.error("Google Docs 내보내기 중 예외 발생: 학번={}, 오류={}", 
                    request.getStudentInfo().getStudentId(), e.getMessage(), e);
        }
        
        return response;
    }
}