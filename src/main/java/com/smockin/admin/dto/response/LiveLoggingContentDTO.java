package com.smockin.admin.dto.response;

import lombok.Getter;

import java.util.Map;

@Getter
public abstract class LiveLoggingContentDTO {

    private final String url;
    private final Map<String, String> headers;
    private final String body;

    protected LiveLoggingContentDTO(final String url, final Map<String, String> headers, final String body) {
        this.url = url;
        this.headers = headers;
        this.body = body;
    }

}

