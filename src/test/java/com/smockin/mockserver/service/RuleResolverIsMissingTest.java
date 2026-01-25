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
class RuleResolverIsMissingTest {

    private RuleResolver ruleResolver;

    private final String ruleFieldName = "FirstName";


    @BeforeEach
    void setup() {
        ruleResolver = new RuleResolverImpl();
    }

    @Test
    void processRuleComparison_Text_IsMissing_Pass() {

        // Setup
        final RestfulMockDefinitionRuleGroupCondition condition =
                new RestfulMockDefinitionRuleGroupCondition(null, ruleFieldName, RuleDataTypeEnum.TEXT, RuleComparatorEnum.IS_MISSING, "", RuleMatchingTypeEnum.REQUEST_PARAM, false);

        // Test
        final boolean result = ruleResolver.processRuleComparison(condition, null);

        // Assertions
        Assertions.assertTrue(result);
    }

    @Test
    void processRuleComparison_Text_IsMissing_Fail() {

        // Setup
        final RestfulMockDefinitionRuleGroupCondition condition =
                new RestfulMockDefinitionRuleGroupCondition(null, ruleFieldName, RuleDataTypeEnum.TEXT, RuleComparatorEnum.IS_MISSING, "Joe", RuleMatchingTypeEnum.REQUEST_PARAM, false);

        // Test
        final boolean result = ruleResolver.processRuleComparison(condition, "Joe");

        // Assertions
        Assertions.assertFalse(result);
    }

    @Test
    void processRuleComparison_CaseSensitiveText_IsMissing_Pass() {

        // Setup
        final RestfulMockDefinitionRuleGroupCondition condition =
                new RestfulMockDefinitionRuleGroupCondition(null, ruleFieldName, RuleDataTypeEnum.TEXT, RuleComparatorEnum.IS_MISSING, "Joe", RuleMatchingTypeEnum.REQUEST_PARAM, true);

        // Test
        final boolean result = ruleResolver.processRuleComparison(condition, "joe");

        // Assertions
        Assertions.assertTrue(result);
    }

    @Test
    void processRuleComparison_Text_IsMissing_DifferentInputValue_Pass() {

        // Setup
        final RestfulMockDefinitionRuleGroupCondition condition =
                new RestfulMockDefinitionRuleGroupCondition(null, ruleFieldName, RuleDataTypeEnum.TEXT, RuleComparatorEnum.IS_MISSING, "Joe", RuleMatchingTypeEnum.REQUEST_PARAM, false);

        // Test
        final boolean result = ruleResolver.processRuleComparison(condition, "jane");

        // Assertions
        Assertions.assertTrue(result);
    }

    @Test
    void processRuleComparison_Text_IsMissing_NullInput_Pass() {

        // Setup
        final RestfulMockDefinitionRuleGroupCondition condition =
                new RestfulMockDefinitionRuleGroupCondition(null, ruleFieldName, RuleDataTypeEnum.TEXT, RuleComparatorEnum.IS_MISSING, "Joe", RuleMatchingTypeEnum.REQUEST_PARAM, false);

        // Test
        final boolean result = ruleResolver.processRuleComparison(condition, null);

        // Assertions
        Assertions.assertTrue(result);
    }

    @Test
    void processRuleComparison_Text_IsMissing_BlankInput_Pass() {

        // Setup
        final RestfulMockDefinitionRuleGroupCondition condition =
                new RestfulMockDefinitionRuleGroupCondition(null, ruleFieldName, RuleDataTypeEnum.TEXT, RuleComparatorEnum.IS_MISSING, "Joe", RuleMatchingTypeEnum.REQUEST_PARAM, false);

        // Test
        final boolean result = ruleResolver.processRuleComparison(condition, "");

        // Assertions
        Assertions.assertTrue(result);
    }

}
