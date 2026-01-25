package com.smlikelion.webfounder.Recruit.Entity;

public enum SchoolStatus {
    ENROLLED("enrolled", "재학"),
    ON_LEAVE("on_leave", "휴학"),
    DEFERRED_GRADUATION("deferred_graduation", "졸업유예");

    private final String status;
    private final String label;

    SchoolStatus(String status, String label) {
        this.status = status;
        this.label = label;
    }

    public String getStatus() {
        return status;
    }

    public String getLabel() { return label; }

    // 상태명이 유효하면 상태 형태로 반환
    // 유효하지 않으면 null로 반환
    public static SchoolStatus getStatusByName(String status) {
        for (SchoolStatus schoolStatus : SchoolStatus.values()) {
            if (schoolStatus.status.equalsIgnoreCase(status)) {
                return schoolStatus;
            }
        }
        return null;
    }
}
