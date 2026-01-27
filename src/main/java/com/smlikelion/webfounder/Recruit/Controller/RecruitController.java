package com.smlikelion.webfounder.Recruit.Controller;

import com.smlikelion.webfounder.Recruit.Dto.Request.RecruitmentRequest;
import com.smlikelion.webfounder.Recruit.Dto.Response.RecruitmentResponse;
import com.smlikelion.webfounder.Recruit.Service.RecruitService;
import com.smlikelion.webfounder.Recruit.exception.DuplicateStudentIdException;
import com.smlikelion.webfounder.global.dto.response.BaseResponse;
import com.smlikelion.webfounder.global.dto.response.ErrorCode;
import org.springframework.http.HttpStatus;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/recruit")
@Slf4j
public class RecruitController {

    @Autowired
    private RecruitService recruitService;

    @Value("${GOOGLE_DOCS_DOCUMENT_ID}")
    private String documentId;


    @Operation(summary = "트랙별 서류 작성하기 및 Google Docs 자동 업로드")
    @PostMapping(value = "/docs", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BaseResponse<RecruitmentResponse>> submitRecruitment(
            @RequestParam("track") String track,
            @RequestPart("request") @Valid RecruitmentRequest request,
            BindingResult bindingResult,
            @RequestPart(value="programmersFile", required=false) MultipartFile programmersFile) {

        logValidationErrors(bindingResult);

        if ("fe".equalsIgnoreCase(track) || "pm".equalsIgnoreCase(track) || "be".equalsIgnoreCase(track)) {
            try {
                // 서류 등록
                RecruitmentResponse recruitResponse = recruitService.registerRecruitment(request, programmersFile, documentId);

                return ResponseEntity.ok(new BaseResponse<>(recruitResponse));

            } catch (DuplicateStudentIdException e) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(new BaseResponse<>(ErrorCode.DUPLICATE_STUDENT_ID_ERROR));
            } catch (Exception e) {
                log.error("Google Docs 업로드 중 오류 발생", e);

                BaseResponse<RecruitmentResponse> response = new BaseResponse<>(ErrorCode.INTERNAL_SERVER_ERROR);
                response.setMessage("Google docs update failed");

                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }
        } else {
            // 유효하지 않은 트랙 값 처리
            BaseResponse<RecruitmentResponse> response = new BaseResponse<>(ErrorCode.UNSUPPORTED_TRACK_ERROR);
            response.setMessage("Invalid track value. Please provide a valid track (fe, pm, be).");

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    @Operation(summary = "트랙별 서류 작성 페이지 조회하기")
    @GetMapping("/docs/{joinerId}")
    public BaseResponse<RecruitmentResponse> getJoinerDetails(
            @PathVariable Long joinerId) {
        log.info("트랙별 서류 조회 조회 요청 - joinerId: {}", joinerId);

        try{
            RecruitmentResponse recruitResponse = recruitService.getRecruitment(joinerId);
            return new BaseResponse<>(recruitResponse);
        } catch (IllegalArgumentException e){
            log.warn("잘못된 요청 - joinerId: {}, error: {}", joinerId, e.getMessage());
            return new BaseResponse<>(ErrorCode.NOT_FOUND);
        } catch (Exception e) {
            log.error("지원자 정보 조회 중 오류 발생 - joinerId: {}, error: {}", joinerId, e.getMessage(), e);
            return new BaseResponse<>(ErrorCode.INTERNAL_SERVER_ERROR);
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
    @PostMapping(value = "/docs/upload")
    public BaseResponse<String> uploadToExistingGoogleDoc(
            @RequestParam("documentId") String documentId,
            @RequestBody @Valid RecruitmentRequest request,
            BindingResult bindingResult) {

        logValidationErrors(bindingResult);

        try {
            String docId = recruitService.uploadToGoogleDocs(documentId, 1L, request);
            return new BaseResponse<>("Content added to document successfully with ID: " + docId);
        } catch (Exception e) {
            return new BaseResponse<>(ErrorCode.INTERNAL_SERVER_ERROR.getCode(), "Google Docs update failed", null);
        }
    }
}
