package com.cuet.sphere.response;

import com.cuet.sphere.model.Notice.NoticeType;
import lombok.Data;

@Data
public class NoticeRequest {
    private String message;
    private String attachment;
    private NoticeType noticeType;
}
