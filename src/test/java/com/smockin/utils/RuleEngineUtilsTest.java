package com.smockin.utils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import org.springframework.mock.web.MockHttpServletRequest;

import java.util.Optional;

class RuleEngineUtilsTest {

    @Test
    void matchOnPathVariable_WildcardIndexAligned_Test() {

        // Setup
        final MockHttpServletRequest req = new MockHttpServletRequest();
        req.setPathInfo("/person/123/details");
        req.setRequestURI(req.getPathInfo());

        // Test
        final String result = RuleEngineUtils.matchOnPathVariable("1", req, "/person/*/details");

        // Assertions
        Assertions.assertEquals("123", result);
    }

    @Test
    void matchOnPathVariable_InvalidArgPosition_Test() {

        // Setup
        final MockHttpServletRequest req = new MockHttpServletRequest();
        req.setPathInfo("/person/123/details");

        // Test & Assertions
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> RuleEngineUtils.matchOnPathVariable("abc", req, "/person/*/details"));
    }

    @Test
    void matchOnPathVariable_NoWildcardForIndex_Test() {

        // Setup
        final MockHttpServletRequest req = new MockHttpServletRequest();
        req.setPathInfo("/person/123/details");

        // Test & Assertions
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> RuleEngineUtils.matchOnPathVariable("1", req, "/person/{id}/details"));
    }

    @Test
    void matchOnJsonField_NestedListField_Test() {

        // Setup
        final String reqBody = "{\"person\":{\"pets\":[{\"type\":\"dog\"},{\"type\":\"cat\"}]}}";

        // Test
        final String result = RuleEngineUtils.matchOnJsonField("person.pets[1].type", reqBody);

        // Assertions
        Assertions.assertEquals("cat", result);
    }

    @Test
    void matchOnJsonField_InvalidListPosition_Test() {

        // Setup
        final String reqBody = "{\"person\":{\"pets\":[{\"type\":\"dog\"}]}}";

        // Test
        final String result = RuleEngineUtils.matchOnJsonField("person.pets[a].type", reqBody);

        // Assertions
        Assertions.assertNull(result);
    }

    @Test
    void matchOnJsonField_TopLevelListField_Test() {

        // Setup
        final String reqBody = "[\"dog\",\"cat\"]";

        // Test
        final String result = RuleEngineUtils.matchOnJsonField("[0]", reqBody);

        // Assertions
        Assertions.assertEquals("dog", result);
    }

    @Test
    void matchOnJsonField_EmptyBody_Test() {

        // Test
        final String result = RuleEngineUtils.matchOnJsonField("person.name", " ");

        // Assertions
        Assertions.assertNull(result);
    }

    @Test
    void isJSONFieldAList_Test() {
        Assertions.assertTrue(RuleEngineUtils.isJSONFieldAList("pets[0]"));
        Assertions.assertTrue(RuleEngineUtils.isJSONFieldAList("[0]"));
        Assertions.assertFalse(RuleEngineUtils.isJSONFieldAList("pets"));
    }

    @Test
    void extractJSONFieldListFieldName_Test() {

        final Optional<String> listName = RuleEngineUtils.extractJSONFieldListFieldName("pets[0]");
        Assertions.assertTrue(listName.isPresent());
        Assertions.assertEquals("pets", listName.get());

        final Optional<String> emptyName = RuleEngineUtils.extractJSONFieldListFieldName("[0]");
        Assertions.assertFalse(emptyName.isPresent());
    }

    @Test
    void extractJSONFieldListPosition_Test() {

        Assertions.assertEquals(Integer.valueOf(2), RuleEngineUtils.extractJSONFieldListPosition("pets[2]"));
        Assertions.assertNull(RuleEngineUtils.extractJSONFieldListPosition("pets[a]"));
    }
}
