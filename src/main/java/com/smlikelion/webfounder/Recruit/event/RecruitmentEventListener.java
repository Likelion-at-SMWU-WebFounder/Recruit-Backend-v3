package com.smlikelion.webfounder.Recruit.event;

import com.smlikelion.webfounder.Recruit.Service.MailService;
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
    private final MailService mailService;

    // 1. 구글 독스 업로드
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleRecruitmentAppliedEvent(RecruitmentAppliedEvent event){
        recruitService.uploadToGoogleDocs(
                event.getDocumentId(),
                event.getApplicationId(),
                event.getRequest()
        );
    }

    // 2. 메일 전송
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleMailSend(RecruitmentAppliedEvent event) {
        mailService.sendApplyStatusMail(event.getEmail());
    }
}
