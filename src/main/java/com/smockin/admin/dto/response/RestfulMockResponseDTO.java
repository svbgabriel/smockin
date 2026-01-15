package com.smockin.admin.dto.response;

import com.smockin.admin.dto.RestfulMockDTO;
import com.smockin.admin.dto.RuleDTO;
import com.smockin.admin.persistence.enums.RestMockTypeEnum;
import com.smockin.admin.persistence.enums.RecordStatusEnum;
import com.smockin.admin.persistence.enums.RestMethodEnum;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by mgallina.
 */
@Setter
@Getter
public class RestfulMockResponseDTO extends RestfulMockDTO {

    private String extId;
    private Date dateCreated;
    private String createdBy;
    private String userCtxPath;
    private List<RuleDTO> rules = new ArrayList<>();

    public RestfulMockResponseDTO() {

    }

    public RestfulMockResponseDTO(final String extId, final String path, final String userCtxPath, final RestMethodEnum method, final RecordStatusEnum status,
                                  final RestMockTypeEnum mockType, boolean statefulParent, final Date dateCreated, final String createdBy, final long proxyTimeoutInMillis, final long webSocketTimeoutInMillis, final long sseHeartBeatInMillis,
                                  final boolean proxyPushIdOnConnect, final boolean randomiseDefinitions, final boolean proxyForwardWhenNoRuleMatch,
                                  boolean randomiseLatency, long randomiseLatencyRangeMinMillis, long randomiseLatencyRangeMaxMillis, String projectId, String customJsSyntax,
                                  final String statefulDefaultResponseBody, final String statefulIdFieldName, final String statefulIdFieldLocation) {

        super(path, method, status, mockType, statefulParent, proxyTimeoutInMillis, webSocketTimeoutInMillis,
                sseHeartBeatInMillis, proxyPushIdOnConnect, randomiseDefinitions, proxyForwardWhenNoRuleMatch,
                randomiseLatency, randomiseLatencyRangeMinMillis, randomiseLatencyRangeMaxMillis, projectId,
                customJsSyntax, statefulDefaultResponseBody, statefulIdFieldName, statefulIdFieldLocation);

        this.extId = extId;
        this.dateCreated = dateCreated;
        this.createdBy = createdBy;
        this.userCtxPath = userCtxPath;
    }

}
