package com.smockin.admin.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by mgallina.
 */
@Setter
@Getter
public class RestfulMockDefinitionDTO {

    private String extId;
    private int orderNo;
    private int httpStatusCode;
    private String responseContentType;
    private String responseBody;
    private long sleepInMillis;
    private boolean suspend;
    private int frequencyCount;
    private int frequencyPercentage;
    private Map<String, String> responseHeaders = new HashMap<String, String>();

    public RestfulMockDefinitionDTO() {

    }

    public RestfulMockDefinitionDTO(String extId, int orderNo, int httpStatusCode, String responseContentType, String responseBody, long sleepInMillis, boolean suspend, int frequencyCount, int frequencyPercentage) {
        this.extId = extId;
        this.orderNo = orderNo;
        this.httpStatusCode = httpStatusCode;
        this.responseContentType = responseContentType;
        this.responseBody = responseBody;
        this.sleepInMillis = sleepInMillis;
        this.suspend = suspend;
        this.frequencyCount = frequencyCount;
        this.frequencyPercentage = frequencyPercentage;
    }

    public RestfulMockDefinitionDTO(int orderNo, int httpStatusCode, String responseContentType, String responseBody, int frequencyCount) {
        this.orderNo = orderNo;
        this.httpStatusCode = httpStatusCode;
        this.responseContentType = responseContentType;
        this.responseBody = responseBody;
        this.frequencyCount = frequencyCount;
    }

}
