package com.smlikelion.webfounder.manage.dto.request;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotEmpty;
import java.util.List;

@Getter
@NoArgsConstructor
public class StashDocsRequest{

    @NotEmpty(message = "stash할 joinerId 목록은 비어 있을 수 없습니다.")
    private List<Long> joinerIds;

    @Builder
    public StashDocsRequest(List<Long> joinerIds) {
        this.joinerIds = joinerIds;
    }
}
