package com.smlikelion.webfounder.Recruit.Service.docs;

import com.google.api.services.docs.v1.model.*;
import org.springframework.stereotype.Component;

@Component
public class DocsRequests {

    public String checkbox(String label, boolean checked) {
        return (checked ? "☑ " : "☐ ") + label;
    }

    public String safe(String s) {
        return s == null ? "" : s;
    }

    public Request insertAtEnd(String text) {
        return new Request().setInsertText(
                new InsertTextRequest()
                        .setText(text)
                        .setEndOfSegmentLocation(new EndOfSegmentLocation())
        );
    }

    public Request insertAtIndex(String text, int index) {
        return new Request().setInsertText(
                new InsertTextRequest()
                        .setText(text)
                        .setLocation(new Location().setIndex(index))
        );
    }

    // 문단 수준(제목) 설정 -> 구글 독스 목차에 표시
    public Request applyHeading(int s, int e, String type) {
        return new Request().setUpdateParagraphStyle(
                new UpdateParagraphStyleRequest()
                        .setRange(new Range().setStartIndex(s).setEndIndex(e))
                        .setParagraphStyle(new ParagraphStyle().setNamedStyleType(type))
                        .setFields("namedStyleType")
        );
    }

    // 문단 정렬 방향 (START, CENTER, END, JUSTIFIED)
    public Request applyParagraphAlign(int s, int e, String align) {
        return new Request().setUpdateParagraphStyle(
                new UpdateParagraphStyleRequest()
                        .setRange(new Range().setStartIndex(s).setEndIndex(e))
                        .setParagraphStyle(new ParagraphStyle().setAlignment(align))
                        .setFields("alignment")
        );
    }

    // 들여쓰기
    public Request applyParagraphIndent(int s, int e, double pt) {
        return new Request().setUpdateParagraphStyle(
                new UpdateParagraphStyleRequest()
                        .setRange(new Range().setStartIndex(s).setEndIndex(e))
                        .setParagraphStyle(
                                new ParagraphStyle().setIndentStart(
                                        new Dimension().setMagnitude(pt).setUnit("PT")
                                )
                        )
                        .setFields("indentStart")
        );
    }

    // 텍스트 스타일 적용
    public Request applyTextStyle(
            int s, int e,
            boolean bold,
            double fontSize,
            Float r, Float g, Float b
    ) {
        TextStyle ts = new TextStyle()
                .setBold(bold)
                .setFontSize(new Dimension().setMagnitude(fontSize).setUnit("PT"));

        String fields = "bold,fontSize";

        if (r != null) {
            ts.setForegroundColor(new OptionalColor().setColor(
                    new Color().setRgbColor(new RgbColor().setRed(r).setGreen(g).setBlue(b))
            ));
            fields += ",foregroundColor";
        }

        return new Request().setUpdateTextStyle(
                new UpdateTextStyleRequest()
                        .setRange(new Range().setStartIndex(s).setEndIndex(e))
                        .setTextStyle(ts)
                        .setFields(fields)
        );
    }

    // 페이지 나누기
    public Request insertPageBreakAtEnd() {
        return new Request().setInsertPageBreak(
                new InsertPageBreakRequest().setEndOfSegmentLocation(new EndOfSegmentLocation())
        );
    }

    // 표 생성
    public Request insertTableAtEnd(int rows, int cols) {
        return new Request().setInsertTable(
                new InsertTableRequest()
                        .setRows(rows)
                        .setColumns(cols)
                        .setEndOfSegmentLocation(new EndOfSegmentLocation())
        );
    }
}
