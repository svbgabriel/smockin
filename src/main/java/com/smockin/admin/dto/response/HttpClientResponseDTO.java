package com.smockin.admin.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by mgallina on 02/08/17.
 */
@Getter
@Setter
public class HttpClientResponseDTO {

    private int status;
    private String contentType;
    private Map<String, String> headers = new HashMap<>();
    private String body;

    public HttpClientResponseDTO() {

    }

    public HttpClientResponseDTO(final int status) {
        this.status = status;
    }

    public HttpClientResponseDTO(final int status, final String contentType, final Map<String, String> headers, final String body) {
        this.status = status;
        this.contentType = contentType;
        this.headers = headers;
        this.body = body;
    }

}
