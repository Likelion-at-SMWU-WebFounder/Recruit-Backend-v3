package com.smlikelion.webfounder.Recruit.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smlikelion.webfounder.Recruit.Dto.Response.BatchResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * Google Apps Script 웹 앱 호출 서비스
 * - 개별 지원서를 20개 단위 묶음 문서에 추가
 * - HTTP POST 요청으로 Apps Script와 통신
 * 
 * @author 채민
 */
@Slf4j
@Service
public class GoogleAppsScriptService {
    
    private final String webAppUrl;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    /**
     * 서비스 초기화
     * - Google Apps Script 웹 앱 URL 주입
     * - RestTemplate과 ObjectMapper 초기화
     * 
     * @author 채민
     */
    public GoogleAppsScriptService(
            @Value("${google.apps-script.web-app-url}") String webAppUrl,
            RestTemplate restTemplate) {
        this.webAppUrl = webAppUrl;
        this.restTemplate = restTemplate;
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * 개별 지원서를 묶음 문서에 추가하는 메인 메서드
     * - 지원서 번호 기준으로 묶음 번호 계산 (20개 단위)
     * - Apps Script 호출하여 묶음 문서 생성/추가
     * 
     * @param applicationNumber 지원서 번호 (학번 등)
     * @param individualDocId 개별 지원서 문서 ID
     * @return 묶음 작업 결과 정보
     * @author 채민
     */
    public BatchResult addApplicationToBatch(String applicationNumber, String individualDocId) {
        try {
            log.info("묶음 문서 추가 요청 시작: 지원서번호={}, 문서ID={}", applicationNumber, individualDocId);
            
            // 묶음 번호 계산 (20개 단위) - 채민
            int batchNumber = calculateBatchNumber(applicationNumber);
            
            // Apps Script 호출용 요청 데이터 생성 - 채민
            Map<String, Object> requestData = createRequestData(applicationNumber, individualDocId, batchNumber);
            
            // HTTP 요청 헤더 설정 - 채민
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestData, headers);
            
            // Apps Script 웹 앱 호출 - 채민
            ResponseEntity<String> response = restTemplate.exchange(
                webAppUrl, 
                HttpMethod.POST, 
                entity, 
                String.class
            );
            
            // 응답 처리 - 채민
            return parseResponse(response.getBody(), batchNumber);
            
        } catch (Exception e) {
            log.error("묶음 문서 추가 실패: 지원서번호={}, 오류={}", applicationNumber, e.getMessage(), e);
            return BatchResult.builder()
                    .batchNumber(calculateBatchNumber(applicationNumber))
                    .success(false)
                    .errorMessage("묶음 문서 추가 실패: " + e.getMessage())
                    .build();
        }
    }
    
    /**
     * 지원서 번호로 묶음 번호 계산
     * - 20개씩 묶어서 관리
     * - 예: 1~20 → 묶음1, 21~40 → 묶음2
     * 
     * @param applicationNumber 지원서 번호
     * @return 묶음 번호 (1부터 시작)
     * @author 채민
     */
    private int calculateBatchNumber(String applicationNumber) {
        try {
            // 학번에서 숫자 추출하여 묶음 번호 계산 - 채민
            int number = Integer.parseInt(applicationNumber.replaceAll("\\D", ""));
            return (number - 1) / 20 + 1;
        } catch (NumberFormatException e) {
            log.warn("지원서 번호에서 숫자 추출 실패: {}", applicationNumber);
            // 기본값으로 묶음1 반환 - 채민
            return 1;
        }
    }
    
    /**
     * Apps Script 호출용 요청 데이터 생성
     * 
     * @param applicationNumber 지원서 번호
     * @param individualDocId 개별 문서 ID
     * @param batchNumber 묶음 번호
     * @return 요청 데이터 Map
     * @author 채민
     */
    private Map<String, Object> createRequestData(String applicationNumber, String individualDocId, int batchNumber) {
        Map<String, Object> data = new HashMap<>();
        data.put("action", "addApplicationToBatch");
        data.put("applicationNumber", applicationNumber);
        data.put("individualDocId", individualDocId);
        data.put("batchNumber", batchNumber);
        
        log.debug("Apps Script 요청 데이터: {}", data);
        return data;
    }
    
    /**
     * Apps Script 응답 파싱
     * - JSON 형태의 응답을 BatchResult 객체로 변환
     * 
     * @param responseBody Apps Script 응답 JSON
     * @param batchNumber 묶음 번호
     * @return 파싱된 결과 객체
     * @author 채민
     */
    private BatchResult parseResponse(String responseBody, int batchNumber) {
        try {
            log.debug("Apps Script 응답: {}", responseBody);
            
            @SuppressWarnings("unchecked")
            Map<String, Object> responseMap = objectMapper.readValue(responseBody, Map.class);
            
            boolean success = Boolean.TRUE.equals(responseMap.get("success"));
            String batchDocId = (String) responseMap.get("batchDocId");
            String url = (String) responseMap.get("url");
            String errorMessage = (String) responseMap.get("errorMessage");
            
            BatchResult result = BatchResult.builder()
                    .batchNumber(batchNumber)
                    .batchDocId(batchDocId)
                    .url(url)
                    .success(success)
                    .errorMessage(errorMessage)
                    .build();
            
            log.info("묶음 문서 작업 완료: 성공={}, 묶음번호={}, 문서ID={}", 
                    success, batchNumber, batchDocId);
            
            return result;
            
        } catch (Exception e) {
            log.error("Apps Script 응답 파싱 실패: {}", e.getMessage(), e);
            return BatchResult.builder()
                    .batchNumber(batchNumber)
                    .success(false)
                    .errorMessage("응답 파싱 실패: " + e.getMessage())
                    .build();
        }
    }
}