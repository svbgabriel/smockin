package com.smockin.admin.dto.response;

import lombok.Getter;

import java.util.Map;

@Getter
public class LiveLoggingOutboundContentDTO extends LiveLoggingContentDTO {

    private final Integer status;

    public LiveLoggingOutboundContentDTO(final String url, final Map<String, String> headers, final String body, final Integer status) {
        super(url, headers, body);
        this.status = status;
    }

}

