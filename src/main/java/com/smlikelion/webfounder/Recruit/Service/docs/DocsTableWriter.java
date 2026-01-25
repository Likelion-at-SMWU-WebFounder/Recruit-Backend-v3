package com.smlikelion.webfounder.Recruit.Service.docs;

import com.google.api.services.docs.v1.Docs;
import com.google.api.services.docs.v1.model.*;
import com.smlikelion.webfounder.Recruit.Dto.Request.RecruitmentRequest;
import com.smlikelion.webfounder.Recruit.Entity.SchoolStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DocsTableWriter {

    private final Docs docsService;

    public TableRef findLastTableRef(String docId) throws IOException {
        Document doc = docsService.documents().get(docId).execute();
        StructuralElement last = null;
        for (StructuralElement el : doc.getBody().getContent()) {
            if (el.getTable() != null) last = el;
        }
        return last == null ? null : new TableRef(last.getStartIndex(), last.getTable());
    }

    public void fillAndStyleTable(String documentId, TableRef ref, long applicationId, RecruitmentRequest req) throws IOException {
        List<String> headers = List.of(
                "번호", "학번", "이메일", "전공",
                "수료 학기", "재/휴학", "졸업연도", "프로그래머스"
        );

        List<String> values = List.of(
                String.valueOf(applicationId),
                req.getStudentInfo().getStudentId(),
                req.getStudentInfo().getEmail(),
                req.getStudentInfo().getMajor(),
                String.valueOf(req.getStudentInfo().getCompletedSem()),
                SchoolStatus.valueOf(req.getStudentInfo().getSchoolStatus()).getLabel(),
                req.getStudentInfo().getGraduatedYear(),
                (req.getStudentInfo().getProgrammers().equals("NOT_ENROLLED")) ? "X" : "O"
        );

        List<Request> requests = new ArrayList<>();

        // 끝에서부터 표 채우기
        for (int r = ref.table.getRows() - 1; r >= 0; r--) {
            TableRow row = ref.table.getTableRows().get(r);
            List<String> contents = (r == 1 ? values : headers);

            for (int c = ref.table.getColumns() - 1; c >= 0; c--) {
                TableCell cell = row.getTableCells().get(c);

                int cellStartIndex = cell.getStartIndex() + 1;
                String content = contents.get(c);
                int start = cell.getStartIndex() + 1;
                int end = start + content.length();

                requests.add(new Request().setInsertText(new InsertTextRequest()
                        .setText(content)
                        .setLocation(new Location().setIndex(cellStartIndex))));

                boolean isHeader = (r == 0);
                TextStyle ts = new TextStyle()
                        .setBold(isHeader)
                        .setFontSize(new Dimension().setMagnitude(11.0).setUnit("PT"));

                requests.add(new Request().setUpdateTextStyle(
                        new UpdateTextStyleRequest()
                                .setRange(new Range().setStartIndex(start).setEndIndex(end))
                                .setTextStyle(ts)
                                .setFields("bold,fontSize")
                ));

                requests.add(new Request().setUpdateParagraphStyle(
                        new UpdateParagraphStyleRequest()
                                .setRange(new Range().setStartIndex(start).setEndIndex(end))
                                .setParagraphStyle(new ParagraphStyle().setAlignment("CENTER"))
                                .setFields("alignment")
                ));
            }
        }

        OptionalColor beige = new OptionalColor().setColor(
                new Color().setRgbColor(
                        new RgbColor().setRed(0.96f).setGreen(0.90f).setBlue(0.82f)
                )
        );

        for (int c = 0; c < 8; c++) {
            requests.add(new Request().setUpdateTableCellStyle(
                    new UpdateTableCellStyleRequest()
                            .setTableRange(new TableRange()
                                    .setTableCellLocation(new TableCellLocation()
                                            .setTableStartLocation(new Location().setIndex(ref.startIndex))
                                            .setRowIndex(0)
                                            .setColumnIndex(c))
                                    .setRowSpan(1)
                                    .setColumnSpan(1))
                            .setTableCellStyle(new TableCellStyle().setBackgroundColor(beige))
                            .setFields("backgroundColor")
            ));
        }

        docsService.documents().batchUpdate(documentId,
                new BatchUpdateDocumentRequest().setRequests(requests)).execute();
    }

    // 표 정보 객체
    public static class TableRef {
        public final int startIndex;
        public final Table table;

        public TableRef(int startIndex, Table table) {
            this.startIndex = startIndex;
            this.table = table;
        }
    }
}
