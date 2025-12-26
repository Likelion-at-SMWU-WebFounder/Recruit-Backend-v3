# Google Docs 지원서 내보내기 기능 구현

Spring Boot 백엔드에서 지원서 데이터를 Google Docs로 내보내는 기능입니다. 개별 문서 생성 + 20개씩 묶음 문서로 동적 추가하는 방식으로 구현되었습니다.

**구현자: 채민**

## 🎯 주요 기능

- ✅ **개별 지원서 문서 생성**: Google Docs 템플릿 기반으로 지원자별 개별 문서 생성
- ✅ **플레이스홀더 치환**: 템플릿의 {{변수명}} 형태를 실제 지원자 데이터로 자동 치환
- ✅ **20개 단위 묶음 관리**: 지원서를 20개씩 묶어서 별도 문서로 관리
- ✅ **Google Apps Script 연동**: 서버리스 방식으로 묶음 문서 생성/관리
- ✅ **기존 코드 최소 수정**: 기존 프로젝트 구조 유지하면서 새 기능 추가

## 📁 프로젝트 구조

### 새로 추가된 파일들

```
src/main/java/com/smlikelion/webfounder/Recruit/
├── Service/
│   ├── GoogleDocsExportService.java     # 개별 문서 생성 서비스
│   └── GoogleAppsScriptService.java     # Apps Script 호출 서비스
└── Dto/Response/
    └── BatchResult.java                 # 묶음 작업 결과 DTO

src/main/resources/
└── application.yml                      # 새로운 설정 추가

docs/
├── GoogleAppsScript.js                  # Google Apps Script 코드
└── README.md                           # 이 문서
```

### 수정된 파일들

- `build.gradle`: Google Drive API 의존성 추가
- `WebConfig.java`: RestTemplate Bean 추가
- `RecruitService.java`: 새로운 메서드 추가
- `RecruitController.java`: 새로운 엔드포인트 추가

## 🔧 설정 방법

### 1. 환경변수 설정

```bash
# 기존 환경변수 (이미 설정되어 있음)
export GOOGLE_DOCS_DOCUMENT_ID="기존_문서_ID"

# 새로 추가할 환경변수 - 최종 설정 완료 - 채민
export GOOGLE_DOCS_TEMPLATE_ID="10hMvydmXTPxMamIna-Pcoap31EtXuMeiCW3dzGEL1BA"
export GOOGLE_DRIVE_OUTPUT_FOLDER_ID="1USmjEqDlz9pDKw_38Z3hg0okiVXZEm85"  # 묶음 문서 저장 폴더
export GOOGLE_DRIVE_INDIVIDUAL_FOLDER_ID="1IvAmFrumfMPeTLmrzOq-leKDL7hWFwGf"  # 개별 지원서 저장 폴더
export GOOGLE_APPS_SCRIPT_WEB_APP_URL="https://script.google.com/a/macros/sookmyung.ac.kr/s/AKfycbw4ZSD32Oqd_7JHORekW3AI1gpHrzSZY0vO7HA2UCGlb_wrQPlPoBvo-0_f6Onz38yR/exec"
```

### 2. Google Apps Script 배포

1. [Google Apps Script](https://script.google.com/) 접속
2. 새 프로젝트 생성
3. `docs/GoogleAppsScript.js` 내용을 Code.gs에 복사
4. CONFIG 상수의 폴더 ID들을 실제 값으로 수정
5. 배포 → 새 배포 → 웹 앱으로 배포
6. 생성된 웹 앱 URL을 환경변수에 설정

### 3. Google Drive 폴더 구조

```
지원서 관리/
├── 개별 지원서/          # GOOGLE_DRIVE_INDIVIDUAL_FOLDER_ID
│   ├── 지원서_12345_김철수.docx
│   ├── 지원서_12346_이영희.docx
│   └── ...
└── 묶음 문서/            # GOOGLE_DRIVE_OUTPUT_FOLDER_ID
    ├── 지원서_묶음_1.docx  (1~20번 지원서)
    ├── 지원서_묶음_2.docx  (21~40번 지원서)
    └── ...
```

## 🚀 API 엔드포인트

### 1. 개별 문서 생성 + 묶음 추가 (신규)

```http
POST /api/recruit/docs/export?track=be
Content-Type: application/json

{
  "studentInfo": { ... },
  "answerList": { ... },
  "interview_time": { ... }
}
```

**응답:**
```json
{
  "success": true,
  "data": {
    "batchDocId": "1BxiM...VdaI",
    "batchNumber": 1,
    "url": "https://docs.google.com/document/d/1BxiM...VdaI/edit",
    "success": true,
    "errorMessage": null
  }
}
```

### 2. 지원서 제출 + 문서 내보내기 통합 (신규)

```http
POST /api/recruit/docs/submit-with-export?track=be
Content-Type: application/json

{
  "studentInfo": { ... },
  "answerList": { ... },
  "interview_time": { ... }
}
```

기존 지원서 등록 로직 + Google Docs 내보내기를 한 번에 처리합니다.

### 3. 기존 엔드포인트 (변경 없음)

- `POST /api/recruit/docs` - 기존 방식 유지
- `GET /api/recruit/docs/{joinerId}` - 기존 방식 유지
- `POST /api/recruit/docs/upload` - 기존 방식 유지

## 📋 플레이스홀더 매핑

Google Docs 템플릿에서 사용할 수 있는 플레이스홀더:

| 플레이스홀더 | 매핑 데이터 | 예시 |
|-------------|------------|------|
| `{{applicationNumber}}` | 학번 | 12345 |
| `{{num}}` | 학번 | 12345 |
| `{{sid}}` | 학번 | 12345 |
| `{{email}}` | 이메일 | student@example.com |
| `{{major}}` | 전공 | 컴퓨터학부 |
| `{{sem}}` | 수료학기 | 6 |
| `{{status}}` | 재/휴학 상태 | ENROLLED |
| `{{gradYr}}` | 졸업년도 | 2024 |
| `{{prog}}` | 프로그래머스 수강 | ENROLLED |
| `{{answer1}}` ~ `{{answer6}}` | 지원서 답변 | 각 문항별 답변 |
| `{{trackSelection}}` | 트랙 선택 | ☑ BE  ☐ FE  ☐ PM |
| `{{portfolio}}` | 포트폴리오 링크 | https://github.com/... |
| `{{interviewTimes}}` | 면접 가능 시간 | 2024-01-15: 14:00<br>2024-01-16: 16:00 |

## 🔄 처리 플로우

### 신규 내보내기 플로우

1. **개별 문서 생성** (`GoogleDocsExportService`)
   - 템플릿 문서를 복사하여 새 문서 생성
   - 파일명: "지원서_{학번}_{이름}"
   - 플레이스홀더를 실제 데이터로 치환

2. **묶음 번호 계산** (`GoogleAppsScriptService`)
   - 학번 기준으로 20개 단위 묶음 번호 계산
   - 예: 1~20번 → 묶음1, 21~40번 → 묶음2

3. **Apps Script 호출**
   - HTTP POST로 웹 앱 호출
   - 개별 문서를 묶음 문서에 추가

4. **묶음 문서 관리** (Apps Script)
   - 묶음 첫 번째면 새 문서 생성
   - 기존 묶음이면 내용 추가
   - 구분선과 함께 내용 복사

## 🎨 템플릿 예시

```
지원서 템플릿

학번: {{applicationNumber}}
이메일: {{email}}
전공: {{major}}

트랙 선택: {{trackSelection}}

문항 1 답변:
{{answer1}}

문항 2 답변:
{{answer2}}

...

면접 가능 시간:
{{interviewTimes}}
```

## 🔍 로그 및 모니터링

- Spring Boot 로그: 각 단계별 성공/실패 로깅
- Apps Script 로그: Google Cloud Console에서 확인
- 오류 발생 시 상세 오류 메시지 반환

## ⚠️ 주의사항

1. **환경변수 설정 필수**: 모든 Google 관련 ID와 URL 설정 필요
2. **권한 설정**: Apps Script에서 Drive, Docs API 권한 승인 필요
3. **폴더 구조**: Google Drive에서 미리 폴더 구조 생성 필요
4. **템플릿 준비**: 플레이스홀더가 포함된 Google Docs 템플릿 준비
5. **기존 기능 유지**: 기존 지원서 등록 로직은 그대로 유지됨

## 🚀 배포 후 테스트

### 1. 환경변수 설정 확인
모든 환경변수가 올바르게 설정되었는지 확인:
```bash
echo $GOOGLE_DOCS_TEMPLATE_ID
echo $GOOGLE_DRIVE_OUTPUT_FOLDER_ID  
echo $GOOGLE_DRIVE_INDIVIDUAL_FOLDER_ID
echo $GOOGLE_APPS_SCRIPT_WEB_APP_URL
```

### 2. 테스트 요청 예시
```bash
curl -X POST "http://localhost:8080/api/recruit/docs/export?track=be" \
-H "Content-Type: application/json" \
-d '{
  "studentInfo": {
    "name": "테스트학생",
    "studentId": "12345",
    "email": "test@sookmyung.ac.kr",
    "major": "컴퓨터학부",
    "track": "BE",
    "phoneNumber": "010-1234-5678",
    "completedSem": 6,
    "portfolio": "https://github.com/test",
    "schoolStatus": "ENROLLED",
    "programmers": "ENROLLED",
    "graduatedYear": "2024",
    "agreeToTerms": true,
    "agreeToEventParticipation": true
  },
  "answerList": {
    "A1": "테스트 답변 1",
    "A2": "테스트 답변 2", 
    "A3": "테스트 답변 3",
    "A4": "테스트 답변 4",
    "A5": "테스트 답변 5",
    "A6": "테스트 답변 6",
    "A7": "테스트 답변 7"
  },
  "interview_time": {
    "2024-01-15": "14:00",
    "2024-01-16": "16:00"
  }
}'
```

### 3. 확인 사항
✅ 개별 지원서 문서가 생성되었는지 확인: [개별 폴더](https://drive.google.com/drive/folders/1IvAmFrumfMPeTLmrzOq-leKDL7hWFwGf)  
✅ 묶음 문서가 생성되었는지 확인: [묶음 폴더](https://drive.google.com/drive/folders/1USmjEqDlz9pDKw_38Z3hg0okiVXZEm85)  
✅ 플레이스홀더가 올바르게 치환되었는지 확인  
✅ 묶음 문서에 구분선과 함께 내용이 추가되었는지 확인  

### 4. 로그 확인
- Spring Boot 로그에서 성공/실패 메시지 확인
- Google Apps Script 실행 로그는 [Google Cloud Console](https://console.cloud.google.com/)에서 확인

---

**문의사항이나 이슈가 있을 경우 저(챔)에게 연락하세요.**