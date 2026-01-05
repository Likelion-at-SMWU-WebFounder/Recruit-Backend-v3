package com.smlikelion.webfounder.Recruit.Entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SendStatus {
    PENDING("대기"),
    SUCCESS("성공"),
    FAIL("실패");

    private final String description;
}
