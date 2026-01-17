package com.smlikelion.webfounder.Recruit.event;

import com.smlikelion.webfounder.Recruit.Entity.SendStatus;
import com.smlikelion.webfounder.Recruit.Service.MailService;
import com.smlikelion.webfounder.Recruit.Service.RecruitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class RecruitmentEventListener {
    private final RecruitService recruitService;
    private final MailService mailService;

    // 1. 구글 독스 업로드
    @Async("taskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleRecruitmentAppliedEvent(RecruitmentAppliedEvent event){
        try {
            recruitService.uploadToGoogleDocs(
                    event.getDocumentId(),
                    event.getApplicationId(),
                    event.getRequest()
            );
            // 상태를 SUCCESS로 변경
            recruitService.updateStatus("GOOGLE_DOCS", event.getApplicationId(), SendStatus.SUCCESS);
        } catch (Exception e) {
            recruitService.updateStatus("GOOGLE_DOCS", event.getApplicationId(), SendStatus.FAIL);
            log.error("구글 독스 업로드 실패 - joinerId:{}, 원인:{}", event.getApplicationId(), e.getMessage(), e);
        }

    }

    // 2. 메일 전송
    @Async("taskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleMailSend(RecruitmentAppliedEvent event) {
        try {
            mailService.sendApplyStatusMail(event.getEmail());
            // 상태를 SUCCESS로 변경
            recruitService.updateStatus("MAIL", event.getApplicationId(), SendStatus.SUCCESS);
        } catch (Exception e) {
            // 상태를 FAIL로 변경
            recruitService.updateStatus("MAIL", event.getApplicationId(), SendStatus.FAIL);
            log.error("메일 전송 실패 - joinerId: {}, 원인:{}", event.getApplicationId(), e.getMessage(), e);
        }
    }
}
