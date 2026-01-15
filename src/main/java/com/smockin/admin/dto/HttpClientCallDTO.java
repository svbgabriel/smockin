package com.smockin.admin.dto;

import com.smockin.admin.persistence.enums.RestMethodEnum;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by mgallina.
 */
@Setter
@Getter
public class HttpClientCallDTO {

    private RestMethodEnum method;
    private Map<String, String> headers = new HashMap<>();
    private String url;
    private String body;

    public HttpClientCallDTO() { }

    public HttpClientCallDTO(final String url, final RestMethodEnum method) {
        this.url = url;
        this.method = method;
    }

}
