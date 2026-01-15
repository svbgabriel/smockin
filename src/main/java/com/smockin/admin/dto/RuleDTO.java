package com.smockin.admin.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by mgallina.
 */
@Setter
@Getter
public class RuleDTO {

    private String extId;
    private int orderNo;
    private int httpStatusCode;
    private String responseContentType;
    private String responseBody;
    private long sleepInMillis;
    private boolean suspend;
    private Map<String, String> responseHeaders = new HashMap<>();
    private List<RuleGroupDTO> groups = new ArrayList<>();

    public RuleDTO() {
    }

    public RuleDTO(int httpStatusCode, String responseBody) {
        this.httpStatusCode = httpStatusCode;
        this.responseBody = responseBody;
    }

    public RuleDTO(String extId, int orderNo, int httpStatusCode, String responseContentType, String responseBody, long sleepInMillis, boolean suspend) {
        this.extId = extId;
        this.orderNo = orderNo;
        this.httpStatusCode = httpStatusCode;
        this.responseContentType = responseContentType;
        this.responseBody = responseBody;
        this.sleepInMillis = sleepInMillis;
        this.suspend = suspend;
    }

}
