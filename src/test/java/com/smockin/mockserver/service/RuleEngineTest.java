package com.smockin.mockserver.service;

import com.smockin.admin.persistence.entity.*;
import com.smockin.admin.persistence.enums.RuleComparatorEnum;
import com.smockin.admin.persistence.enums.RuleDataTypeEnum;
import com.smockin.admin.persistence.enums.RuleMatchingTypeEnum;
import com.smockin.admin.service.SmockinUserService;
import com.smockin.mockserver.service.dto.RestfulResponseDTO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by mgallina.
 */
@ExtendWith(MockitoExtension.class)
class RuleEngineTest {

    @Mock
    private RuleResolver ruleResolver;

    private MockHttpServletRequest req;

    @Mock
    private List<RestfulMockDefinitionRule> rules;

    @Mock
    private SmockinUserService smockinUserService;

    @Spy
    @InjectMocks
    private RuleEngineImpl ruleEngine = new RuleEngineImpl();

    private String userCtxPath;


    @BeforeEach
    void setUp() {

        userCtxPath = "";
        req = new MockHttpServletRequest();

    }

    @Test
    void process_nullRules_Test() {

        // Setup
        rules = null;

        // Test & Assertions
        Assertions.assertThrows(NullPointerException.class,
                () -> ruleEngine.process(req, rules));

    }

    @Test
    void process_emptyRules_Test() {

        // Setup
        rules = new ArrayList<>();

        // Test
        final RestfulResponseDTO result = ruleEngine.process(req, rules);

        // Assertions
        Assertions.assertNull(result);

    }

    @Test
    void process_Test() {

        // Setup
        rules = new ArrayList<>();

        final RestfulMock mock = new RestfulMock();
        mock.setPath("/person/{name}");
        final SmockinUser user = new SmockinUser();
        user.setCtxPath(userCtxPath);
        mock.setCreatedBy(user);
        final RestfulMockDefinitionRule rule = new RestfulMockDefinitionRule(mock, 1, 200, MediaType.APPLICATION_JSON_VALUE, "{ \"msg\" : \"foobar\" }", 0, false);
        final RestfulMockDefinitionRuleGroup group = new RestfulMockDefinitionRuleGroup(rule, 1);
        final RestfulMockDefinitionRuleGroupCondition condition = new RestfulMockDefinitionRuleGroupCondition(group, "name", RuleDataTypeEnum.TEXT, RuleComparatorEnum.EQUALS, "joe", RuleMatchingTypeEnum.REQUEST_BODY, false);

        group.getConditions().add(condition);
        rule.getConditionGroups().add(group);
        rules.add(rule);

        String body = "{ \"name\" : \"joe\" }";
        req.setContent(body.getBytes(StandardCharsets.UTF_8));
        Mockito.when(ruleResolver.processRuleComparison(Mockito.any(RestfulMockDefinitionRuleGroupCondition.class), Mockito.anyString())).thenReturn(true);

        // Test
        final RestfulResponseDTO result = ruleEngine.process(req, rules);

        // Assertions
        Assertions.assertNotNull(result);
        Assertions.assertEquals(rule.getHttpStatusCode(), result.getHttpStatusCode());
        Assertions.assertEquals(rule.getResponseContentType(), result.getResponseContentType());
        Assertions.assertEquals(rule.getResponseBody(), result.getResponseBody());
    }

    @Test
    void extractInboundValue_nullRuleMatchingType_Test() {

        // Test & Assertions
        Assertions.assertThrows(NullPointerException.class,
                () -> ruleEngine.extractInboundValue(null, "", req, "/person/{name}", userCtxPath));

    }

    @Test
    void extractInboundValue_reqHeader_Test() {

        // Setup
        final String fieldName = "name";
        final String reqResponse = "Hey Joe";
        req.addHeader(fieldName, reqResponse);

        // Test
        final String result = ruleEngine.extractInboundValue(RuleMatchingTypeEnum.REQUEST_HEADER, fieldName, req, "/person/{name}", userCtxPath);

        // Assertions
        Assertions.assertNotNull(result);
        Assertions.assertEquals(reqResponse, result);

    }

    @Test
    void extractInboundValue_reqParam_Test() {

        // Setup
        final String fieldName = "name";
        final String reqResponse = "Hey Joe";

        req.setMethod(HttpMethod.POST.name());
        req.addParameter(fieldName, reqResponse);

        // Test
        final String result = ruleEngine.extractInboundValue(RuleMatchingTypeEnum.REQUEST_PARAM, "name", req, "/person/{name}", userCtxPath);

        // Assertions
        Assertions.assertNotNull(result);
        Assertions.assertEquals(reqResponse, result);

    }

    @Test
    void extractInboundValue_reqBody_Test() {

        // Setup
        final String reqResponse = "Hey Joe";
        req.setContent(reqResponse.getBytes(StandardCharsets.UTF_8));

        // Test
        final String result = ruleEngine.extractInboundValue(RuleMatchingTypeEnum.REQUEST_BODY, "", req, "/person/{name}", userCtxPath);

        // Assertions
        Assertions.assertNotNull(result);
        Assertions.assertEquals(reqResponse, result);

    }

    @Test
    void extractInboundValue_pathVariable_Test() {

        // Setup
        final String fieldName = "name";
        req.setPathInfo("/person/Joe");

        // Test
        final String result = ruleEngine.extractInboundValue(RuleMatchingTypeEnum.PATH_VARIABLE, fieldName, req, "/person/{name}", userCtxPath);

        // Assertions
        Assertions.assertNotNull(result);
        Assertions.assertEquals("Joe", result);

    }

    @Test
    void extractInboundValue_jsonReqBody_Test() {

        // Setup
        final String fieldName = "username";
        final String fieldValue = "admin";
        String body = "{\"username\":\"" + fieldValue + "\"}";
        req.setContent(body.getBytes(StandardCharsets.UTF_8));

        // Test
        final String result = ruleEngine.extractInboundValue(RuleMatchingTypeEnum.REQUEST_BODY_JSON_ANY, fieldName, req, "/person/{name}", userCtxPath);

        // Assertions
        Assertions.assertNotNull(result);
        Assertions.assertEquals(fieldValue, result);

    }

    @Test
    void extractInboundValue_jsonReqBody_NotFound_Test() {

        // Setup
        final String fieldName = "username";
        String body = "{\"foo\":\"bar\"}";
        req.setContent(body.getBytes(StandardCharsets.UTF_8));

        // Test
        final String result = ruleEngine.extractInboundValue(RuleMatchingTypeEnum.REQUEST_BODY_JSON_ANY, fieldName, req, "/person/{name}", userCtxPath);

        // Assertions
        Assertions.assertNull(result);

    }

    @Test
    void extractInboundValue_jsonReqBody_invalidJson_Test() {

        // Setup
        final String fieldName = "username";
        String body = "username = admin";
        req.setContentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE);
        req.setContent(body.getBytes(StandardCharsets.UTF_8));

        // Test
        final String result = ruleEngine.extractInboundValue(RuleMatchingTypeEnum.REQUEST_BODY_JSON_ANY, fieldName, req, "/person/{name}", userCtxPath);

        // Assertions
        Assertions.assertNull(result);

    }

    @Test
    void extractInboundValue_jsonReqBody_null_Test() {

        // Setup
        final String fieldName = "username";

        // Test
        final String result = ruleEngine.extractInboundValue(RuleMatchingTypeEnum.REQUEST_BODY_JSON_ANY, fieldName, req, "/person/{name}", userCtxPath);

        // Assertions
        Assertions.assertNull(result);

    }

}
