package com.smlikelion.webfounder.Recruit.Dto.Response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;

import java.time.OffsetDateTime;

@Getter
public class TimeResponse {

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX", timezone = "Asia/Seoul")
    private OffsetDateTime serverTime;

    public TimeResponse(OffsetDateTime serverTime) {
        this.serverTime = serverTime;
    }
}
