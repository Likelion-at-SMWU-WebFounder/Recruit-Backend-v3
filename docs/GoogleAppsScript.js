/**
 * Google Apps Script - 지원서 묶음 문서 관리
 * - Spring Boot에서 호출되는 웹 앱
 * - 개별 지원서를 20개씩 묶음 문서로 관리
 *
 * @author 채민
 * @modified Claude (버그 수정 및 서식 복사 개선)
 */

// 설정 상수 - 최종 설정 완료 - 채민
const CONFIG = {
    OUTPUT_FOLDER_ID: '1USmjEqDlz9pDKw_38Z3hg0okiVXZEm85', // 묶음 문서 저장 폴더
    INDIVIDUAL_FOLDER_ID: '1IvAmFrumfMPeTLmrzOq-leKDL7hWFwGf', // 개별 지원서 저장 폴더
    BATCH_SIZE: 20
};

/**
 * 메인 엔트리 포인트 - HTTP POST 요청 처리
 * Spring Boot에서 호출하는 doPost 함수
 *
 * @param {Event} e POST 요청 이벤트 객체
 * @return {Object} JSON 응답 (success, batchDocId, url, errorMessage)
 */
function doPost(e) {
    try {
        const requestData = JSON.parse(e.postData.contents);

        if (!requestData.action || !requestData.applicationNumber || !requestData.individualDocId) {
            return createErrorResponse('필수 파라미터가 누락되었습니다.');
        }

        switch (requestData.action) {
            case 'addApplicationToBatch':
                return addApplicationToBatch(
                    requestData.applicationNumber,
                    requestData.individualDocId,
                    requestData.batchNumber
                );
            default:
                return createErrorResponse('알 수 없는 action: ' + requestData.action);
        }

    } catch (error) {
        console.error('doPost 처리 중 오류:', error);
        return createErrorResponse('요청 처리 실패: ' + error.message);
    }
}

/**
 * 개별 지원서를 묶음 문서에 추가하는 메인 함수
 *
 * @param {string} applicationNumber 지원서 번호 (학번)
 * @param {string} individualDocId 개별 지원서 문서 ID
 * @param {number} batchNumber 묶음 번호 (1, 2, 3, ...)
 * @return {Object} JSON 응답
 */
function addApplicationToBatch(applicationNumber, individualDocId, batchNumber) {
    try {
        console.log(`묶음 문서 추가 시작: 지원서=${applicationNumber}, 묶음=${batchNumber}`);

        const batchDocId = findOrCreateBatchDocument(batchNumber);
        copyDocumentContent(individualDocId, batchDocId, applicationNumber);

        const url = `https://docs.google.com/document/d/${batchDocId}/edit`;

        console.log(`묶음 문서 추가 완료: 문서ID=${batchDocId}`);
        return createSuccessResponse(batchDocId, batchNumber, url);

    } catch (error) {
        console.error('묶음 문서 추가 중 오류:', error);
        return createErrorResponse('묶음 문서 추가 실패: ' + error.message);
    }
}

/**
 * 묶음 문서를 찾거나 새로 생성
 * - 동시성 제어를 위한 Lock 사용
 *
 * @param {number} batchNumber 묶음 번호
 * @return {string} 묶음 문서 ID
 */
function findOrCreateBatchDocument(batchNumber) {
    const batchFileName = `지원서_묶음_${batchNumber}`;
    const folder = DriveApp.getFolderById(CONFIG.OUTPUT_FOLDER_ID);

    // Lock으로 동시성 제어
    const lock = LockService.getScriptLock();

    try {
        lock.waitLock(30000); // 최대 30초 대기

        const existingFiles = folder.getFilesByName(batchFileName);

        if (existingFiles.hasNext()) {
            const existingFile = existingFiles.next();
            console.log(`기존 묶음 문서 발견: ${batchFileName}`);
            return existingFile.getId();
        } else {
            const newDoc = DocumentApp.create(batchFileName);
            const newFile = DriveApp.getFileById(newDoc.getId());

            folder.addFile(newFile);
            DriveApp.getRootFolder().removeFile(newFile);

            const body = newDoc.getBody();
            body.clear();
            body.appendParagraph(`지원서 묶음 ${batchNumber}`)
                .setHeading(DocumentApp.ParagraphHeading.TITLE);
            body.appendParagraph(`생성일시: ${new Date().toLocaleString('ko-KR')}`)
                .setHeading(DocumentApp.ParagraphHeading.SUBTITLE);
            body.appendHorizontalRule();

            newDoc.saveAndClose();

            console.log(`새 묶음 문서 생성: ${batchFileName}, ID=${newDoc.getId()}`);
            return newDoc.getId();
        }

    } finally {
        lock.releaseLock();
    }
}

/**
 * 개별 문서의 모든 내용을 묶음 문서에 복사
 * - 표, 문단, 리스트 등 모든 요소 복사
 * - 서식(배경색, 정렬 등) 유지하며 복사
 *
 * @param {string} sourceDocId 원본 문서 ID (개별 지원서)
 * @param {string} targetDocId 대상 문서 ID (묶음 문서)
 * @param {string} applicationNumber 지원서 번호
 */
function copyDocumentContent(sourceDocId, targetDocId, applicationNumber) {
    try {
        const sourceDoc = DocumentApp.openById(sourceDocId);
        const targetDoc = DocumentApp.openById(targetDocId);
        const targetBody = targetDoc.getBody();

        // 지원서 구분 헤더 추가
        targetBody.appendParagraph(`\n=== 지원서 ${applicationNumber} ===`)
            .setHeading(DocumentApp.ParagraphHeading.HEADING2);

        const sourceBody = sourceDoc.getBody();
        const numChildren = sourceBody.getNumChildren();

        for (let i = 0; i < numChildren; i++) {
            const element = sourceBody.getChild(i);
            const elementType = element.getType();

            switch (elementType) {
                case DocumentApp.ElementType.PARAGRAPH:
                    copyParagraph(element.asParagraph(), targetBody);
                    break;
                case DocumentApp.ElementType.TABLE:
                    copyTable(element.asTable(), targetBody);
                    break;
                case DocumentApp.ElementType.LIST_ITEM:
                    copyListItem(element.asListItem(), targetBody);
                    break;
                case DocumentApp.ElementType.HORIZONTAL_RULE:
                    targetBody.appendHorizontalRule();
                    break;
                default:
                    try {
                        targetBody.appendParagraph(element.asText().getText());
                    } catch (e) {
                        console.log('복사 불가능한 요소 타입:', elementType);
                    }
            }
        }

        // 지원서 끝 구분선 추가
        targetBody.appendHorizontalRule();

        targetDoc.saveAndClose();
        sourceDoc.saveAndClose();

        console.log(`문서 내용 복사 완료: ${sourceDocId} -> ${targetDocId}`);

    } catch (error) {
        console.error('문서 내용 복사 중 오류:', error);
        throw new Error('문서 내용 복사 실패: ' + error.message);
    }
}

/**
 * 문단 요소를 복사 (서식 완전 유지)
 * - 헤딩, 정렬, 텍스트 속성 모두 복사
 *
 * @param {Paragraph} sourceParagraph 원본 문단
 * @param {Body} targetBody 대상 문서 본문
 */
function copyParagraph(sourceParagraph, targetBody) {
    const text = sourceParagraph.getText();

    if (text.trim() === '') {
        targetBody.appendParagraph('');
        return;
    }

    const newParagraph = targetBody.appendParagraph(text);

    // 헤딩 스타일 복사
    try {
        newParagraph.setHeading(sourceParagraph.getHeading());
    } catch (e) {
        console.log('헤딩 복사 실패:', e.message);
    }

    // 정렬 복사
    try {
        newParagraph.setAlignment(sourceParagraph.getAlignment());
    } catch (e) {
        console.log('정렬 복사 실패:', e.message);
    }

    // 들여쓰기 복사
    try {
        newParagraph.setIndentStart(sourceParagraph.getIndentStart());
        newParagraph.setIndentEnd(sourceParagraph.getIndentEnd());
        newParagraph.setIndentFirstLine(sourceParagraph.getIndentFirstLine());
    } catch (e) {
        console.log('들여쓰기 복사 실패:', e.message);
    }

    // 줄 간격 복사
    try {
        newParagraph.setLineSpacing(sourceParagraph.getLineSpacing());
        newParagraph.setSpacingBefore(sourceParagraph.getSpacingBefore());
        newParagraph.setSpacingAfter(sourceParagraph.getSpacingAfter());
    } catch (e) {
        console.log('줄 간격 복사 실패:', e.message);
    }

    // 텍스트 스타일 복사 (굵기, 색상, 폰트 등)
    copyTextStyles(sourceParagraph, newParagraph);
}

/**
 * 텍스트 스타일을 복사 (굵기, 기울임, 색상, 폰트 등)
 *
 * @param {Paragraph} sourceParagraph 원본 문단
 * @param {Paragraph} targetParagraph 대상 문단
 */
function copyTextStyles(sourceParagraph, targetParagraph) {
    try {
        const sourceText = sourceParagraph.editAsText();
        const targetText = targetParagraph.editAsText();
        const textLength = sourceParagraph.getText().length;

        if (textLength === 0) return;

        // 각 문자별로 스타일 복사 (성능상 전체 텍스트 단위로 처리)
        // 첫 번째 문자의 스타일을 전체에 적용 (단순화)
        try {
            const bold = sourceText.isBold(0);
            if (bold !== null) targetText.setBold(bold);
        } catch (e) {}

        try {
            const italic = sourceText.isItalic(0);
            if (italic !== null) targetText.setItalic(italic);
        } catch (e) {}

        try {
            const underline = sourceText.isUnderline(0);
            if (underline !== null) targetText.setUnderline(underline);
        } catch (e) {}

        try {
            const foregroundColor = sourceText.getForegroundColor(0);
            if (foregroundColor) targetText.setForegroundColor(foregroundColor);
        } catch (e) {}

        try {
            const backgroundColor = sourceText.getBackgroundColor(0);
            if (backgroundColor) targetText.setBackgroundColor(backgroundColor);
        } catch (e) {}

        try {
            const fontSize = sourceText.getFontSize(0);
            if (fontSize) targetText.setFontSize(fontSize);
        } catch (e) {}

        try {
            const fontFamily = sourceText.getFontFamily(0);
            if (fontFamily) targetText.setFontFamily(fontFamily);
        } catch (e) {}

    } catch (error) {
        console.log('텍스트 스타일 복사 실패:', error.message);
    }
}

/**
 * 표 요소를 복사 (전체 표 구조 + 셀 배경색 유지)
 * - 빈 행 추가 버그 수정
 * - 셀 배경색 복사 추가
 *
 * @param {Table} sourceTable 원본 표
 * @param {Body} targetBody 대상 문서 본문
 */
function copyTable(sourceTable, targetBody) {
    const numRows = sourceTable.getNumRows();

    if (numRows === 0) return;

    const firstRow = sourceTable.getRow(0);
    const numCols = firstRow.getNumCells();

    if (numCols === 0) return;

    // 첫 번째 행 데이터로 표 초기화 (빈 행 추가 버그 방지)
    const firstRowCells = [];
    for (let col = 0; col < numCols; col++) {
        firstRowCells.push(firstRow.getCell(col).getText());
    }

    const newTable = targetBody.appendTable([firstRowCells]);

    // 첫 번째 행 스타일 복사
    copyRowStyles(firstRow, newTable.getRow(0), numCols);

    // 나머지 행 추가 및 스타일 복사
    for (let row = 1; row < numRows; row++) {
        const sourceRow = sourceTable.getRow(row);
        const newRow = newTable.appendTableRow();

        for (let col = 0; col < numCols; col++) {
            const sourceCell = sourceRow.getCell(col);
            const newCell = newRow.appendTableCell(sourceCell.getText());

            // 셀 스타일 복사
            copyCellStyles(sourceCell, newCell);
        }
    }

    // 표 테두리 스타일 복사
    try {
        newTable.setBorderWidth(sourceTable.getBorderWidth());
        newTable.setBorderColor(sourceTable.getBorderColor());
    } catch (e) {
        console.log('표 테두리 스타일 복사 실패:', e.message);
    }
}

/**
 * 표 행의 스타일을 복사
 *
 * @param {TableRow} sourceRow 원본 행
 * @param {TableRow} targetRow 대상 행
 * @param {number} numCols 열 개수
 */
function copyRowStyles(sourceRow, targetRow, numCols) {
    for (let col = 0; col < numCols; col++) {
        const sourceCell = sourceRow.getCell(col);
        const targetCell = targetRow.getCell(col);

        copyCellStyles(sourceCell, targetCell);
    }
}

/**
 * 표 셀의 스타일을 복사 (배경색, 정렬 등)
 *
 * @param {TableCell} sourceCell 원본 셀
 * @param {TableCell} targetCell 대상 셀
 */
function copyCellStyles(sourceCell, targetCell) {
    // 배경색 복사
    try {
        const backgroundColor = sourceCell.getBackgroundColor();
        if (backgroundColor) {
            targetCell.setBackgroundColor(backgroundColor);
        }
    } catch (e) {
        console.log('셀 배경색 복사 실패:', e.message);
    }

    // 수직 정렬 복사
    try {
        targetCell.setVerticalAlignment(sourceCell.getVerticalAlignment());
    } catch (e) {
        console.log('셀 수직 정렬 복사 실패:', e.message);
    }

    // 셀 너비 복사
    try {
        const width = sourceCell.getWidth();
        if (width) {
            targetCell.setWidth(width);
        }
    } catch (e) {
        console.log('셀 너비 복사 실패:', e.message);
    }

    // 패딩 복사
    try {
        targetCell.setPaddingTop(sourceCell.getPaddingTop());
        targetCell.setPaddingBottom(sourceCell.getPaddingBottom());
        targetCell.setPaddingLeft(sourceCell.getPaddingLeft());
        targetCell.setPaddingRight(sourceCell.getPaddingRight());
    } catch (e) {
        console.log('셀 패딩 복사 실패:', e.message);
    }

    // 셀 내 텍스트 스타일 복사
    try {
        const sourceChild = sourceCell.getChild(0);
        const targetChild = targetCell.getChild(0);

        if (sourceChild && targetChild &&
            sourceChild.getType() === DocumentApp.ElementType.PARAGRAPH &&
            targetChild.getType() === DocumentApp.ElementType.PARAGRAPH) {

            const sourcePara = sourceChild.asParagraph();
            const targetPara = targetChild.asParagraph();

            // 정렬 복사
            try {
                targetPara.setAlignment(sourcePara.getAlignment());
            } catch (e) {}

            // 텍스트 스타일 복사
            copyTextStyles(sourcePara, targetPara);
        }
    } catch (e) {
        console.log('셀 내 텍스트 스타일 복사 실패:', e.message);
    }
}

/**
 * 리스트 항목을 복사 (들여쓰기 레벨 유지)
 *
 * @param {ListItem} sourceListItem 원본 리스트 항목
 * @param {Body} targetBody 대상 문서 본문
 */
function copyListItem(sourceListItem, targetBody) {
    const text = sourceListItem.getText();
    const newListItem = targetBody.appendListItem(text);

    // 리스트 타입 복사
    try {
        newListItem.setGlyphType(sourceListItem.getGlyphType());
    } catch (e) {
        console.log('리스트 타입 복사 실패:', e.message);
    }

    // 들여쓰기 레벨 복사
    try {
        newListItem.setNestingLevel(sourceListItem.getNestingLevel());
    } catch (e) {
        console.log('리스트 들여쓰기 복사 실패:', e.message);
    }

    // 정렬 복사
    try {
        newListItem.setAlignment(sourceListItem.getAlignment());
    } catch (e) {
        console.log('리스트 정렬 복사 실패:', e.message);
    }
}

/**
 * 성공 응답 객체 생성
 *
 * @param {string} batchDocId 묶음 문서 ID
 * @param {number} batchNumber 묶음 번호
 * @param {string} url 문서 URL
 * @return {Object} 성공 응답
 */
function createSuccessResponse(batchDocId, batchNumber, url) {
    return ContentService
        .createTextOutput(JSON.stringify({
            success: true,
            batchDocId: batchDocId,
            batchNumber: batchNumber,
            url: url,
            timestamp: new Date().toISOString()
        }))
        .setMimeType(ContentService.MimeType.JSON);
}

/**
 * 오류 응답 객체 생성
 *
 * @param {string} errorMessage 오류 메시지
 * @return {Object} 오류 응답
 */
function createErrorResponse(errorMessage) {
    return ContentService
        .createTextOutput(JSON.stringify({
            success: false,
            errorMessage: errorMessage,
            timestamp: new Date().toISOString()
        }))
        .setMimeType(ContentService.MimeType.JSON);
}

/**
 * 테스트용 함수 - Apps Script 에디터에서 직접 실행 가능
 */
function testAddApplicationToBatch() {
    const testData = {
        applicationNumber: 'TEST001',
        individualDocId: 'YOUR_TEST_DOCUMENT_ID',
        batchNumber: 1
    };

    try {
        const result = addApplicationToBatch(
            testData.applicationNumber,
            testData.individualDocId,
            testData.batchNumber
        );

        console.log('테스트 결과:', result.getContent());
    } catch (error) {
        console.error('테스트 실행 중 오류:', error);
    }
}

/**
 * 배포 정보
 *
 * 1. Google Apps Script 프로젝트 생성
 * 2. 이 코드를 Code.gs에 붙여넣기
 * 3. CONFIG 상수의 폴더 ID들을 실제 값으로 수정
 * 4. "배포" > "새 배포" > "웹 앱" 선택
 * 5. 실행 대상: "나", 액세스 권한: "모든 사용자"
 * 6. 배포 후 웹 앱 URL을 application.yml에 설정
 */