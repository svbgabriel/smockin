package com.smockin.mockserver.service;

import com.smockin.admin.dto.UserKeyValueDataDTO;
import com.smockin.admin.enums.UserModeEnum;
import com.smockin.admin.service.SmockinUserService;
import com.smockin.admin.service.UserKeyValueDataService;
import com.smockin.mockserver.exception.InboundParamMatchException;
import com.smockin.mockserver.service.enums.ParamMatchTypeEnum;
import com.smockin.utils.GeneralUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpServletRequest;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by mgallina.
 */
@ExtendWith(MockitoExtension.class)
class InboundParamMatchServiceTest {

    private MockHttpServletRequest request;
    private String sanitizedUserCtxInboundPath;
    private long userId;

    @Mock
    private SmockinUserService smockinUserService;

    @Mock
    private UserKeyValueDataService userKeyValueDataService;

    @Spy
    @InjectMocks
    private InboundParamMatchServiceImpl inboundParamMatchServiceImpl = new InboundParamMatchServiceImpl();


    @BeforeEach
    void setUp() {

        sanitizedUserCtxInboundPath = "";
        userId = 1;
        request = new MockHttpServletRequest();
    }

    @Test
    void processParamMatch_NoToken_Test() {
        Assertions.assertNull(inboundParamMatchServiceImpl.processParamMatch(request, "/person/{name}", "Hello World", sanitizedUserCtxInboundPath, userId));
    }

    @Test
    void processParamMatch_InvalidToken_Test() {

        // Test
        final String responseBody = "Hello " + ParamMatchTypeEnum.PARAM_PREFIX + "Foo";

        // Assertions
        Assertions.assertNull(inboundParamMatchServiceImpl.processParamMatch(request, "/person/{name}", responseBody, sanitizedUserCtxInboundPath, userId));
    }

    @Test
    void processParamMatch_InvalidTokenWithBrackets_Test() {

        // Setup
        final String responseBody = "Hello " + ParamMatchTypeEnum.PARAM_PREFIX + "Foo()";

        // Test & Assertions
        Assertions.assertNull(inboundParamMatchServiceImpl.processParamMatch(request, "/person/{name}", responseBody, sanitizedUserCtxInboundPath, userId));
    }

    @Test
    void processParamMatch_Empty_Test() {

        // Setup
        final String responseBody = "Hello " + ParamMatchTypeEnum.PARAM_PREFIX + "(  )";

        // Test & Assertions
        Assertions.assertNull(inboundParamMatchServiceImpl.processParamMatch(request, "/person/{name}", responseBody, sanitizedUserCtxInboundPath, userId));
    }

    @Test
    void processParamMatch_Blank_Test() {

        // Setup
        final String responseBody = "Hello " + ParamMatchTypeEnum.PARAM_PREFIX + "()";

        // Test & Assertions
        Assertions.assertNull(inboundParamMatchServiceImpl.processParamMatch(request, "/person/{name}", responseBody, sanitizedUserCtxInboundPath, userId));

    }

    @Test
    void processParamMatch_header_Test() {

        // Setup
        final String responseBody = "Hello " + ParamMatchTypeEnum.PARAM_PREFIX + ParamMatchTypeEnum.requestHeader.name() + "(name)";

        request.addHeader("name", "Roger");

        // Test
        final String result = inboundParamMatchServiceImpl.processParamMatch(request, "/person/{name}", responseBody, sanitizedUserCtxInboundPath, userId);

        // Assertions
        Assertions.assertEquals("Hello Roger", result);
    }

    @Test
    void processParamMatch_headerCase_Test() {

        // Setup
        final String responseBody = "Hello " + ParamMatchTypeEnum.PARAM_PREFIX + ParamMatchTypeEnum.requestHeader.name() + "(NAME)";

        request.addHeader("name", "Roger");

        // Test
        final String result = inboundParamMatchServiceImpl.processParamMatch(request, "/person/{name}", responseBody, sanitizedUserCtxInboundPath, userId);

        // Assertions
        Assertions.assertEquals("Hello Roger", result);
    }

    @Test
    void processParamMatch_headerNoMatch_Test() {

        // Test
        final String responseBody = "Hello " + ParamMatchTypeEnum.PARAM_PREFIX + ParamMatchTypeEnum.requestHeader.name() + "(name)";
        final String result = inboundParamMatchServiceImpl.processParamMatch(request, "/person/{name}", responseBody, sanitizedUserCtxInboundPath, userId);

        // Assertions
        Assertions.assertEquals("Hello ", result);
    }

    @Test
    void processParamMatch_reqParam_Test() {

        // Setup
        final String responseBody = "Hello " + ParamMatchTypeEnum.PARAM_PREFIX + ParamMatchTypeEnum.requestParameter.name() + "(name)";

        request.addParameter("name", "Roger");

        // Test
        final String result = inboundParamMatchServiceImpl.processParamMatch(request, "/person/{name}", responseBody, sanitizedUserCtxInboundPath, userId);

        // Assertions
        Assertions.assertEquals("Hello Roger", result);
    }

    @Test
    void processParamMatch_reqParamCase_Test() {

        // Setup
        final String responseBody = "Hello " + ParamMatchTypeEnum.PARAM_PREFIX + ParamMatchTypeEnum.requestParameter.name() + "(NAME)";

        request.setMethod(HttpMethod.GET.name());
        request.setParameter("name", "Roger");
        request.addParameter("name", new String[]{"Roger"});

        // Test
        final String result = inboundParamMatchServiceImpl.processParamMatch(request, "/person/{name}", responseBody, sanitizedUserCtxInboundPath, userId);

        // Assertions
        Assertions.assertEquals("Hello Roger", result);
    }

    @Test
    void processParamMatch_reqParamNoMatch_Test() {

        // Test
        final String responseBody = "Hello " + ParamMatchTypeEnum.PARAM_PREFIX + ParamMatchTypeEnum.requestParameter.name() + "(name)";
        final String result = inboundParamMatchServiceImpl.processParamMatch(request, "/person/{name}", responseBody, sanitizedUserCtxInboundPath, userId);

        // Assertions
        Assertions.assertEquals("Hello ", result);
    }

    @Test
    void processParamMatch_pathVar_Test() {

        // Setup
        final String responseBody = "Hello " + ParamMatchTypeEnum.PARAM_PREFIX + ParamMatchTypeEnum.pathVar.name() + "(name)";

        sanitizedUserCtxInboundPath = "/person/Roger";

        // Test
        final String result = inboundParamMatchServiceImpl.processParamMatch(request, "/person/{name}", responseBody, sanitizedUserCtxInboundPath, userId);

        // Assertions
        Assertions.assertEquals("Hello Roger", result);
    }

    @Test
    void processParamMatch_pathVarCase_Test() {

        // Setup
        final String responseBody = "Hello " + ParamMatchTypeEnum.PARAM_PREFIX + ParamMatchTypeEnum.pathVar.name() + "(NAME)";

        sanitizedUserCtxInboundPath = "/person/Roger";

        // Test
        final String result = inboundParamMatchServiceImpl.processParamMatch(request, "/person/{name}", responseBody, sanitizedUserCtxInboundPath, userId);

        // Assertions
        Assertions.assertEquals("Hello Roger", result);
    }

    @Test
    void processParamMatch_pathVarNoMatch_Test() {

        // Test
        final String responseBody = "Hello " + ParamMatchTypeEnum.PARAM_PREFIX + ParamMatchTypeEnum.pathVar.name() + "(name)";
        final String result = inboundParamMatchServiceImpl.processParamMatch(request, "/person/{name}", responseBody, sanitizedUserCtxInboundPath, userId);

        // Assertions
        Assertions.assertEquals("Hello ", result);
    }

    @Test
    void enrichWithInboundParamMatches_multiMatchesAndSpaces_Test() throws InboundParamMatchException {

        // Setup
        final String responseBody = "Hello " + ParamMatchTypeEnum.PARAM_PREFIX + ParamMatchTypeEnum.requestHeader.name() + "('name'), you are " + ParamMatchTypeEnum.PARAM_PREFIX + ParamMatchTypeEnum.requestHeader.name() + "(GenDer) and are " + ParamMatchTypeEnum.PARAM_PREFIX + ParamMatchTypeEnum.requestHeader.name() + "(\"age\") years old";

        request.addHeader("name", "Roger");
        request.addHeader("age", "21");
        request.addHeader("gender", "Male");

        Mockito.when(smockinUserService.getUserMode()).thenReturn(UserModeEnum.INACTIVE);

        // Test
        final String result = inboundParamMatchServiceImpl.enrichWithInboundParamMatches(request, "/person/{name}", responseBody, sanitizedUserCtxInboundPath, userId);

        // Assertions
        Assertions.assertEquals("Hello Roger, you are Male and are 21 years old", result);
    }

    @Test
    void enrichWithInboundParamMatches_partialMatch_Test() throws InboundParamMatchException {

        // Setup
        final String responseBody = "Hello " + ParamMatchTypeEnum.PARAM_PREFIX + ParamMatchTypeEnum.requestHeader.name() + "(name), you are " + ParamMatchTypeEnum.PARAM_PREFIX + ParamMatchTypeEnum.requestHeader.name() + "(age) years old";

        request.addHeader("name", "Roger");

        // Test
        final String result = inboundParamMatchServiceImpl.enrichWithInboundParamMatches(request, "/person/{name}", responseBody, sanitizedUserCtxInboundPath, userId);

        // Assertions
        Assertions.assertEquals("Hello Roger, you are  years old", result);
    }

    @Test
    void enrichWithInboundParamMatches_withNoMadeUpToken_Test() throws InboundParamMatchException {

        // Setup
        final String responseBody = "Hello " + ParamMatchTypeEnum.PARAM_PREFIX + ParamMatchTypeEnum.requestHeader.name() + "(name), you are " + ParamMatchTypeEnum.PARAM_PREFIX + "FOO(age) years old";

        request.addHeader("name", "Roger");

        // Test
        final String result = inboundParamMatchServiceImpl.enrichWithInboundParamMatches(request, "/person/{name}", responseBody, sanitizedUserCtxInboundPath, userId);

        // Assertions
        Assertions.assertNotNull(result);
        Assertions.assertEquals("Hello Roger, you are " + ParamMatchTypeEnum.PARAM_PREFIX + "FOO(age) years old", result);
    }

    @Test
    void processParamMatch_isoDate_Test() {

        // Setup
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        final String responseBody = "The date is " + ParamMatchTypeEnum.PARAM_PREFIX + ParamMatchTypeEnum.isoDate.name();

        // Test
        final String result = inboundParamMatchServiceImpl.processParamMatch(request, "/person/{name}", responseBody, sanitizedUserCtxInboundPath, userId);

        // Assertions
        final String remainder = result.replaceAll("The date is ", "");

        try {
            Assertions.assertNotNull(new SimpleDateFormat(GeneralUtils.ISO_DATE_FORMAT).parse(remainder));
        } catch (Throwable ex) {
            Assertions.fail();
        }
    }

    @Test
    void processParamMatch_isoDateTime_Test() {

        // Setup
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        final String responseBody = "The date and time is " + ParamMatchTypeEnum.PARAM_PREFIX + ParamMatchTypeEnum.isoDatetime.name();

        // Test
        final String result = inboundParamMatchServiceImpl.processParamMatch(request, "/person/{name}", responseBody, sanitizedUserCtxInboundPath, userId);

        // Assertions
        final String remainder = result.replaceAll("The date and time is ", "");

        try {
            Assertions.assertNotNull(new SimpleDateFormat(GeneralUtils.ISO_DATETIME_FORMAT).parse(remainder));
        } catch (Throwable ex) {
            Assertions.fail();
        }
    }

    @Test
    void processParamMatch_uuid_Test() {

        // Setup
        final String responseBody = "Your ID is " + ParamMatchTypeEnum.PARAM_PREFIX + ParamMatchTypeEnum.uuid.name();

        // Test
        final String result = inboundParamMatchServiceImpl.processParamMatch(request, "/person/{name}", responseBody, sanitizedUserCtxInboundPath, userId);

        // Assertions
        final String remainder = result.replaceAll("Your ID is ", "");

        try {
            Assertions.assertNotNull(UUID.fromString(remainder));
        } catch (Throwable ex) {
            Assertions.fail();
        }
    }

    @Test
    void processParamMatch_randomNumber_Test() {

        // Setup
        final String responseBody = "Your number is " + ParamMatchTypeEnum.PARAM_PREFIX + ParamMatchTypeEnum.randomNumber.name() + "(1,3)";

        // Test
        final String result = inboundParamMatchServiceImpl.processParamMatch(request, "/person/{name}", responseBody, sanitizedUserCtxInboundPath, userId);

        // Assertions
        final String remainder = result.replaceAll("Your number is ", "");
        Assertions.assertTrue(NumberUtils.isDigits(remainder));
        Assertions.assertTrue((Integer.parseInt(remainder) == 1) || (Integer.valueOf(remainder) == 2) || (Integer.valueOf(remainder) == 3));
    }

    @Test
    void processParamMatch_randomNumberZero_Test() {

        // Setup
        final String responseBody = "Your number is " + ParamMatchTypeEnum.PARAM_PREFIX + ParamMatchTypeEnum.randomNumber.name() + "(0,0)";

        // Test
        final String result = inboundParamMatchServiceImpl.processParamMatch(request, "/person/{name}", responseBody, sanitizedUserCtxInboundPath, userId);

        // Assertions
        final String remainder = result.replaceAll("Your number is ", "");
        Assertions.assertTrue(NumberUtils.isDigits(remainder));
        Assertions.assertEquals(Integer.valueOf(0), Integer.valueOf(remainder));
    }

    @Test
    void processParamMatch_randomNumberNoParams_Test() {

        // Setup
        final String responseBody = "Your number is " + ParamMatchTypeEnum.PARAM_PREFIX + ParamMatchTypeEnum.randomNumber.name() + "()";

        // Test & Assertions
        final IllegalArgumentException ex = Assertions.assertThrows(IllegalArgumentException.class,
                () -> inboundParamMatchServiceImpl.processParamMatch(request, "/person/{name}", responseBody, sanitizedUserCtxInboundPath, userId));
        Assertions.assertEquals("randomNumber is missing args", ex.getMessage());

    }

    @Test
    void processParamMatch_kvpMatch_Test() {

        // Setup
        final String responseBody = "I say " + ParamMatchTypeEnum.PARAM_PREFIX + ParamMatchTypeEnum.lookUpKvp + "(Hello)";

        // Mock
        Mockito.when(userKeyValueDataService.loadByKey(Mockito.anyString(), Mockito.anyLong()))
                .thenReturn(new UserKeyValueDataDTO(GeneralUtils.generateUUID(), "Hello", "Bonjour"));

        // Test
        final String result = inboundParamMatchServiceImpl.processParamMatch(request, "/person", responseBody, sanitizedUserCtxInboundPath, userId);

        // Assertions
        Assertions.assertEquals("I say Bonjour", result);
    }

    @Test
    void processParamMatch_kvpNoMatch_Test() {

        // Setup
        final String responseBody = "I say " + ParamMatchTypeEnum.PARAM_PREFIX + ParamMatchTypeEnum.lookUpKvp + "(Hello)";

        // Mock
        Mockito.when(userKeyValueDataService.loadByKey(Mockito.anyString(), Mockito.anyLong()))
                .thenReturn(null);

        // Test
        final String result = inboundParamMatchServiceImpl.processParamMatch(request, "/person", responseBody, sanitizedUserCtxInboundPath, userId);

        // Assertions
        Assertions.assertEquals("I say ", result);
    }

    @Test
    void processParamMatch_kvpNestedRequestBodyMatch_Test() {

        // Setup
        final String responseBody = "I say " + ParamMatchTypeEnum.PARAM_PREFIX + ParamMatchTypeEnum.lookUpKvp + "(" + ParamMatchTypeEnum.PARAM_PREFIX + ParamMatchTypeEnum.requestBody + ")";

        // Mock
        request.setContent("greeting".getBytes(StandardCharsets.UTF_8));
        Mockito.when(userKeyValueDataService.loadByKey(Mockito.anyString(), Mockito.anyLong()))
                .thenReturn(new UserKeyValueDataDTO(GeneralUtils.generateUUID(), "greeting", "Good day!"));

        // Test
        final String result = inboundParamMatchServiceImpl.processParamMatch(request, "/person", responseBody, sanitizedUserCtxInboundPath, userId);

        // Assertions
        Assertions.assertEquals("I say Good day!", result);
    }

    @Test
    void processParamMatch_kvpNestedRequestParamMatch_Test() {

        // Setup
        final String responseBody = "Watcha " + ParamMatchTypeEnum.PARAM_PREFIX + ParamMatchTypeEnum.lookUpKvp + "(" + ParamMatchTypeEnum.PARAM_PREFIX + ParamMatchTypeEnum.requestParameter + "(name)" + ")";

        // Mock
        request.addParameter("name", new String[]{"Max"});
        Mockito.when(userKeyValueDataService.loadByKey(Mockito.anyString(), Mockito.anyLong()))
                .thenReturn(new UserKeyValueDataDTO(GeneralUtils.generateUUID(), "max", "Your name is Max"));

        // Test
        final String result = inboundParamMatchServiceImpl.processParamMatch(request, "/person", responseBody, sanitizedUserCtxInboundPath, userId);

        // Assertions
        Assertions.assertEquals("Watcha Your name is Max", result);
    }

    @Test
    void processParamMatch_kvpNestedPathVarMatch_Test() {

        // Setup
        final String responseBody = "Watcha " + ParamMatchTypeEnum.PARAM_PREFIX + ParamMatchTypeEnum.lookUpKvp + "(" + ParamMatchTypeEnum.PARAM_PREFIX + ParamMatchTypeEnum.pathVar + "(name)" + ")";

        // Mock
        sanitizedUserCtxInboundPath = "/person/max";
        Mockito.when(userKeyValueDataService.loadByKey(Mockito.anyString(), Mockito.anyLong()))
                .thenReturn(new UserKeyValueDataDTO(GeneralUtils.generateUUID(), "max", "Your name is Max"));

        // Test
        final String result = inboundParamMatchServiceImpl.processParamMatch(request, "/person/{name}", responseBody, sanitizedUserCtxInboundPath, userId);

        // Assertions
        Assertions.assertEquals("Watcha Your name is Max", result);
    }

    @Test
    void processParamMatch_kvpNestedRequestHeaderMatch_Test() {

        // Setup
        final String responseBody = "Watcha " + ParamMatchTypeEnum.PARAM_PREFIX + ParamMatchTypeEnum.lookUpKvp + "(" + ParamMatchTypeEnum.PARAM_PREFIX + ParamMatchTypeEnum.requestHeader + "(name)" + ")";

        // Mock
        request.addHeader("name", "Max");
        Mockito.when(userKeyValueDataService.loadByKey(Mockito.anyString(), Mockito.anyLong()))
                .thenReturn(new UserKeyValueDataDTO(GeneralUtils.generateUUID(), "max", "Your name is Max"));

        // Test
        final String result = inboundParamMatchServiceImpl.processParamMatch(request, "/person", responseBody, sanitizedUserCtxInboundPath, userId);

        // Assertions
        Assertions.assertEquals("Watcha Your name is Max", result);
    }

    @Test
    void processParamMatch_kvpNestedInvalidParam_Test() {

        // Setup
        final String responseBody = "Watcha " + ParamMatchTypeEnum.PARAM_PREFIX + ParamMatchTypeEnum.lookUpKvp + "(" + ParamMatchTypeEnum.PARAM_PREFIX + "XXX(name)" + ")";

        // Test
        final String result = inboundParamMatchServiceImpl.processParamMatch(request, "/person", responseBody, sanitizedUserCtxInboundPath, userId);

        // Assertions
        Assertions.assertEquals("Watcha ", result);
    }

}
