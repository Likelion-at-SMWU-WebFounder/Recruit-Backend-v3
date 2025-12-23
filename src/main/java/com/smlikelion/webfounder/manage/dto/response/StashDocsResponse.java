package com.smlikelion.webfounder.manage.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
public class StashDocsResponse {
    private final int requested;
    private final int stashed;
    private final List<Long> failed;

    @Builder
    public StashDocsResponse(int requested, int stashed, List<Long> failed){
        this.requested = requested;
        this.stashed = stashed;
        this.failed = failed;
    }
}