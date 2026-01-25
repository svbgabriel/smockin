package com.smockin.admin.persistence.dao;

import com.smockin.SmockinTestUtils;
import com.smockin.admin.persistence.entity.RestfulMock;
import com.smockin.admin.persistence.enums.RecordStatusEnum;
import com.smockin.admin.persistence.enums.RestMethodEnum;
import com.smockin.admin.persistence.enums.RestMockTypeEnum;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

/**
 * Created by mgallina.
 */
class RestfulMockDAOPathMatchTest {

    private RestfulMock a, b, c, d, e;
    private RestfulMockDAOImpl restfulMockDAOImpl;

    @BeforeEach
    void setUp() {

        a = SmockinTestUtils.buildRestfulMock("/js", RestMockTypeEnum.CUSTOM_JS, 1, RestMethodEnum.GET, RecordStatusEnum.ACTIVE, null);
        b = SmockinTestUtils.buildRestfulMock("/js2/{id}", RestMockTypeEnum.CUSTOM_JS, 2, RestMethodEnum.GET, RecordStatusEnum.ACTIVE, null);
        c = SmockinTestUtils.buildRestfulMock("/js3/1", RestMockTypeEnum.CUSTOM_JS, 3, RestMethodEnum.GET, RecordStatusEnum.ACTIVE, null);
        d = SmockinTestUtils.buildRestfulMock("/firstname/{name}/lastname", RestMockTypeEnum.SEQ, 4, RestMethodEnum.GET, RecordStatusEnum.INACTIVE, null);
        e = SmockinTestUtils.buildRestfulMock("/hello/{name}/howareyou/{date}", RestMockTypeEnum.SEQ, 5, RestMethodEnum.GET, RecordStatusEnum.ACTIVE, null);

        restfulMockDAOImpl = new RestfulMockDAOImpl();

    }

    @Test
    void matchPath_simpleMatch_Test() {

        final RestfulMock loadedMock = restfulMockDAOImpl.matchPath(Arrays.asList(a, b, c, d, e), "/js", false);
        Assertions.assertNotNull(loadedMock);
        Assertions.assertEquals(a.getPath(), loadedMock.getPath());
    }

    @Test
    void matchPath_pathVar_Test() {

        final RestfulMock loadedMock = restfulMockDAOImpl.matchPath(Arrays.asList(a, b, c, d, e), "/js2/1", false);
        Assertions.assertNotNull(loadedMock);
        Assertions.assertEquals(b.getPath(), loadedMock.getPath());
    }

    @Test
    void matchPath_pathVar2_Test() {

        final RestfulMock loadedMock = restfulMockDAOImpl.matchPath(Arrays.asList(a, b, c, d, e), "/firstname/bob/lastname", false);
        Assertions.assertNotNull(loadedMock);
        Assertions.assertEquals(d.getPath(), loadedMock.getPath());
    }

    @Test
    void matchPath_pathVar3_Test() {

        final RestfulMock loadedMock = restfulMockDAOImpl.matchPath(Arrays.asList(a, b, c, d, e), "/hello/mike/howareyou/today", false);
        Assertions.assertNotNull(loadedMock);
        Assertions.assertEquals(e.getPath(), loadedMock.getPath());
    }

}
