package com.smlikelion.webfounder.Recruit.Controller;

import com.smlikelion.webfounder.Recruit.Dto.Request.RecruitmentRequest;
import com.smlikelion.webfounder.Recruit.Dto.Response.BatchResult;
import com.smlikelion.webfounder.Recruit.Dto.Response.RecruitmentResponse;
import com.smlikelion.webfounder.Recruit.Entity.Joiner;
import com.smlikelion.webfounder.Recruit.Repository.JoinerRepository;
import com.smlikelion.webfounder.Recruit.Service.RecruitService;
import com.smlikelion.webfounder.Recruit.exception.DuplicateStudentIdException;
import com.smlikelion.webfounder.global.dto.response.BaseResponse;
import com.smlikelion.webfounder.global.dto.response.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/recruit")
@Slf4j
public class RecruitController {

    @Autowired
    private RecruitService recruitService;

    @Autowired
    private JoinerRepository joinerRepository;

    @Value("${GOOGLE_DOCS_DOCUMENT_ID}")
    private String documentId;


    @Operation(summary = "트랙별 서류 작성하기 및 Google Docs 자동 업로드")
    @PostMapping("/docs")
    public BaseResponse<RecruitmentResponse> submitRecruitment(
            @RequestParam("track") String track,
            @RequestBody @Valid RecruitmentRequest request,
            BindingResult bindingResult) {

        logValidationErrors(bindingResult);

        if ("fe".equalsIgnoreCase(track) || "pm".equalsIgnoreCase(track) || "be".equalsIgnoreCase(track)) {
            try {
                // 서류 등록
                RecruitmentResponse recruitResponse = recruitService.registerRecruitment(request);

                // Google Docs 업로드
                recruitService.uploadToGoogleDocs(documentId, request);
                log.info("Google Docs에 서류가 정상적으로 업로드됨: {}", documentId);

                return new BaseResponse<>(recruitResponse);
            } catch (DuplicateStudentIdException e) {
                return new BaseResponse<>(ErrorCode.NOT_FOUND.getCode(), ErrorCode.DUPLICATE_STUDENT_ID_ERROR.getMessage(), null);
            } catch (Exception e) {
                log.error("Google Docs 업로드 중 오류 발생", e);
                return new BaseResponse<>(ErrorCode.INTERNAL_SERVER_ERROR.getCode(), "Google Docs update failed", null);
            }
        } else {
            // 유효하지 않은 트랙 값 처리
            String errorMessage = "Invalid track value. Please provide a valid track (fe, pm, be).";
            return new BaseResponse<>(ErrorCode.NOT_FOUND.getCode(), errorMessage, null);
        }
    }

    @Operation(summary = "트랙별 서류 작성 페이지 조회하기")
    @GetMapping("/docs/{joinerId}")
    public BaseResponse<RecruitmentResponse> getJoinerDetails(
            @PathVariable Long joinerId) {
        Joiner joiner = joinerRepository.findById(joinerId).orElse(null);

        if (joiner != null) {
            // Joiner를 찾은 경우, RecruitmentResponse로 변환하여 응답 반환
            RecruitmentResponse recruitResponse = RecruitmentResponse.builder()
                    .id(joiner.getId())
                    .studentInfo(joiner.toStudentInfoResponse())
                    .answerList(joiner.toAnswerListResponse())
                    .interviewTime(joiner.getInterviewTimeValues()) // 필요에 따라 수정
                    .build();

            return new BaseResponse<>(recruitResponse);
        } else {
            // Joiner를 찾지 못한 경우, 오류 응답 반환
            return new BaseResponse<>(ErrorCode.NOT_FOUND);
        }
    }

    @Operation(summary = "트랙별 서류 결과 합격자 조회하기")
    @PostMapping("/docs/result")
    private void logValidationErrors(BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            List<ObjectError> errors = bindingResult.getAllErrors();
            errors.forEach(error -> log.error("Validation error: {}", error.getDefaultMessage()));
        }
    }

    @Operation(summary = "트랙별 서류를 Google Docs에 추가")
    @PostMapping("/docs/upload")
    public BaseResponse<String> uploadToExistingGoogleDoc(
            @RequestParam("documentId") String documentId,
            @RequestBody @Valid RecruitmentRequest request,
            BindingResult bindingResult) {

        logValidationErrors(bindingResult);

        try {
            String docId = recruitService.uploadToGoogleDocs(documentId, request);
            return new BaseResponse<>("Content added to document successfully with ID: " + docId);
        } catch (Exception e) {
            return new BaseResponse<>(ErrorCode.INTERNAL_SERVER_ERROR.getCode(), "Google Docs update failed", null);
        }
    }
    
    /**
     * 새로운 Google Docs 내보내기 API
     * - 개별 지원서 문서 생성 + 20개 단위 묶음 문서 추가
     * - 기존 지원서 등록과 분리된 독립적인 엔드포인트
     * 
     * @author 채민
     */
    @Operation(summary = "지원서를 개별 문서 생성 후 묶음 문서에 추가")
    @PostMapping("/docs/export")
    public BaseResponse<BatchResult> exportToGoogleDocs(
            @RequestParam("track") String track,
            @RequestBody @Valid RecruitmentRequest request,
            BindingResult bindingResult) {

        logValidationErrors(bindingResult);

        // 트랙 유효성 검사 - 채민
        if (!"fe".equalsIgnoreCase(track) && !"pm".equalsIgnoreCase(track) && !"be".equalsIgnoreCase(track)) {
            return new BaseResponse<>(ErrorCode.NOT_FOUND.getCode(), 
                "Invalid track value. Please provide a valid track (fe, pm, be).", null);
        }

        try {
            // Google Docs 내보내기 실행 - 채민
            BatchResult batchResult = recruitService.exportToGoogleDocsWithBatch(request);
            
            if (batchResult.isSuccess()) {
                log.info("Google Docs 내보내기 성공: 학번={}, 묶음번호={}", 
                        request.getStudentInfo().getStudentId(), batchResult.getBatchNumber());
                return new BaseResponse<>(batchResult);
            } else {
                log.error("Google Docs 내보내기 실패: 학번={}, 오류={}", 
                        request.getStudentInfo().getStudentId(), batchResult.getErrorMessage());
                return new BaseResponse<>(ErrorCode.INTERNAL_SERVER_ERROR.getCode(), 
                        batchResult.getErrorMessage(), batchResult);
            }
            
        } catch (Exception e) {
            log.error("Google Docs 내보내기 중 예외 발생", e);
            return new BaseResponse<>(ErrorCode.INTERNAL_SERVER_ERROR.getCode(), 
                    "Google Docs export failed: " + e.getMessage(), null);
        }
    }
    
    /**
     * 통합 지원서 제출 API (새로운 Google Docs 내보내기 포함)
     * - 지원서 DB 저장 + Google Docs 내보내기 통합 실행
     * - 기존 /docs 엔드포인트의 개선된 버전
     * 
     * @author 채민
     */
    @Operation(summary = "지원서 제출 및 새로운 Google Docs 내보내기 (통합)")
    @PostMapping("/docs/submit-with-export")
    public BaseResponse<RecruitmentResponse> submitRecruitmentWithExport(
            @RequestParam("track") String track,
            @RequestBody @Valid RecruitmentRequest request,
            BindingResult bindingResult) {

        logValidationErrors(bindingResult);

        // 트랙 유효성 검사 - 채민
        if (!"fe".equalsIgnoreCase(track) && !"pm".equalsIgnoreCase(track) && !"be".equalsIgnoreCase(track)) {
            return new BaseResponse<>(ErrorCode.NOT_FOUND.getCode(), 
                "Invalid track value. Please provide a valid track (fe, pm, be).", null);
        }

        try {
            // 지원서 등록 + Google Docs 내보내기 통합 실행 - 채민
            RecruitmentResponse recruitResponse = recruitService.registerRecruitmentWithDocsExport(request);
            
            log.info("통합 지원서 제출 완료: 학번={}", request.getStudentInfo().getStudentId());
            return new BaseResponse<>(recruitResponse);
            
        } catch (DuplicateStudentIdException e) {
            log.warn("중복 학번 지원서 제출 시도: {}", request.getStudentInfo().getStudentId());
            return new BaseResponse<>(ErrorCode.NOT_FOUND.getCode(), 
                    ErrorCode.DUPLICATE_STUDENT_ID_ERROR.getMessage(), null);
        } catch (Exception e) {
            log.error("통합 지원서 제출 중 오류 발생", e);
            return new BaseResponse<>(ErrorCode.INTERNAL_SERVER_ERROR.getCode(), 
                    "Recruitment submission failed: " + e.getMessage(), null);
        }
    }
}
