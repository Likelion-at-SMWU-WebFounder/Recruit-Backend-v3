package com.smlikelion.webfounder.Recruit.event;

import com.smlikelion.webfounder.Recruit.Dto.Request.RecruitmentRequest;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RecruitmentAppliedEvent {
    private final String documentId;
    private final Long applicationId;
    private final RecruitmentRequest request;
}
