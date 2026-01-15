package com.smockin.admin.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by mgallina.
 */
@Setter
@Getter
public class RuleGroupDTO {

    private String extId;
    private int orderNo;
    private List<RuleConditionDTO> conditions = new ArrayList<>();

    public RuleGroupDTO() {
    }

    public RuleGroupDTO(String extId, int orderNo) {
        this.extId = extId;
        this.orderNo = orderNo;
    }

}
