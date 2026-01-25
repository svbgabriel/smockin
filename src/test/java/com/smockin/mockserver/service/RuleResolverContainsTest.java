package com.smockin.mockserver.service;

import com.smockin.admin.persistence.entity.RestfulMockDefinitionRuleGroupCondition;
import com.smockin.admin.persistence.enums.RuleComparatorEnum;
import com.smockin.admin.persistence.enums.RuleDataTypeEnum;
import com.smockin.admin.persistence.enums.RuleMatchingTypeEnum;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Created by mgallina.
 */
class RuleResolverContainsTest {

    private RuleResolver ruleResolver;

    private final String ruleTextValue = "HiJkLmNoP";
    private final String inboundTextValue = "aBcDeFg" + ruleTextValue + "qRsTuVwXyZ";


    @BeforeEach
    void setup() {
        ruleResolver = new RuleResolverImpl();
    }

    @Test
    void processRuleComparison_NullValue_Text_Contains_Fail() {

        // Setup
        final RestfulMockDefinitionRuleGroupCondition condition =
                new RestfulMockDefinitionRuleGroupCondition(null, "NAME", RuleDataTypeEnum.TEXT, RuleComparatorEnum.CONTAINS, ruleTextValue, RuleMatchingTypeEnum.REQUEST_PARAM, false);

        // Test
        final boolean result = ruleResolver.processRuleComparison(condition, null);

        // Assertions
        Assertions.assertFalse(result);
    }

    @Test
    void processRuleComparison_Text_Contains_Pass() {

        // Setup
        final RestfulMockDefinitionRuleGroupCondition condition =
                new RestfulMockDefinitionRuleGroupCondition(null, "NAME", RuleDataTypeEnum.TEXT, RuleComparatorEnum.CONTAINS, ruleTextValue.toUpperCase(), RuleMatchingTypeEnum.REQUEST_PARAM, false);

        // Test
        final boolean result = ruleResolver.processRuleComparison(condition, inboundTextValue);

        // Assertions
        Assertions.assertTrue(result);
    }

    @Test
    void processRuleComparison_CaseSensitiveText_Contains_Pass() {

        // Setup
        final RestfulMockDefinitionRuleGroupCondition condition =
                new RestfulMockDefinitionRuleGroupCondition(null, "NAME", RuleDataTypeEnum.TEXT, RuleComparatorEnum.CONTAINS, ruleTextValue, RuleMatchingTypeEnum.REQUEST_PARAM, true);

        // Test
        final boolean result = ruleResolver.processRuleComparison(condition, inboundTextValue);

        // Assertions
        Assertions.assertTrue(result);
    }

    @Test
    void processRuleComparison_CaseSensitiveText_Contains_Fail() {

        // Setup
        final RestfulMockDefinitionRuleGroupCondition condition =
                new RestfulMockDefinitionRuleGroupCondition(null, "NAME", RuleDataTypeEnum.TEXT, RuleComparatorEnum.CONTAINS, ruleTextValue.toUpperCase(), RuleMatchingTypeEnum.REQUEST_PARAM, true);

        // Test
        final boolean result = ruleResolver.processRuleComparison(condition, inboundTextValue);

        // Assertions
        Assertions.assertFalse(result);
    }

    @Test
    void processRuleComparison_CaseSensitiveFieldIsNullText_Contains_Fail() {

        // Setup
        final RestfulMockDefinitionRuleGroupCondition condition =
                new RestfulMockDefinitionRuleGroupCondition(null, "NAME", RuleDataTypeEnum.TEXT, RuleComparatorEnum.CONTAINS, ruleTextValue.toUpperCase(), RuleMatchingTypeEnum.REQUEST_PARAM, null);

        // Test
        final boolean result = ruleResolver.processRuleComparison(condition, inboundTextValue);

        // Assertions
        Assertions.assertTrue(result);
    }

}
