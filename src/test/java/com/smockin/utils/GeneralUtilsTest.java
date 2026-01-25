package com.smockin.utils;

import com.smockin.admin.enums.UserModeEnum;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpMethod;

import jakarta.servlet.http.HttpServletRequest;

import java.util.*;

/**
 * Created by mgallina on 08/08/17.
 */
class GeneralUtilsTest {

    @Test
    void generateUUID_Populated_Test() {
        Assertions.assertNotNull(GeneralUtils.generateUUID());
    }

    @Test
    void generateUUID_Distinct_Test() {
        Assertions.assertNotEquals(GeneralUtils.generateUUID(), GeneralUtils.generateUUID());
    }

    @Test
    void getCurrentDate_Populated_Test() {
        Assertions.assertNotNull(GeneralUtils.getCurrentDate());
    }

    @Test
    void findHeaderIgnoreCaseTest() {

        // Setup
        final HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
        Mockito.when(req.getHeader("name")).thenReturn("Bob");
        Mockito.when(req.getHeader("Age")).thenReturn("21");

        Vector<String> headerNames = new Vector<>(Arrays.asList("name", "Age"));
        Mockito.when(req.getHeaderNames()).thenReturn(headerNames.elements());

        // Test
        final String nameResult = GeneralUtils.findHeaderIgnoreCase(req, "NAME");

        // Assertions
        Assertions.assertNotNull(nameResult);
        Assertions.assertEquals("Bob", nameResult);

        // Test
        final String ageResult = GeneralUtils.findHeaderIgnoreCase(req, "age");

        // Assertions
        Assertions.assertNotNull(ageResult);
        Assertions.assertEquals("21", ageResult);
    }

    @Test
    void findRequestParamIgnoreCaseTest() {

        // Setup
        final HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
        Map<String, String[]> params = new HashMap<>();
        params.put("name", new String[]{"Bob"});
        params.put("Age", new String[]{"21"});

        Mockito.when(req.getParameterMap()).thenReturn(params);

        // Test
        final String nameResult = GeneralUtils.extractRequestParamByName(req, "NAME");

        // Assertions
        Assertions.assertNotNull(nameResult);
        Assertions.assertEquals("Bob", nameResult);

        // Test
        final String ageResult = GeneralUtils.extractRequestParamByName(req, "age");

        // Assertions
        Assertions.assertNotNull(ageResult);
        Assertions.assertEquals("21", ageResult);
    }

    @Test
    void findPathVarIgnoreCase1Test() {

        // Test
        final String nameResult = GeneralUtils.findPathVarIgnoreCase("/person/Bob", "/person/{name}", "NAME");

        // Assertions
        Assertions.assertNotNull(nameResult);
        Assertions.assertEquals("Bob", nameResult);
    }

    @Test
    void findPathVarIgnoreCase2Test() {

        // Test
        final String ageResult = GeneralUtils.findPathVarIgnoreCase("/person/21", "/person/{age}", "agE");

        // Assertions
        Assertions.assertNotNull(ageResult);
        Assertions.assertEquals("21", ageResult);
    }

    @Test
    void extractRequestParamByNameTest() {

        // Setup
        final HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
        final Map<String, String[]> params = new HashMap<>();
        params.put("name", new String[]{"bob"});

        Mockito.when(req.getParameterMap()).thenReturn(params);

        // Test
        final String result = GeneralUtils.extractRequestParamByName(req, "name");

        // Assertions
        Assertions.assertNotNull(result);
        Assertions.assertEquals("bob", result);
    }

    @Test
    void extractAllRequestParamsTest() {

        // Setup
        final HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
        final Map<String, String[]> params = new HashMap<>();
        params.put("name", new String[]{"bob"});
        params.put("age", new String[]{"27"});

        Mockito.when(req.getParameterMap()).thenReturn(params);

        // Test
        final Map<String, String> results = GeneralUtils.extractAllRequestParams(req);

        // Assertions
        Assertions.assertNotNull(results);
        Assertions.assertEquals(2, results.size());
        Assertions.assertEquals("bob", results.get("name"));
        Assertions.assertEquals("27", results.get("age"));
    }

    @Test
    void extractAllRequestParams_nullValues_Test() {

        // Setup
        final HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
        final Map<String, String[]> params = new HashMap<>();
        params.put("name", null);
        params.put("age", null);

        Mockito.when(req.getMethod()).thenReturn(HttpMethod.PATCH.name());
        Mockito.when(req.getParameterMap()).thenReturn(params);

        // Test
        final Map<String, String> results = GeneralUtils.extractAllRequestParams(req);

        // Assertions
        Assertions.assertNotNull(results);
        Assertions.assertEquals(2, results.size());
        Assertions.assertNull(results.get("name"));
        Assertions.assertNull(results.get("age"));

    }

    @Test
    void extractAllRequestParams_emptyMap_Test() {

        // Setup
        final HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
        Mockito.when(req.getParameterMap()).thenReturn(new HashMap<>());

        // Test
        final Map<String, String> results = GeneralUtils.extractAllRequestParams(req);

        // Assertions
        Assertions.assertNotNull(results);
        Assertions.assertEquals(0, results.size());

    }

    @Test
    void deserializeJSONToListTest() {

        // Test
        final List<Map<String, ?>> result = GeneralUtils.deserialiseJSONToList("[{\"fruit\":{\"name\":\"pear\"}},{\"fruit\":{\"name\":\"apple\"}}]");

        // Assertions
        Assertions.assertNotNull(result);
        Assertions.assertEquals(2, result.size());
        Assertions.assertNotNull(result.get(0));
        Assertions.assertNotNull(result.get(1));
        Assertions.assertNotNull(result.get(0).get("fruit"));
        Assertions.assertNotNull(result.get(1).get("fruit"));
        Assertions.assertTrue(result.get(0).get("fruit") instanceof Map);
        Assertions.assertTrue(result.get(1).get("fruit") instanceof Map);
        Assertions.assertTrue(((Map) result.get(0).get("fruit")).get("name") != null);
        Assertions.assertTrue(((Map) result.get(1).get("fruit")).get("name") != null);
        Assertions.assertEquals("pear", ((Map) result.get(0).get("fruit")).get("name"));
        Assertions.assertEquals("apple", ((Map) result.get(1).get("fruit")).get("name"));

    }

    @Test
    void deserializeJSONToListEmptyTest() {

        // Test
        final List<Map<String, ?>> result = GeneralUtils.deserialiseJSONToList("[]");

        // Assertions
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isEmpty());

    }

    @Test
    void deserializeJSONToListNullTest() {

        // Test
        final List<Map<String, ?>> result = GeneralUtils.deserialiseJSONToList(null);

        // Assertions
        Assertions.assertNull(result);

    }

    @Test
    void deserializeJSONToListBlankTest() {

        // Test
        final List<Map<String, ?>> result = GeneralUtils.deserialiseJSONToList(" ");

        // Assertions
        Assertions.assertNull(result);

    }

    @Test
    void removeJsCommentsTest() {

        // Setup
        final String jsSrc = "function doSomething(a,b) {\n"
                + "  var c = a;\n"
                + "  // hide this line\n"
                + "  var d = b; // hide this half of the line\n"
                + "  var e = c+d;\n"
                + "} // end of function";

        // Test
        final String result = GeneralUtils.removeJsComments(jsSrc);

        // Assertions
        Assertions.assertNotNull(result);
        Assertions.assertEquals("function doSomething(a,b) {\n" +
                "  var c = a;\n" +
                "  var d = b; \n" +
                "  var e = c+d;\n" +
                "}", result);
    }

    @Test
    void removeJsComments_noCommentsPresent_Test() {

        // Setup
        final String jsSrc = "function doSomething(a,b) {\n"
                + "  var c = a;\n"
                + "  var d = b;\n"
                + "  var e = c+d;\n"
                + "}";

        // Test
        final String result = GeneralUtils.removeJsComments(jsSrc);

        // Assertions
        Assertions.assertNotNull(result);
        Assertions.assertEquals("function doSomething(a,b) {\n" +
                "  var c = a;\n" +
                "  var d = b;\n" +
                "  var e = c+d;\n" +
                "}", result);
    }

    @Test
    void removeJsComments_singleLine_Test() {

        // Setup
        final String jsSrc = "function doSomething(a,b) { var c = a; var d = b; var e = c+d; } // end of line";

        // Test
        final String result = GeneralUtils.removeJsComments(jsSrc);

        // Assertions
        Assertions.assertNotNull(result);
        Assertions.assertEquals("function doSomething(a,b) { var c = a; var d = b; var e = c+d; }", result);
    }

    @Test
    void removeJsComments_nullInput_Test() {

        // Test & Assertions
        Assertions.assertNull(GeneralUtils.removeJsComments(null));
    }

    @Test
    void removeJsComments_BlankInput_Test() {

        // Test
        final String result = GeneralUtils.removeJsComments("");

        // Assertions
        Assertions.assertNotNull(result);
        Assertions.assertEquals("", result);
    }

    @Test
    void findAllPathVars_WildcardAndNamed_Test() {

        // Test
        final Map<String, String> result = GeneralUtils.findAllPathVars(
                "/person/123/details/ABC", "/person/*/details/{code}");

        // Assertions
        Assertions.assertNotNull(result);
        Assertions.assertEquals(2, result.size());
        Assertions.assertEquals("123", result.get("*1"));
        Assertions.assertEquals("ABC", result.get("code"));
    }

    @Test
    void findAllPathVars_InboundShorter_ReturnsEmpty_Test() {

        // Test
        final Map<String, String> result = GeneralUtils.findAllPathVars(
                "/person/123", "/person/123/details");

        // Assertions
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    void sanitizeMultiUserPath_activeRemovesContext_Test() {

        // Test
        final String result = GeneralUtils.sanitizeMultiUserPath(
                UserModeEnum.ACTIVE, "/ctx/api/ping", "/ctx");

        // Assertions
        Assertions.assertEquals("/api/ping", result);
    }

    @Test
    void prefixPath_handlesBlankAndMissingSlash_Test() {

        // Test & Assertions
        Assertions.assertNull(GeneralUtils.prefixPath(" "));
        Assertions.assertEquals("/test", GeneralUtils.prefixPath("test"));
        Assertions.assertEquals("/test", GeneralUtils.prefixPath("/test"));
    }

}
