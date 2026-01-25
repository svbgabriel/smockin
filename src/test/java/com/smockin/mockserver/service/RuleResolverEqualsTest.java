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
class RuleResolverEqualsTest {

    private RuleResolver ruleResolver;

    private final String inboundTextValue = "aBcDeF";
    private final String inboundNumericWholeValue = "201";
    private final String inboundNumericDecimalValue = "201.321";

    @BeforeEach
    void setup() {
        ruleResolver = new RuleResolverImpl();
    }

    @Test
    void processRuleComparison_NullComp_Fail() {

        // Setup
        final RestfulMockDefinitionRuleGroupCondition condition =
                new RestfulMockDefinitionRuleGroupCondition(null, "NAME", RuleDataTypeEnum.TEXT, null, inboundTextValue, RuleMatchingTypeEnum.REQUEST_PARAM, false);

        // Test & Assertions
        final IllegalArgumentException ex = Assertions.assertThrows(IllegalArgumentException.class,
                () -> ruleResolver.processRuleComparison(condition, null));
        Assertions.assertEquals("Invalid rule comparator. Cannot be null", ex.getMessage());
    }

    @Test
    void processRuleComparison_NullValue_Text_Equals_Fail() {

        // Setup
        final RestfulMockDefinitionRuleGroupCondition condition =
                new RestfulMockDefinitionRuleGroupCondition(null, "NAME", RuleDataTypeEnum.TEXT, RuleComparatorEnum.EQUALS, inboundTextValue, RuleMatchingTypeEnum.REQUEST_PARAM, false);

        // Test
        final boolean result = ruleResolver.processRuleComparison(condition, null);

        // Assertions
        Assertions.assertFalse(result);
    }

    @Test
    void processRuleComparison_Text_Equals_Pass() {

        // Setup
        final RestfulMockDefinitionRuleGroupCondition condition =
                new RestfulMockDefinitionRuleGroupCondition(null, "NAME", RuleDataTypeEnum.TEXT, RuleComparatorEnum.EQUALS, inboundTextValue, RuleMatchingTypeEnum.REQUEST_PARAM, false);

        // Test
        final boolean result = ruleResolver.processRuleComparison(condition, inboundTextValue.toUpperCase());

        // Assertions
        Assertions.assertTrue(result);
    }

    @Test
    void processRuleComparison_Text_Equals_Fail() {

        // Setup
        final RestfulMockDefinitionRuleGroupCondition condition =
                new RestfulMockDefinitionRuleGroupCondition(null, "NAME", RuleDataTypeEnum.TEXT, RuleComparatorEnum.EQUALS, inboundTextValue, RuleMatchingTypeEnum.REQUEST_PARAM, false);

        // Test
        final boolean result = ruleResolver.processRuleComparison(condition, inboundTextValue + "GHI");

        // Assertions
        Assertions.assertFalse(result);
    }

    @Test
    void processRuleComparison_CaseSensitiveFieldIsNull_Text_Equals_Pass() {

        // Setup
        final RestfulMockDefinitionRuleGroupCondition condition =
                new RestfulMockDefinitionRuleGroupCondition(null, "NAME", RuleDataTypeEnum.TEXT, RuleComparatorEnum.EQUALS, inboundTextValue, RuleMatchingTypeEnum.REQUEST_PARAM, null);

        // Test
        final boolean result = ruleResolver.processRuleComparison(condition, inboundTextValue.toUpperCase());

        // Assertions
        Assertions.assertTrue(result);
    }

    @Test
    void processRuleComparison_CaseSensitive_Text_Equals_Pass() {

        // Setup
        final RestfulMockDefinitionRuleGroupCondition condition =
                new RestfulMockDefinitionRuleGroupCondition(null, "NAME", RuleDataTypeEnum.TEXT, RuleComparatorEnum.EQUALS, inboundTextValue, RuleMatchingTypeEnum.REQUEST_PARAM, true);

        // Test
        final boolean result = ruleResolver.processRuleComparison(condition, inboundTextValue);

        // Assertions
        Assertions.assertTrue(result);
    }

    @Test
    void processRuleComparison_CaseSensitive_Text_Equals_Fail() {

        // Setup
        final RestfulMockDefinitionRuleGroupCondition condition =
                new RestfulMockDefinitionRuleGroupCondition(null, "NAME", RuleDataTypeEnum.TEXT, RuleComparatorEnum.EQUALS, inboundTextValue, RuleMatchingTypeEnum.REQUEST_PARAM, true);

        // Test
        final boolean result = ruleResolver.processRuleComparison(condition, inboundTextValue.toUpperCase());

        // Assertions
        Assertions.assertFalse(result);
    }

    @Test
    void processRuleComparison_Whole_Numeric_Equals_Pass() {

        // Setup
        final RestfulMockDefinitionRuleGroupCondition condition =
                new RestfulMockDefinitionRuleGroupCondition(null, "NAME", RuleDataTypeEnum.NUMERIC, RuleComparatorEnum.EQUALS, inboundNumericWholeValue, RuleMatchingTypeEnum.REQUEST_PARAM, null);

        // Test
        final boolean result = ruleResolver.processRuleComparison(condition, inboundNumericWholeValue);

        // Assertions
        Assertions.assertTrue(result);
    }

    @Test
    void processRuleComparison_Whole_Numeric_Equals_Fail() {

        // Setup
        final RestfulMockDefinitionRuleGroupCondition condition =
                new RestfulMockDefinitionRuleGroupCondition(null, "NAME", RuleDataTypeEnum.NUMERIC, RuleComparatorEnum.EQUALS, inboundNumericWholeValue, RuleMatchingTypeEnum.REQUEST_PARAM, null);

        // Test
        final boolean result = ruleResolver.processRuleComparison(condition, "101");

        // Assertions
        Assertions.assertFalse(result);
    }

    @Test
    void processRuleComparison_Decimal_Numeric_Equals_Pass() {

        // Setup
        final RestfulMockDefinitionRuleGroupCondition condition =
                new RestfulMockDefinitionRuleGroupCondition(null, "NAME", RuleDataTypeEnum.NUMERIC, RuleComparatorEnum.EQUALS, inboundNumericDecimalValue, RuleMatchingTypeEnum.REQUEST_PARAM, null);

        // Test
        final boolean result = ruleResolver.processRuleComparison(condition, inboundNumericDecimalValue);

        // Assertions
        Assertions.assertTrue(result);
    }

    @Test
    void processRuleComparison_Decimal_Numeric_Equals_Fail() {

        // Setup
        final RestfulMockDefinitionRuleGroupCondition condition =
                new RestfulMockDefinitionRuleGroupCondition(null, "NAME", RuleDataTypeEnum.NUMERIC, RuleComparatorEnum.EQUALS, inboundNumericDecimalValue, RuleMatchingTypeEnum.REQUEST_PARAM, null);

        // Test
        final boolean result = ruleResolver.processRuleComparison(condition, "201.322");

        // Assertions
        Assertions.assertFalse(result);
    }

    @Test
    void processRuleComparison_Invalid_Numeric_Equals_Fail() {

        // Setup
        final RestfulMockDefinitionRuleGroupCondition condition =
                new RestfulMockDefinitionRuleGroupCondition(null, "NAME", RuleDataTypeEnum.NUMERIC, RuleComparatorEnum.EQUALS, inboundNumericDecimalValue, RuleMatchingTypeEnum.REQUEST_PARAM, null);

        // Test
        final boolean result = ruleResolver.processRuleComparison(condition, "One");

        // Assertions
        Assertions.assertFalse(result);
    }

}
