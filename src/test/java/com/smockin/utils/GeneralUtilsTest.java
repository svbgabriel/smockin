package com.smockin.utils;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;
import org.springframework.http.HttpMethod;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

/**
 * Created by mgallina on 08/08/17.
 */
public class GeneralUtilsTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void generateUUID_Populated_Test() {
        Assert.assertNotNull(GeneralUtils.generateUUID());
    }

    @Test
    public void generateUUID_Distinct_Test() {
        Assert.assertNotEquals(GeneralUtils.generateUUID(), GeneralUtils.generateUUID());
    }

    @Test
    public void getCurrentDate_Populated_Test() {
        Assert.assertNotNull(GeneralUtils.getCurrentDate());
    }

    @Test
    public void findHeaderIgnoreCaseTest() {

        // Setup
        final HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
        Mockito.when(req.getHeader("name")).thenReturn("Bob");
        Mockito.when(req.getHeader("Age")).thenReturn("21");

        Vector<String> headerNames = new Vector<>(Arrays.asList("name", "Age"));
        Mockito.when(req.getHeaderNames()).thenReturn(headerNames.elements());

        // Test
        final String nameResult = GeneralUtils.findHeaderIgnoreCase(req, "NAME");

        // Assertions
        Assert.assertNotNull(nameResult);
        Assert.assertEquals("Bob", nameResult);

        // Test
        final String ageResult = GeneralUtils.findHeaderIgnoreCase(req, "age");

        // Assertions
        Assert.assertNotNull(ageResult);
        Assert.assertEquals("21", ageResult);
    }

    @Test
    public void findRequestParamIgnoreCaseTest() {

        // Setup
        final HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
        Map<String, String[]> params = new HashMap<>();
        params.put("name", new String[] { "Bob" });
        params.put("Age", new String[] { "21" });

        Mockito.when(req.getParameterMap()).thenReturn(params);

        // Test
        final String nameResult = GeneralUtils.extractRequestParamByName(req, "NAME");

        // Assertions
        Assert.assertNotNull(nameResult);
        Assert.assertEquals("Bob", nameResult);

        // Test
        final String ageResult = GeneralUtils.extractRequestParamByName(req, "age");

        // Assertions
        Assert.assertNotNull(ageResult);
        Assert.assertEquals("21", ageResult);
    }

    @Test
    public void findPathVarIgnoreCase1Test() {

        // Test
        final String nameResult = GeneralUtils.findPathVarIgnoreCase("/person/Bob", "/person/{name}", "NAME");

        // Assertions
        Assert.assertNotNull(nameResult);
        Assert.assertEquals("Bob", nameResult);
    }

    @Test
    public void findPathVarIgnoreCase2Test() {

        // Test
        final String ageResult = GeneralUtils.findPathVarIgnoreCase("/person/21", "/person/{age}", "agE");

        // Assertions
        Assert.assertNotNull(ageResult);
        Assert.assertEquals("21", ageResult);
    }

    @Test
    public void extractRequestParamByNameTest() {

        // Setup
        final HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
        final Map<String, String[]> params = new HashMap<>();
        params.put("name", new String[] { "bob" });

        Mockito.when(req.getParameterMap()).thenReturn(params);

        // Test
        final String result = GeneralUtils.extractRequestParamByName(req, "name");

        // Assertions
        Assert.assertNotNull(result);
        Assert.assertEquals("bob", result);
    }

    @Test
    public void extractAllRequestParamsTest() {

        // Setup
        final HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
        final Map<String, String[]> params = new HashMap<>();
        params.put("name", new String[] { "bob" });
        params.put("age", new String[] { "27" });

        Mockito.when(req.getParameterMap()).thenReturn(params);

        // Test
        final Map<String, String> results = GeneralUtils.extractAllRequestParams(req);

        // Assertions
        Assert.assertNotNull(results);
        Assert.assertEquals(2, results.size());
        Assert.assertEquals("bob", results.get("name"));
        Assert.assertEquals("27", results.get("age"));
    }

    @Test
    public void extractAllRequestParams_nullValues_Test() {

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
        Assert.assertNotNull(results);
        Assert.assertEquals(2, results.size());
        Assert.assertNull(results.get("name"));
        Assert.assertNull(results.get("age"));

    }

    @Test
    public void extractAllRequestParams_emptyMap_Test() {

        // Setup
        final HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
        Mockito.when(req.getParameterMap()).thenReturn(new HashMap<>());

        // Test
        final Map<String, String> results = GeneralUtils.extractAllRequestParams(req);

        // Assertions
        Assert.assertNotNull(results);
        Assert.assertEquals(0, results.size());

    }

    @Test
    public void deserialiseJSONToListTest() {

        // Test
        final List<Map<String, ?>> result = GeneralUtils.deserialiseJSONToList("[{\"fruit\":{\"name\":\"pear\"}},{\"fruit\":{\"name\":\"apple\"}}]");

        // Assertions
        Assert.assertNotNull(result);
        Assert.assertEquals(2, result.size());
        Assert.assertNotNull(result.get(0));
        Assert.assertNotNull(result.get(1));
        Assert.assertNotNull(result.get(0).get("fruit"));
        Assert.assertNotNull(result.get(1).get("fruit"));
        Assert.assertTrue(result.get(0).get("fruit") instanceof Map);
        Assert.assertTrue(result.get(1).get("fruit") instanceof Map);
        Assert.assertTrue(((Map)result.get(0).get("fruit")).get("name") != null);
        Assert.assertTrue(((Map)result.get(1).get("fruit")).get("name") != null);
        Assert.assertEquals("pear", ((Map)result.get(0).get("fruit")).get("name"));
        Assert.assertEquals("apple", ((Map)result.get(1).get("fruit")).get("name"));

    }

    @Test
    public void deserialiseJSONToListEmptyTest() {

        // Test
        final List<Map<String, ?>> result = GeneralUtils.deserialiseJSONToList("[]");

        // Assertions
        Assert.assertNotNull(result);
        Assert.assertTrue(result.isEmpty());

    }

    @Test
    public void deserialiseJSONToListNullTest() {

        // Test
        final List<Map<String, ?>> result = GeneralUtils.deserialiseJSONToList(null);

        // Assertions
        Assert.assertNull(result);

    }

    @Test
    public void deserialiseJSONToListBlankTest() {

        // Test
        final List<Map<String, ?>> result = GeneralUtils.deserialiseJSONToList(" ");

        // Assertions
        Assert.assertNull(result);

    }

    @Test
    public void removeJsCommentsTest() {

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
        Assert.assertNotNull(result);
        Assert.assertEquals("function doSomething(a,b) {\n" +
                "  var c = a;\n" +
                "  var d = b; \n" +
                "  var e = c+d;\n" +
                "}", result);
    }

    @Test
    public void removeJsComments_noCommentsPresent_Test() {

        // Setup
        final String jsSrc = "function doSomething(a,b) {\n"
                + "  var c = a;\n"
                + "  var d = b;\n"
                + "  var e = c+d;\n"
                + "}";

        // Test
        final String result = GeneralUtils.removeJsComments(jsSrc);

        // Assertions
        Assert.assertNotNull(result);
        Assert.assertEquals("function doSomething(a,b) {\n" +
                "  var c = a;\n" +
                "  var d = b;\n" +
                "  var e = c+d;\n" +
                "}", result);
    }

    @Test
    public void removeJsComments_singleLine_Test() {

        // Setup
        final String jsSrc = "function doSomething(a,b) { var c = a; var d = b; var e = c+d; } // end of line";

        // Test
        final String result = GeneralUtils.removeJsComments(jsSrc);

        // Assertions
        Assert.assertNotNull(result);
        Assert.assertEquals("function doSomething(a,b) { var c = a; var d = b; var e = c+d; }", result);
    }

    @Test
    public void removeJsComments_nullInput_Test() {

        // Test & Assertions
        Assert.assertNull(GeneralUtils.removeJsComments(null));
    }

    @Test
    public void removeJsComments_BlankInput_Test() {

        // Test
        final String result = GeneralUtils.removeJsComments("");

        // Assertions
        Assert.assertNotNull(result);
        Assert.assertEquals("", result);
    }

}
