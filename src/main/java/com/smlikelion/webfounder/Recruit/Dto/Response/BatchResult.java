package com.smlikelion.webfounder.Recruit.Dto.Response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Google Apps Script에서 묶음 문서 생성 결과를 담는 DTO
 * - 20개씩 묶어서 생성되는 배치 문서의 정보를 반환
 * 
 * @author 채민
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchResult {
    
    /**
     * 생성된 묶음 문서의 Google Docs ID
     * - 해당 ID로 문서 접근 가능
     * 
     * @author 채민
     */
    private String batchDocId;
    
    /**
     * 묶음 번호 (1, 2, 3, ...)
     * - 지원서 번호를 20으로 나눈 몫 + 1
     * - 예: 지원서 1~20번 → 묶음1, 21~40번 → 묶음2
     * 
     * @author 채민
     */
    private int batchNumber;
    
    /**
     * 생성된 문서의 URL
     * - Google Docs 웹에서 직접 열 수 있는 링크
     * 
     * @author 채민
     */
    private String url;
    
    /**
     * 작업 성공 여부
     * - true: 성공적으로 묶음 문서에 추가됨
     * - false: 오류 발생
     * 
     * @author 채민
     */
    private boolean success;
    
    /**
     * 오류 메시지 (실패 시)
     * - success가 false일 때 오류 상세 내용
     * 
     * @author 채민
     */
    private String errorMessage;
}