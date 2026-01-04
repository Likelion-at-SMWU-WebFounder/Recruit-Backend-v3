package com.smlikelion.webfounder.Recruit.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetUrlRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.IOException;
import java.time.Duration;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AwsS3Service {

    @Value("${aws.s3.bucket}")
    private String bucket;
    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    /**
     * 파일 업로드
     * @param name 지원자의 이름
     * @param file 프로그래머스 인증 파일
     * @return fileName
     */
    public String uploadFile(String name, MultipartFile file) {
        try{
            String fileName = UUID.randomUUID() + "_" + name + "_" + file.getOriginalFilename();

            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(fileName)
                    .contentType(file.getContentType())
                    .contentLength(file.getSize())
                    .build();

            s3Client.putObject(putObjectRequest,
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

            log.info("S3에 파일 업로드 완료: {}", fileName);
            return fileName;

        } catch (Exception e){
            throw new RuntimeException("S3 업로드 실패", e);
        }
    }

    /**
     * Presigned URL 생성
     * @param fileName S3에 저장된 파일 키
     * @param durationMinutes URL 유효 시간 (분)
     * @return Presigned URL
     */
    public String generatePresignedUrl(String fileName, int durationMinutes){
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucket)
                .key(fileName)
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(durationMinutes))
                .getObjectRequest(getObjectRequest)
                .build();

        PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);
        String url = presignedRequest.url().toString();
        log.info("{} 파일에 대한 presigned URL 생성 완료, 유효기간: {}분", fileName, durationMinutes);

        return url;
    }

    /**
     * Presigned URL 생성 (기본 1시간)
     */
    public String generatePresignedUrl(String fileName) {
        return generatePresignedUrl(fileName, 60);
    }

    /**
     * 파일 다운로드
     */
    public byte[] downloadFile(String fileName) throws IOException {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucket)
                .key(fileName)
                .build();

        return s3Client.getObjectAsBytes(getObjectRequest).asByteArray();
    }

    /**
      * 파일 URL 생성
      */
    private String getFileUrl(String fileName) {
        GetUrlRequest getUrlRequest = GetUrlRequest.builder()
                .bucket(bucket)
                .key(fileName)
                .build();

        return s3Client.utilities().getUrl(getUrlRequest).toString();
    }

    /**
     * 파일 삭제
     */
    public void deleteFile(String fileName) {
        DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(fileName)
                .build();

        s3Client.deleteObject(deleteRequest);
    }

    /**
     * 퍼블릭 URL 생성
     */
    private String getUrl(String fileName) {
        GetUrlRequest getUrlRequest = GetUrlRequest.builder()
                .bucket(bucket)
                .key(fileName)
                .build();

        return s3Client.utilities().getUrl(getUrlRequest).toString();
    }

}
