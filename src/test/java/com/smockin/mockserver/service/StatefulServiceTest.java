package com.smockin.mockserver.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.smockin.admin.persistence.entity.RestfulMockStatefulMeta;
import com.smockin.utils.GeneralUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

class StatefulServiceTest {

    private StatefulServiceImpl statefulServiceImpl;

    @BeforeEach
    void setUp() {

        statefulServiceImpl = new StatefulServiceImpl();

    }

    @Test
    void findStateRecordPath_inMultipleMapRecords_Test() {

        // Setup
        final String json = "[{\"jsonapi\":{\"version\":\"1.0\"},\"data\":{\"id\":\"1\",\"type\":\"customers\",\"name\":\"Bob\"},\"included\":[]},{\"jsonapi\":{\"version\":\"1.0\"},\"data\":{\"id\":\"2\",\"type\":\"customers\",\"name\":\"Mike\"},\"included\":[]},{\"jsonapi\":{\"version\":\"1.0\"},\"data\":{\"id\":\"3\",\"type\":\"customers\",\"name\":\"Pete\"},\"included\":[]}]";

        final List<Map<String, Object>> allState = GeneralUtils.deserializeJson(json,
                new TypeReference<List<Map<String, Object>>>() {
                });

        final String[] pathArray = {"data", "id"};
        final String targetId = "2";

        // Test
        final Optional<StatefulServiceImpl.StatefulPath> outcome = statefulServiceImpl.findDataStateRecordPath(allState, pathArray, targetId);

        // Assertions
        Assertions.assertTrue(outcome.isPresent());
        Assertions.assertEquals("[1].data.id=2", outcome.get().path());
        Assertions.assertEquals(Integer.valueOf(1), outcome.get().index());

    }

    @Test
    void findStateRecordPath_inMultipleMapRecordsWithDataLists_Test() {

        // Setup
        final String json = "[{\"jsonapi\":{\"version\":\"1.0\"},\"data\":[{\"id\":\"1\",\"type\":\"customers\",\"name\":\"Bob\"}],\"included\":[]},{\"jsonapi\":{\"version\":\"1.0\"},\"data\":[{\"id\":\"2\",\"type\":\"customers\",\"name\":\"Mike\"}],\"included\":[]},{\"jsonapi\":{\"version\":\"1.0\"},\"data\":[{\"id\":\"3\",\"type\":\"customers\",\"name\":\"Pete\"}],\"included\":[]}]";

        final List<Map<String, Object>> allState = GeneralUtils.deserializeJson(json,
                new TypeReference<List<Map<String, Object>>>() {
                });

        final String[] pathArray = {"data", "id"};
        final String targetId = "3";

        // Test
        final Optional<StatefulServiceImpl.StatefulPath> outcome = statefulServiceImpl.findDataStateRecordPath(allState, pathArray, targetId);

        // Assertions
        Assertions.assertTrue(outcome.isPresent());
        Assertions.assertEquals("[2].data.[0].id=3", outcome.get().path());
        Assertions.assertEquals(Integer.valueOf(2), outcome.get().index());

    }

    @Test
    void findStateRecordPath_inSingleDataList_Test() {

        // Setup
        final String json = "[{\"jsonapi\":{\"version\":\"1.0\"},\"data\":[{\"id\":\"1\",\"type\":\"customers\",\"name\":\"Bob\"},{\"id\":\"2\",\"type\":\"customers\",\"name\":\"Billy\"},{\"id\":\"3\",\"type\":\"customers\",\"name\":\"Sally\"},{\"id\":\"4\",\"type\":\"customers\",\"name\":\"Jennifer\"}],\"included\":[]}]";

        final List<Map<String, Object>> allState = GeneralUtils.deserializeJson(json,
                new TypeReference<List<Map<String, Object>>>() {
                });

        final String[] pathArray = {"data", "id"};
        final String targetId = "3";

        // Test
        final Optional<StatefulServiceImpl.StatefulPath> outcome = statefulServiceImpl.findDataStateRecordPath(allState, pathArray, targetId);

        // Assertions
        Assertions.assertTrue(outcome.isPresent());
        Assertions.assertEquals("[0].data.[2].id=3", outcome.get().path());
        Assertions.assertEquals(Integer.valueOf(0), outcome.get().index());

    }

    @Test
    void findStateRecordPath_withOddNestedIdJsonStructure_Test() {

        // Setup
        final String json = "[{\"jsonapi\":{\"version\":\"1.0\"},\"data\":[{\"id\":\"1\",\"type\":\"customers\",\"name\":\"Bob\"},{\"id\":\"2\",\"type\":\"customers\",\"name\":\"Billy\"}],\"included\":[]},{\"jsonapi\":{\"version\":\"1.0\"},\"data\":[{\"id\":\"3\",\"type\":\"customers\",\"name\":\"Mike\"}],\"included\":[]}]";

        final List<Map<String, Object>> allState = GeneralUtils.deserializeJson(json,
                new TypeReference<List<Map<String, Object>>>() {
                });

        final String[] pathArray = {"data", "id"};
        final String targetId = "2";

        // Test
        final Optional<StatefulServiceImpl.StatefulPath> outcome = statefulServiceImpl.findDataStateRecordPath(allState, pathArray, targetId);

        // Assertions
        Assertions.assertTrue(outcome.isPresent());
        Assertions.assertEquals("[0].data.[1].id=2", outcome.get().path());
        Assertions.assertEquals(Integer.valueOf(0), outcome.get().index());

    }

    @Test
    void findStateRecordPath_withComplexIdJsonStructure_Test() {

        // Setup
        final String json = "[{\"foo1\":\"bar1\",\"foo2\":1,\"foo3\":true,\"jsonapi\":{\"version\":\"1.0\"},\"data\":[{\"data1\":[{\"id\":\"1\",\"type\":\"customers\",\"name\":\"Bob\"}]},{\"data1\":[{\"id\":\"2\",\"type\":\"customers\",\"name\":\"Max\"},{\"id\":\"3\",\"type\":\"customers\",\"name\":\"Jane\"},{\"id\":\"4\",\"type\":\"customers\",\"name\":\"Sam\"}]},{\"data1\":[{\"id\":\"5\",\"type\":\"customers\",\"name\":\"Darren\"},{\"id\":\"6\",\"type\":\"customers\",\"name\":\"Mandy\"}]}],\"included\":[]},{\"foo1\":\"bar2\",\"foo2\":2,\"foo3\":true,\"jsonapi\":{\"version\":\"1.0\"},\"data\":[{\"data1\":[{\"id\":\"7\",\"type\":\"customers\",\"name\":\"Bob\"}]},{\"data1\":[{\"id\":\"8\",\"type\":\"customers\",\"name\":\"Max\"},{\"id\":\"9\",\"type\":\"customers\",\"name\":\"Jane\"},{\"id\":\"10\",\"type\":\"customers\",\"name\":\"Sam\"}]},{\"data1\":[{\"id\":\"11\",\"type\":\"customers\",\"name\":\"Darren\"},{\"id\":\"12\",\"type\":\"customers\",\"name\":\"Mandy\"}]}],\"included\":[]}]";

        final List<Map<String, Object>> allState = GeneralUtils.deserializeJson(json,
                new TypeReference<List<Map<String, Object>>>() {
                });

        final String[] pathArray = {"data", "data1", "id"};
        final String targetId = "5";

        // Test
        final Optional<StatefulServiceImpl.StatefulPath> outcome = statefulServiceImpl.findDataStateRecordPath(allState, pathArray, targetId);

        // Assertions
        Assertions.assertTrue(outcome.isPresent());
        Assertions.assertEquals("[0].data.[2].data1.[0].id=5", outcome.get().path());
        Assertions.assertEquals(Integer.valueOf(0), outcome.get().index());

    }

    @Test
    void findStateRecordPath_withComplexIdJsonStructure_Test2() {

        // Setup
        final String json = "[{\"foo1\":\"bar1\",\"foo2\":1,\"foo3\":true,\"jsonapi\":{\"version\":\"1.0\"},\"data\":[{\"data1\":[{\"id\":\"1\",\"type\":\"customers\",\"name\":\"Bob\"}]},{\"data1\":[{\"id\":\"2\",\"type\":\"customers\",\"name\":\"Max\"},{\"id\":\"3\",\"type\":\"customers\",\"name\":\"Jane\"},{\"id\":\"4\",\"type\":\"customers\",\"name\":\"Sam\"}]},{\"data1\":[{\"id\":\"5\",\"type\":\"customers\",\"name\":\"Darren\"},{\"id\":\"6\",\"type\":\"customers\",\"name\":\"Mandy\"}]}],\"included\":[]},{\"foo1\":\"bar2\",\"foo2\":2,\"foo3\":true,\"jsonapi\":{\"version\":\"1.0\"},\"data\":[{\"data1\":[{\"id\":\"7\",\"type\":\"customers\",\"name\":\"Bob\"}]},{\"data1\":[{\"id\":\"8\",\"type\":\"customers\",\"name\":\"Max\"},{\"id\":\"9\",\"type\":\"customers\",\"name\":\"Jane\"},{\"id\":\"10\",\"type\":\"customers\",\"name\":\"Sam\"}]},{\"data1\":[{\"id\":\"11\",\"type\":\"customers\",\"name\":\"Darren\"},{\"id\":\"12\",\"type\":\"customers\",\"name\":\"Mandy\"}]}],\"included\":[]}]";

        final List<Map<String, Object>> allState = GeneralUtils.deserializeJson(json,
                new TypeReference<List<Map<String, Object>>>() {
                });

        final String[] pathArray = {"data", "data1", "id"};
        final String targetId = "7";

        // Test
        final Optional<StatefulServiceImpl.StatefulPath> outcome = statefulServiceImpl.findDataStateRecordPath(allState, pathArray, targetId);

        // Assertions
        Assertions.assertTrue(outcome.isPresent());
        Assertions.assertEquals("[1].data.[0].data1.[0].id=7", outcome.get().path());
        Assertions.assertEquals(Integer.valueOf(1), outcome.get().index());

    }

    @Test
    void findStateRecordPath_withComplexIdJsonStructure_Test3() {

        // Setup
        final String json = "[{\"foo1\":\"bar1\",\"foo2\":1,\"foo3\":true,\"jsonapi\":{\"version\":\"1.0\"},\"data\":[{\"data1\":[{\"id\":\"1\",\"type\":\"customers\",\"name\":\"Bob\"}]},{\"data1\":[{\"id\":\"2\",\"type\":\"customers\",\"name\":\"Max\"},{\"id\":\"3\",\"type\":\"customers\",\"name\":\"Jane\"},{\"id\":\"4\",\"type\":\"customers\",\"name\":\"Sam\"}]},{\"data1\":[{\"id\":\"5\",\"type\":\"customers\",\"name\":\"Darren\"},{\"id\":\"6\",\"type\":\"customers\",\"name\":\"Mandy\"}]}],\"included\":[]},{\"foo1\":\"bar2\",\"foo2\":2,\"foo3\":true,\"jsonapi\":{\"version\":\"1.0\"},\"data\":[{\"data1\":[{\"id\":\"7\",\"type\":\"customers\",\"name\":\"Bob\"}]},{\"data1\":[{\"id\":\"8\",\"type\":\"customers\",\"name\":\"Max\"},{\"id\":\"9\",\"type\":\"customers\",\"name\":\"Jane\"},{\"id\":\"10\",\"type\":\"customers\",\"name\":\"Sam\"}]},{\"data1\":[{\"id\":\"11\",\"type\":\"customers\",\"name\":\"Darren\"},{\"id\":\"12\",\"type\":\"customers\",\"name\":\"Mandy\"}]}],\"included\":[]}]";

        final List<Map<String, Object>> allState = GeneralUtils.deserializeJson(json,
                new TypeReference<List<Map<String, Object>>>() {
                });

        final String[] pathArray = {"data", "data1", "id"};
        final String targetId = "10";

        // Test
        final Optional<StatefulServiceImpl.StatefulPath> outcome = statefulServiceImpl.findDataStateRecordPath(allState, pathArray, targetId);

        // Assertions
        Assertions.assertTrue(outcome.isPresent());
        Assertions.assertEquals("[1].data.[1].data1.[2].id=10", outcome.get().path());
        Assertions.assertEquals(Integer.valueOf(1), outcome.get().index());

    }

    @Test
    void extractArrayPositionTest() {

        // Test
        final int positionOutcome1 = statefulServiceImpl.extractArrayPosition("[1]");
        final int positionOutcome2 = statefulServiceImpl.extractArrayPosition("[2]");

        // Assertions
        Assertions.assertEquals(1, positionOutcome1);
        Assertions.assertEquals(2, positionOutcome2);
    }

    @Test
    void extractArrayPositionNullTest() {

        // Test & Assertions
        Assertions.assertNull(statefulServiceImpl.extractArrayPosition(null));
    }

    @Test
    void extractArrayPositionInvalidCharTest() {

        // Test & Assertions
        Assertions.assertNull(statefulServiceImpl.extractArrayPosition("[x]"));
    }

    @Test
    void extractArrayPositionInvalidPathTest() {

        // Test & Assertions
        Assertions.assertNull(statefulServiceImpl.extractArrayPosition("xxx"));
    }

    @Test
    void findStateRecordByPath_inSingleDataList_Test() {

        // Setup
        final String json = "[{\"jsonapi\":{\"version\":\"1.0\"},\"data\":[{\"id\":\"1\",\"type\":\"customers\",\"name\":\"Bob\"},{\"id\":\"2\",\"type\":\"customers\",\"name\":\"Billy\"},{\"id\":\"3\",\"type\":\"customers\",\"name\":\"Sally\"},{\"id\":\"4\",\"type\":\"customers\",\"name\":\"Jennifer\"}],\"included\":[]}]";

        final List<Map<String, Object>> allState = GeneralUtils.deserializeJson(json,
                new TypeReference<List<Map<String, Object>>>() {
                });

        final String jsonPath = "[0].data.[1].id=2";

        // Test
        final Optional<Map<String, Object>> result = statefulServiceImpl.findDataStateRecordByPath(allState, jsonPath);

        // Assertions
        Assertions.assertTrue(result.isPresent());
        Assertions.assertNotNull(result.get());

        Assertions.assertNotNull(result.get().get("data"));
        Assertions.assertTrue(result.get().get("data") instanceof List);
        Assertions.assertEquals(1, ((List) result.get().get("data")).size());
        Assertions.assertEquals("2", ((Map) ((List) result.get().get("data")).get(0)).get("id"));
        Assertions.assertEquals("Billy", ((Map) ((List) result.get().get("data")).get(0)).get("name"));

        // Ensure cached state list remains unmodified.
        Assertions.assertEquals(1, allState.size());
        Assertions.assertTrue(allState.get(0).get("data") instanceof List);
        Assertions.assertEquals(4, ((List) allState.get(0).get("data")).size());

    }

    @Test
    void findStateRecordByPath_inMultipleMapRecords_Test() {

        // Setup
        final String json = "[{\"jsonapi\":{\"version\":\"1.0\"},\"data\":{\"id\":\"1\",\"type\":\"customers\",\"name\":\"Bob\"},\"included\":[]},{\"jsonapi\":{\"version\":\"1.0\"},\"data\":{\"id\":\"2\",\"type\":\"customers\",\"name\":\"Mike\"},\"included\":[]},{\"jsonapi\":{\"version\":\"1.0\"},\"data\":{\"id\":\"3\",\"type\":\"customers\",\"name\":\"Pete\"},\"included\":[]}]";

        final List<Map<String, Object>> allState = GeneralUtils.deserializeJson(json,
                new TypeReference<List<Map<String, Object>>>() {
                });

        final String jsonPath = "[1].data.id=2";

        // Test
        final Optional<Map<String, Object>> result = statefulServiceImpl.findDataStateRecordByPath(allState, jsonPath);

        // Assertions
        Assertions.assertTrue(result.isPresent());
        Assertions.assertNotNull(result.get());
        Assertions.assertNotNull(result.get().get("data"));
        Assertions.assertTrue(result.get().get("data") instanceof Map);
        Assertions.assertEquals("2", ((Map) result.get().get("data")).get("id"));
        Assertions.assertEquals("Mike", ((Map) result.get().get("data")).get("name"));

    }

    @Test
    void findStateRecordByPath_inMultipleMapRecordsWithDataLists_Test() {

        // Setup
        final String json = "[{\"jsonapi\":{\"version\":\"1.0\"},\"data\":[{\"id\":\"1\",\"type\":\"customers\",\"name\":\"Bob\"}],\"included\":[]},{\"jsonapi\":{\"version\":\"1.0\"},\"data\":[{\"id\":\"2\",\"type\":\"customers\",\"name\":\"Mike\"}],\"included\":[]},{\"jsonapi\":{\"version\":\"1.0\"},\"data\":[{\"id\":\"3\",\"type\":\"customers\",\"name\":\"Pete\"}],\"included\":[]}]";

        final List<Map<String, Object>> allState = GeneralUtils.deserializeJson(json,
                new TypeReference<List<Map<String, Object>>>() {
                });

        final String jsonPath = "[2].data.[0].id=3";

        // Test
        final Optional<Map<String, Object>> result = statefulServiceImpl.findDataStateRecordByPath(allState, jsonPath);

        // Assertions
        Assertions.assertTrue(result.isPresent());
        Assertions.assertNotNull(result.get());
        Assertions.assertNotNull(result.get().get("data"));
        Assertions.assertTrue(result.get().get("data") instanceof List);
        Assertions.assertEquals(1, ((List) result.get().get("data")).size());
        Assertions.assertEquals("3", ((Map) ((List) result.get().get("data")).get(0)).get("id"));
        Assertions.assertEquals("Pete", ((Map) ((List) result.get().get("data")).get(0)).get("name"));

    }

    @Test
    void findStateRecordByPath_withOddNestedIdJsonStructure_Test() {

        // Setup
        final String json = "[{\"jsonapi\":{\"version\":\"1.0\"},\"data\":[{\"id\":\"1\",\"type\":\"customers\",\"name\":\"Bob\"},{\"id\":\"2\",\"type\":\"customers\",\"name\":\"Billy\"}],\"included\":[]},{\"jsonapi\":{\"version\":\"1.0\"},\"data\":[{\"id\":\"3\",\"type\":\"customers\",\"name\":\"Mike\"}],\"included\":[]}]";

        final List<Map<String, Object>> allState = GeneralUtils.deserializeJson(json,
                new TypeReference<List<Map<String, Object>>>() {
                });

        final String jsonPath = "[0].data.[1].id=2";

        // Test
        final Optional<Map<String, Object>> result = statefulServiceImpl.findDataStateRecordByPath(allState, jsonPath);

        // Assertions
        Assertions.assertTrue(result.isPresent());
        Assertions.assertNotNull(result.get());
        Assertions.assertNotNull(result.get().get("data"));
        Assertions.assertTrue(result.get().get("data") instanceof List);
        Assertions.assertEquals(1, ((List) result.get().get("data")).size());
        Assertions.assertEquals("2", ((Map) ((List) result.get().get("data")).get(0)).get("id"));
        Assertions.assertEquals("Billy", ((Map) ((List) result.get().get("data")).get(0)).get("name"));

    }

    @Test
    void findStateRecordByPath_withComplexIdJsonStructure1_Test() {

        // Setup
        final String json = "[{\"foo1\":\"bar1\",\"foo2\":1,\"foo3\":true,\"jsonapi\":{\"version\":\"1.0\"},\"data\":[{\"data1\":[{\"id\":\"1\",\"type\":\"customers\",\"name\":\"Bob\"}]},{\"data1\":[{\"id\":\"2\",\"type\":\"customers\",\"name\":\"Max\"},{\"id\":\"3\",\"type\":\"customers\",\"name\":\"Jane\"},{\"id\":\"4\",\"type\":\"customers\",\"name\":\"Sam\"}]},{\"data1\":[{\"id\":\"5\",\"type\":\"customers\",\"name\":\"Darren\"},{\"id\":\"6\",\"type\":\"customers\",\"name\":\"Mandy\"}]}],\"included\":[]},{\"foo1\":\"bar2\",\"foo2\":2,\"foo3\":true,\"jsonapi\":{\"version\":\"1.0\"},\"data\":[{\"data1\":[{\"id\":\"7\",\"type\":\"customers\",\"name\":\"Bob\"}]},{\"data1\":[{\"id\":\"8\",\"type\":\"customers\",\"name\":\"Max\"},{\"id\":\"9\",\"type\":\"customers\",\"name\":\"Jane\"},{\"id\":\"10\",\"type\":\"customers\",\"name\":\"Sam\"}]},{\"data1\":[{\"id\":\"11\",\"type\":\"customers\",\"name\":\"Darren\"},{\"id\":\"12\",\"type\":\"customers\",\"name\":\"Mandy\"}]}],\"included\":[]}]";

        final List<Map<String, Object>> allState = GeneralUtils.deserializeJson(json,
                new TypeReference<List<Map<String, Object>>>() {
                });

        final String jsonPath = "[1].data.[2].data1.[0].id=11";

        // Test
        final Optional<Map<String, Object>> result = statefulServiceImpl.findDataStateRecordByPath(allState, jsonPath);

        // Assertions
        Assertions.assertTrue(result.isPresent());
        Assertions.assertNotNull(result.get());
        Assertions.assertNotNull(result.get().get("data"));
        Assertions.assertNotNull(result.get().get("data") instanceof List);
        Assertions.assertEquals(1, ((List) result.get().get("data")).size());
        Assertions.assertTrue(((List) result.get().get("data")).get(0) instanceof Map);
        Assertions.assertTrue(((Map) ((List) result.get().get("data")).get(0)).get("data1") instanceof List);
        Assertions.assertEquals(1, ((List) ((Map) ((List) result.get().get("data")).get(0)).get("data1")).size());
        Assertions.assertTrue(((List) ((Map) ((List) result.get().get("data")).get(0)).get("data1")).get(0) instanceof Map);
        Assertions.assertEquals("11", ((Map) ((List) ((Map) ((List) result.get().get("data")).get(0)).get("data1")).get(0)).get("id"));
        Assertions.assertEquals("Darren", ((Map) ((List) ((Map) ((List) result.get().get("data")).get(0)).get("data1")).get(0)).get("name"));

    }

    @Test
    void findStateRecordByPath_withComplexIdJsonStructure2_Test() {

        // Setup
        final String json = "[{\"foo1\":\"bar1\",\"foo2\":1,\"foo3\":true,\"jsonapi\":{\"version\":\"1.0\"},\"data\":[{\"data1\":[{\"id\":\"1\",\"type\":\"customers\",\"name\":\"Bob\"}]},{\"data1\":[{\"id\":\"2\",\"type\":\"customers\",\"name\":\"Max\"},{\"id\":\"3\",\"type\":\"customers\",\"name\":\"Jane\"},{\"id\":\"4\",\"type\":\"customers\",\"name\":\"Sam\"}]},{\"data1\":[{\"id\":\"5\",\"type\":\"customers\",\"name\":\"Darren\"},{\"id\":\"6\",\"type\":\"customers\",\"name\":\"Mandy\"}]}],\"included\":[]},{\"foo1\":\"bar2\",\"foo2\":2,\"foo3\":true,\"jsonapi\":{\"version\":\"1.0\"},\"data\":[{\"data1\":[{\"id\":\"7\",\"type\":\"customers\",\"name\":\"Bob\"}]},{\"data1\":[{\"id\":\"8\",\"type\":\"customers\",\"name\":\"Max\"},{\"id\":\"9\",\"type\":\"customers\",\"name\":\"Jane\"},{\"id\":\"10\",\"type\":\"customers\",\"name\":\"Sam\"}]},{\"data1\":[{\"id\":\"11\",\"type\":\"customers\",\"name\":\"Darren\"},{\"id\":\"12\",\"type\":\"customers\",\"name\":\"Mandy\"}]}],\"included\":[]}]";

        final List<Map<String, Object>> allState = GeneralUtils.deserializeJson(json,
                new TypeReference<List<Map<String, Object>>>() {
                });

        final String jsonPath = "[1].data.[1].data1.[1].id=9";

        // Test
        final Optional<Map<String, Object>> result = statefulServiceImpl.findDataStateRecordByPath(allState, jsonPath);

        // Assertions
        Assertions.assertTrue(result.isPresent());
        Assertions.assertNotNull(result.get());
        Assertions.assertNotNull(result.get().get("data"));
        Assertions.assertNotNull(result.get().get("data") instanceof List);
        Assertions.assertEquals(1, ((List) result.get().get("data")).size());
        Assertions.assertTrue(((List) result.get().get("data")).get(0) instanceof Map);
        Assertions.assertTrue(((Map) ((List) result.get().get("data")).get(0)).get("data1") instanceof List);
        Assertions.assertEquals(1, ((List) ((Map) ((List) result.get().get("data")).get(0)).get("data1")).size());
        Assertions.assertTrue(((List) ((Map) ((List) result.get().get("data")).get(0)).get("data1")).get(0) instanceof Map);
        Assertions.assertEquals("9", ((Map) ((List) ((Map) ((List) result.get().get("data")).get(0)).get("data1")).get(0)).get("id"));
        Assertions.assertEquals("Jane", ((Map) ((List) ((Map) ((List) result.get().get("data")).get(0)).get("data1")).get(0)).get("name"));

    }

    @Test
    void findStateRecordByPath_IdMisMatch_Test() {

        // Setup
        final String json = "[{\"jsonapi\":{\"version\":\"1.0\"},\"data\":[{\"id\":\"1\",\"type\":\"customers\",\"name\":\"Bob\"}],\"included\":[]},{\"jsonapi\":{\"version\":\"1.0\"},\"data\":[{\"id\":\"2\",\"type\":\"customers\",\"name\":\"Mike\"}],\"included\":[]},{\"jsonapi\":{\"version\":\"1.0\"},\"data\":[{\"id\":\"3\",\"type\":\"customers\",\"name\":\"Pete\"}],\"included\":[]}]";

        final List<Map<String, Object>> allState = GeneralUtils.deserializeJson(json,
                new TypeReference<List<Map<String, Object>>>() {
                });

        final String jsonPath = "[2].data.[0].id=2";

        // Test
        final Optional<Map<String, Object>> result = statefulServiceImpl.findDataStateRecordByPath(allState, jsonPath);

        // Assertions
        Assertions.assertFalse(result.isPresent());

    }

    @Test
    void findDataStateRecordTest() {

        // Setup
        final String json = "[{\"jsonapi\":{\"version\":\"1.0\"},\"data\":{\"id\":\"1\",\"type\":\"customers\",\"name\":\"Bob\"},\"included\":[]},{\"jsonapi\":{\"version\":\"1.0\"},\"data\":{\"id\":\"2\",\"type\":\"customers\",\"name\":\"Mike\"},\"included\":[]},{\"jsonapi\":{\"version\":\"1.0\"},\"data\":{\"id\":\"3\",\"type\":\"customers\",\"name\":\"Pete\"},\"included\":[]}]";

        final List<Map<String, Object>> allState = GeneralUtils.deserializeJson(json,
                new TypeReference<List<Map<String, Object>>>() {
                });

        // Test
        final Optional<Map<String, Object>> record = statefulServiceImpl.findDataStateRecord(allState, "data.id", "2");

        // Assertions
        Assertions.assertTrue(record.isPresent());
        Assertions.assertNotNull(record.get());
        Assertions.assertTrue(record.get().get("data") instanceof Map);
        Assertions.assertEquals("2", ((Map) record.get().get("data")).get("id"));
        Assertions.assertEquals("Mike", ((Map) record.get().get("data")).get("name"));
    }

    @Test
    void findDataStateRecord_complexJson_Test() {

        // Setup
        final String json = "[{\"version\":1,\"system\":\"Foo1\",\"active\":true,\"data\":[{\"type\":\"customers\",\"meta\":null,\"keys\":[{\"name\":\"Sian\"}]}],\"included\":[]},{\"version\":1,\"system\":\"Foo2\",\"active\":true,\"data\":[{\"type\":\"customers\",\"meta\":[\"A\",\"B\",\"C\"],\"keys\":[{\"name\":\"Sam\"}]}],\"included\":[]},{\"version\":1,\"system\":\"Foo3\",\"active\":true,\"data\":[{\"type\":\"customers\",\"meta\":[\"A\",\"C\"],\"keys\":[{\"name\":\"Will\"}]}],\"included\":[]},{\"version\":1,\"system\":\"Foo4\",\"active\":true,\"data\":[{\"type\":\"customers\",\"meta\":null,\"keys\":[{\"name\":\"Billy\"}]}],\"included\":[]}]";

        final List<Map<String, Object>> allState = GeneralUtils.deserializeJson(json,
                new TypeReference<List<Map<String, Object>>>() {
                });

        // Test
        final Optional<Map<String, Object>> record = statefulServiceImpl.findDataStateRecord(allState, "data.keys.name", "Will");

        // Assertions
        Assertions.assertTrue(record.isPresent());
        Assertions.assertNotNull(record.get());
        Assertions.assertTrue(record.get().get("data") instanceof List);
        Assertions.assertEquals(1, ((List) record.get().get("data")).size());
        Assertions.assertTrue(((List) record.get().get("data")).get(0) instanceof Map);
        Assertions.assertTrue(((Map) ((List) record.get().get("data")).get(0)).get("keys") instanceof List);
        Assertions.assertEquals(1, ((List) ((Map) ((List) record.get().get("data")).get(0)).get("keys")).size());
        Assertions.assertTrue(((List) ((Map) ((List) record.get().get("data")).get(0)).get("keys")).get(0) instanceof Map);
        Assertions.assertEquals("Will", ((Map) ((List) ((Map) ((List) record.get().get("data")).get(0)).get("keys")).get(0)).get("name"));
    }

    @Test
    void appendIdToJson_noIdPresentInComplexJson_Test() {

        // Setup
        final String json = "{\"version\":1,\"system\":\"Foo2\",\"active\":true,\"data\":[{\"type\":\"customers\",\"meta\":[\"A\",\"B\",\"C\"],\"keys\":[{\"name\":\"Sam\"}]}],\"included\":[]}";

        final Map<String, Object> newState = GeneralUtils.deserializeJson(json,
                new TypeReference<Map<String, Object>>() {
                });
        final RestfulMockStatefulMeta restfulMockStatefulMeta = new RestfulMockStatefulMeta();
        restfulMockStatefulMeta.setIdFieldName("id");
        restfulMockStatefulMeta.setIdFieldLocation("data.keys.id");

        // Test
        statefulServiceImpl.appendIdToJson(newState, restfulMockStatefulMeta);

        // Assertions
        Assertions.assertNotNull(newState);
        Assertions.assertNotNull(newState.get("data"));
        Assertions.assertTrue(newState.get("data") instanceof List);
        Assertions.assertEquals(1, ((List) newState.get("data")).size());
        Assertions.assertNotNull(((List) newState.get("data")).get(0));
        Assertions.assertTrue(((List) newState.get("data")).get(0) instanceof Map);
        Assertions.assertTrue(((Map) ((List) newState.get("data")).get(0)).containsKey("keys"));
        Assertions.assertNotNull(((Map) ((List) newState.get("data")).get(0)).get("keys"));
        Assertions.assertTrue(((Map) ((List) newState.get("data")).get(0)).get("keys") instanceof List);
        Assertions.assertEquals(1, ((List) ((Map) ((List) newState.get("data")).get(0)).get("keys")).size());
        Assertions.assertTrue(((List) ((Map) ((List) newState.get("data")).get(0)).get("keys")).get(0) instanceof Map);
        Assertions.assertTrue(((Map) ((List) ((Map) ((List) newState.get("data")).get(0)).get("keys")).get(0)).containsKey("id"));
        Assertions.assertNotNull(((Map) ((List) ((Map) ((List) newState.get("data")).get(0)).get("keys")).get(0)).get("id"));
    }

    @Test
    void appendIdToJson_idAlreadyPresentInComplexJson_Test() {

        // Setup
        final String existingId = "12345";
        final String json = "{\"version\":1,\"system\":\"Foo2\",\"active\":true,\"data\":[{\"type\":\"customers\",\"meta\":[\"A\",\"B\",\"C\"],\"keys\":[{\"id\":\"" + existingId + "\",\"name\":\"Sam\"}]}],\"included\":[]}";

        final Map<String, Object> newState = GeneralUtils.deserializeJson(json,
                new TypeReference<Map<String, Object>>() {
                });
        final RestfulMockStatefulMeta restfulMockStatefulMeta = new RestfulMockStatefulMeta();
        restfulMockStatefulMeta.setIdFieldName("id");
        restfulMockStatefulMeta.setIdFieldLocation("data.keys.id");

        // Test
        statefulServiceImpl.appendIdToJson(newState, restfulMockStatefulMeta);

        // Assertions
        Assertions.assertNotNull(newState);
        Assertions.assertNotNull(newState.get("data"));
        Assertions.assertTrue(newState.get("data") instanceof List);
        Assertions.assertEquals(1, ((List) newState.get("data")).size());
        Assertions.assertNotNull(((List) newState.get("data")).get(0));
        Assertions.assertTrue(((List) newState.get("data")).get(0) instanceof Map);
        Assertions.assertTrue(((Map) ((List) newState.get("data")).get(0)).containsKey("keys"));
        Assertions.assertNotNull(((Map) ((List) newState.get("data")).get(0)).get("keys"));
        Assertions.assertTrue(((Map) ((List) newState.get("data")).get(0)).get("keys") instanceof List);
        Assertions.assertEquals(1, ((List) ((Map) ((List) newState.get("data")).get(0)).get("keys")).size());
        Assertions.assertTrue(((List) ((Map) ((List) newState.get("data")).get(0)).get("keys")).get(0) instanceof Map);
        Assertions.assertTrue(((Map) ((List) ((Map) ((List) newState.get("data")).get(0)).get("keys")).get(0)).containsKey("id"));
        Assertions.assertEquals(existingId, ((Map) ((List) ((Map) ((List) newState.get("data")).get(0)).get("keys")).get(0)).get("id"));
    }

    @Test
    void appendIdToJson_noIdPresentInSimpleFlatJson_Test() {

        // Setup
        final String json = "{\"version\":1,\"system\":\"Foo2\",\"active\":true,\"name\":\"Sam\"}";

        final Map<String, Object> newState = GeneralUtils.deserializeJson(json,
                new TypeReference<Map<String, Object>>() {
                });
        final RestfulMockStatefulMeta restfulMockStatefulMeta = new RestfulMockStatefulMeta();
        restfulMockStatefulMeta.setIdFieldName("id");

        // Test
        statefulServiceImpl.appendIdToJson(newState, restfulMockStatefulMeta);

        // Assertions
        Assertions.assertNotNull(newState);
        Assertions.assertTrue(newState.containsKey("id"));
        Assertions.assertNotNull(newState.get("id"));
    }

    @Test
    void appendIdToJson_idAlreadyPresentInSimpleFlatJson_Test() {

        // Setup
        final String existingId = "12345";
        final String json = "{\"version\":1,\"system\":\"Foo2\",\"active\":true,\"id\":\"" + existingId + "\",\"name\":\"Sam\"}";

        final Map<String, Object> newState = GeneralUtils.deserializeJson(json,
                new TypeReference<Map<String, Object>>() {
                });
        final RestfulMockStatefulMeta restfulMockStatefulMeta = new RestfulMockStatefulMeta();
        restfulMockStatefulMeta.setIdFieldName("id");

        // Test
        statefulServiceImpl.appendIdToJson(newState, restfulMockStatefulMeta);

        // Assertions
        Assertions.assertNotNull(newState);
        Assertions.assertTrue(newState.containsKey("id"));
        Assertions.assertNotNull(newState.get("id"));
        Assertions.assertEquals(existingId, newState.get("id"));
    }

}
