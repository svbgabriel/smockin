package com.smockin.admin.dto;

import com.smockin.admin.persistence.enums.RuleComparatorEnum;
import com.smockin.admin.persistence.enums.RuleDataTypeEnum;
import com.smockin.admin.persistence.enums.RuleMatchingTypeEnum;
import lombok.Getter;
import lombok.Setter;


/**
 * Created by mgallina.
 */
@Setter
public class RuleConditionDTO {

    @Getter
    private String extId;
    @Getter
    private String field;
    @Getter
    private RuleDataTypeEnum dataType;
    @Getter
    private RuleComparatorEnum comparator;
    @Getter
    private String value;
    @Getter
    private RuleMatchingTypeEnum ruleMatchingType;
    private Boolean caseSensitive;

    public RuleConditionDTO() {
    }

    public RuleConditionDTO(String extId, String field, RuleDataTypeEnum dataType, RuleComparatorEnum comparator, String value, RuleMatchingTypeEnum ruleMatchingType, Boolean caseSensitive) {
        this.extId = extId;
        this.field = field;
        this.dataType = dataType;
        this.comparator = comparator;
        this.value = value;
        this.ruleMatchingType = ruleMatchingType;
        this.caseSensitive = caseSensitive;
    }

    public RuleConditionDTO(String field, RuleDataTypeEnum dataType, RuleComparatorEnum comparator, String value, RuleMatchingTypeEnum ruleMatchingType, Boolean caseSensitive) {
        this.field = field;
        this.dataType = dataType;
        this.comparator = comparator;
        this.value = value;
        this.ruleMatchingType = ruleMatchingType;
        this.caseSensitive = caseSensitive;
    }

    public Boolean isCaseSensitive() {
        return caseSensitive;
    }

}
