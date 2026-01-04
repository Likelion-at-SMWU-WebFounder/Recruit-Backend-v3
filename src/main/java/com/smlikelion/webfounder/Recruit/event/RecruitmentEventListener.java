package com.smlikelion.webfounder.Recruit.event;

import com.smlikelion.webfounder.Recruit.Service.RecruitService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class RecruitmentEventListener {
    private final RecruitService recruitService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleRecruitmentAppliedEvent(RecruitmentAppliedEvent event){
        recruitService.uploadToGoogleDocs(
                event.getDocumentId(),
                event.getApplicationId(),
                event.getRequest()
        );
    }
}
