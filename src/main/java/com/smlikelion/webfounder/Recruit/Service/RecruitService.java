package com.smlikelion.webfounder.Recruit.Service;

import com.smlikelion.webfounder.Recruit.Dto.Request.RecruitmentRequest;
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
import org.springframework.web.multipart.MultipartFile;


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
    private final AwsS3Service awsS3Service;

    public RecruitmentResponse registerRecruitment(RecruitmentRequest request, MultipartFile programmersFile) {

        String studentId = request.getStudentInfo().getStudentId();

        // 동일한 학번을 가진 지원자가 이미 존재하는지 확인
        if (joinerRepository.existsByStudentId(studentId)) {
            throw new DuplicateStudentIdException("동일한 학번으로 중복된 지원서가 이미 제출되었습니다.");
        }

        // 프로그래머스 인증 파일 S3에 업로드
        String fileName = null;
        if(programmersFile != null)
            fileName = awsS3Service.uploadFile(request.getStudentInfo().getName(), programmersFile);

        Joiner joiner = request.getStudentInfo().toJoiner();
        joiner.setProgrammersImageUrl(fileName);
        joiner.setStashed(false);
        joiner.setInterviewTime(request.getInterview_time());

        List<String> answerList = request.getAnswerListRequest().toAnswerList();
        joiner.setAnswerList(answerList);

        joiner = joinerRepository.save(joiner);
        if (joiner != null) {
            mailService.sendApplyStatusMail(joiner.getEmail());
        }

        StudentInfoResponse studentInfoResponse = joiner.toStudentInfoResponse(fileName);

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

    public RecruitmentResponse getRecruitment(Long joinerId) {

        // 1. joinerId 유효성 검증
        if(joinerId == null){
            throw new IllegalArgumentException("지원자 ID는 null일 수 없습니다.");
        }

        // 2. joiner 조회
        Joiner joiner = joinerRepository.findById(joinerId).orElse(null);

        String presignedUrl = null;

        // 3. fileName이 null이 아니고 비어있지 않을 때만 Pre-signed URL 생성
        try {
            String fileName = joiner.getProgrammersImageUrl();

            if (fileName != null && !fileName.isEmpty()) {
                presignedUrl = awsS3Service.generatePresignedUrl(fileName);
            }
        } catch (Exception e) {
            log.warn("Pre-signed URL 생성 실패 - joinerId: {}, fileName: {}, error: {}",
                    joinerId, joiner.getProgrammersImageUrl(), e.getMessage());
        }

        return RecruitmentResponse.builder()
                .id(joiner.getId())
                .studentInfo(joiner.toStudentInfoResponse(presignedUrl))
                .answerList(joiner.toAnswerListResponse())
                .interviewTime(joiner.getInterviewTimeValues()) // 필요에 따라 수정
                .build();

    }

    public String uploadToGoogleDocs(String documentId, Long applicationId, RecruitmentRequest request) {
        if (request == null || request.getStudentInfo() == null || request.getAnswerListRequest() == null) {
            throw new IllegalArgumentException("필수 요청 데이터가 누락되었습니다.");
        }

        try {
//            googleDocsService.uploadRecruitmentToGoogleDocs(documentId, request);
            googleDocsService.appendOneApplication(documentId, applicationId, request);
            return documentId;
        } catch (IOException e) {
            throw new RuntimeException("Google Docs 업로드 실패", e);
        }
    }
}