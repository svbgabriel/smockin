package com.smockin.utils;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.mock.web.MockHttpServletRequest;

import java.util.Optional;

public class RuleEngineUtilsTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void matchOnPathVariable_WildcardIndexAligned_Test() {

        // Setup
        final MockHttpServletRequest req = new MockHttpServletRequest();
        req.setPathInfo("/person/123/details");
        req.setRequestURI(req.getPathInfo());

        // Test
        final String result = RuleEngineUtils.matchOnPathVariable("1", req, "/person/*/details");

        // Assertions
        Assert.assertEquals("123", result);
    }

    @Test
    public void matchOnPathVariable_InvalidArgPosition_Test() {

        // Setup
        thrown.expect(IllegalArgumentException.class);
        final MockHttpServletRequest req = new MockHttpServletRequest();
        req.setPathInfo("/person/123/details");

        // Test
        RuleEngineUtils.matchOnPathVariable("abc", req, "/person/*/details");
    }

    @Test
    public void matchOnPathVariable_NoWildcardForIndex_Test() {

        // Setup
        thrown.expect(IllegalArgumentException.class);
        final MockHttpServletRequest req = new MockHttpServletRequest();
        req.setPathInfo("/person/123/details");

        // Test
        RuleEngineUtils.matchOnPathVariable("1", req, "/person/{id}/details");
    }

    @Test
    public void matchOnJsonField_NestedListField_Test() {

        // Setup
        final String reqBody = "{\"person\":{\"pets\":[{\"type\":\"dog\"},{\"type\":\"cat\"}]}}";

        // Test
        final String result = RuleEngineUtils.matchOnJsonField("person.pets[1].type", reqBody);

        // Assertions
        Assert.assertEquals("cat", result);
    }

    @Test
    public void matchOnJsonField_InvalidListPosition_Test() {

        // Setup
        final String reqBody = "{\"person\":{\"pets\":[{\"type\":\"dog\"}]}}";

        // Test
        final String result = RuleEngineUtils.matchOnJsonField("person.pets[a].type", reqBody);

        // Assertions
        Assert.assertNull(result);
    }

    @Test
    public void matchOnJsonField_TopLevelListField_Test() {

        // Setup
        final String reqBody = "[\"dog\",\"cat\"]";

        // Test
        final String result = RuleEngineUtils.matchOnJsonField("[0]", reqBody);

        // Assertions
        Assert.assertEquals("dog", result);
    }

    @Test
    public void matchOnJsonField_EmptyBody_Test() {

        // Test
        final String result = RuleEngineUtils.matchOnJsonField("person.name", " ");

        // Assertions
        Assert.assertNull(result);
    }

    @Test
    public void isJSONFieldAList_Test() {
        Assert.assertTrue(RuleEngineUtils.isJSONFieldAList("pets[0]"));
        Assert.assertTrue(RuleEngineUtils.isJSONFieldAList("[0]"));
        Assert.assertFalse(RuleEngineUtils.isJSONFieldAList("pets"));
    }

    @Test
    public void extractJSONFieldListFieldName_Test() {

        final Optional<String> listName = RuleEngineUtils.extractJSONFieldListFieldName("pets[0]");
        Assert.assertTrue(listName.isPresent());
        Assert.assertEquals("pets", listName.get());

        final Optional<String> emptyName = RuleEngineUtils.extractJSONFieldListFieldName("[0]");
        Assert.assertFalse(emptyName.isPresent());
    }

    @Test
    public void extractJSONFieldListPosition_Test() {

        Assert.assertEquals(Integer.valueOf(2), RuleEngineUtils.extractJSONFieldListPosition("pets[2]"));
        Assert.assertNull(RuleEngineUtils.extractJSONFieldListPosition("pets[a]"));
    }
}
