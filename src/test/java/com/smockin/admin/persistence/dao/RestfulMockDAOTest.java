package com.smockin.admin.persistence.dao;

import com.smockin.SmockinTestUtils;
import com.smockin.admin.persistence.entity.RestfulMock;
import com.smockin.admin.persistence.entity.SmockinUser;
import com.smockin.admin.persistence.enums.RecordStatusEnum;
import com.smockin.admin.persistence.enums.RestMethodEnum;
import com.smockin.admin.persistence.enums.RestMockTypeEnum;
import com.smockin.admin.persistence.enums.SmockinUserRoleEnum;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;

/**
 * Created by mgallina.
 */
@ExtendWith(SpringExtension.class)
@SpringBootConfiguration
@EnableAutoConfiguration
@EnableJpaRepositories("com.smockin.admin.persistence.dao")
@EntityScan("com.smockin.admin.persistence.entity")
class RestfulMockDAOTest {

    @Autowired
    private RestfulMockDAO restfulMockDAO;

    @Autowired
    private SmockinUserDAO smockinUserDAO;

    private RestfulMock a, b, c, d, e, f, g, h, i, j, k;
    private SmockinUser user;

    @BeforeEach
    void setUp() {

        user = smockinUserDAO.saveAndFlush(SmockinTestUtils.buildSmockinUser());

        a = SmockinTestUtils.buildRestfulMock("/a", RestMockTypeEnum.SEQ, 6, RestMethodEnum.GET, RecordStatusEnum.ACTIVE, user);
        b = SmockinTestUtils.buildRestfulMock("/b", RestMockTypeEnum.SEQ, 2, RestMethodEnum.GET, RecordStatusEnum.ACTIVE, user);
        c = SmockinTestUtils.buildRestfulMock("/c", RestMockTypeEnum.SEQ, 3, RestMethodEnum.GET, RecordStatusEnum.ACTIVE, user);
        d = SmockinTestUtils.buildRestfulMock("/d", RestMockTypeEnum.SEQ, 10, RestMethodEnum.GET, RecordStatusEnum.INACTIVE, user);
        e = SmockinTestUtils.buildRestfulMock("/e", RestMockTypeEnum.SEQ, 1, RestMethodEnum.GET, RecordStatusEnum.ACTIVE, user);
        f = SmockinTestUtils.buildRestfulMock("/f", RestMockTypeEnum.SEQ, 12, RestMethodEnum.GET, RecordStatusEnum.ACTIVE, user);
        g = SmockinTestUtils.buildRestfulMock("/g", RestMockTypeEnum.SEQ, 14, RestMethodEnum.GET, RecordStatusEnum.INACTIVE, user);
        h = SmockinTestUtils.buildRestfulMock("/h", RestMockTypeEnum.SEQ, 15, RestMethodEnum.GET, RecordStatusEnum.ACTIVE, user);

        a = restfulMockDAO.saveAndFlush(a);
        b = restfulMockDAO.saveAndFlush(b);
        c = restfulMockDAO.saveAndFlush(c);
        d = restfulMockDAO.saveAndFlush(d);
        e = restfulMockDAO.saveAndFlush(e);
        f = restfulMockDAO.saveAndFlush(f);
        g = restfulMockDAO.saveAndFlush(g);
        h = restfulMockDAO.saveAndFlush(h);

    }

    @AfterEach
    void tearDown() {

        restfulMockDAO.deleteAll();
        restfulMockDAO.flush();

        smockinUserDAO.deleteAll();
        smockinUserDAO.flush();
    }

    @Test
    void findAllByStatusTest() {

        final List<RestfulMock> mocks = restfulMockDAO.findAllByStatus(RecordStatusEnum.ACTIVE);

        Assertions.assertNotNull(mocks);
        Assertions.assertEquals(6, mocks.size());

        int[] expectedLoadOrder = new int[]{1, 2, 3, 6, 12, 15};

        int index = 0;

        for (RestfulMock m : mocks) {
            Assertions.assertEquals(expectedLoadOrder[index++], m.getInitializationOrder());
        }

    }

    @Test
    void findAllTest() {

        final List<RestfulMock> mocks = restfulMockDAO.findAll();

        Assertions.assertNotNull(mocks);
        Assertions.assertEquals(8, mocks.size());

        int[] expectedLoadOrder = new int[]{1, 2, 3, 6, 10, 12, 14, 15};

        int index = 0;

        for (RestfulMock m : mocks) {
            Assertions.assertEquals(expectedLoadOrder[index++], m.getInitializationOrder());
        }

    }

    @Test
    void uniquePathMethodAndUserConstraintTest() {

        a.setPath("/foo");
        a.setMethod(RestMethodEnum.GET);
        a.setCreatedBy(user);

        b.setPath("/foo");
        b.setMethod(RestMethodEnum.GET);
        b.setCreatedBy(user);

        Assertions.assertThrows(DataIntegrityViolationException.class, () -> {
            restfulMockDAO.saveAndFlush(a);
            restfulMockDAO.saveAndFlush(b);
        });
    }

    @Test
    void findAllPathDuplicatesTest() {

        SmockinUser userB = smockinUserDAO.saveAndFlush(buildPathDuplicatesUser("userB", SmockinUserRoleEnum.REGULAR));
        SmockinUser userC = smockinUserDAO.saveAndFlush(buildPathDuplicatesUser("userC", SmockinUserRoleEnum.REGULAR));
        SmockinUser userD = smockinUserDAO.saveAndFlush(buildPathDuplicatesUser("userD", SmockinUserRoleEnum.REGULAR));
        SmockinUser userE = smockinUserDAO.saveAndFlush(buildPathDuplicatesUser("userE", SmockinUserRoleEnum.REGULAR));
        SmockinUser userF = smockinUserDAO.saveAndFlush(buildPathDuplicatesUser("userF", SmockinUserRoleEnum.ADMIN));
        SmockinUser userG = smockinUserDAO.saveAndFlush(buildPathDuplicatesUser("userG", SmockinUserRoleEnum.REGULAR));
        SmockinUser userH = smockinUserDAO.saveAndFlush(buildPathDuplicatesUser("userH", SmockinUserRoleEnum.REGULAR));
        SmockinUser userI = smockinUserDAO.saveAndFlush(buildPathDuplicatesUser("userI", SmockinUserRoleEnum.REGULAR));
        SmockinUser userJ = smockinUserDAO.saveAndFlush(buildPathDuplicatesUser("userJ", SmockinUserRoleEnum.REGULAR));
        SmockinUser userK = smockinUserDAO.saveAndFlush(buildPathDuplicatesUser("userK", SmockinUserRoleEnum.REGULAR));

        a.setPath("/a");
        a.setCreatedBy(user);

        b.setPath("/b");
        b.setCreatedBy(userB);

        c.setPath("/a");
        c.setCreatedBy(userC);

        d.setPath("/b");
        d.setCreatedBy(userD);
        d.setStatus(RecordStatusEnum.ACTIVE);

        e.setPath("/c");
        e.setCreatedBy(userE);

        f.setPath("/c");
        f.setCreatedBy(userF);
        f.setStatus(RecordStatusEnum.INACTIVE);

        g.setPath("/a");
        g.setCreatedBy(userG);
        g.setStatus(RecordStatusEnum.INACTIVE);

        h.setPath("/a");
        h.setCreatedBy(userH);
        h.setMethod(RestMethodEnum.PUT);

        i = SmockinTestUtils.buildRestfulMock("/a", RestMockTypeEnum.SEQ, 1, RestMethodEnum.PATCH, RecordStatusEnum.ACTIVE, userI);
        j = SmockinTestUtils.buildRestfulMock("/a", RestMockTypeEnum.SEQ, 2, RestMethodEnum.PATCH, RecordStatusEnum.ACTIVE, userJ);
        k = SmockinTestUtils.buildRestfulMock("/a", RestMockTypeEnum.SEQ, 3, RestMethodEnum.PATCH, RecordStatusEnum.ACTIVE, userK);

        a = restfulMockDAO.saveAndFlush(a);
        b = restfulMockDAO.saveAndFlush(b);
        c = restfulMockDAO.saveAndFlush(c);
        d = restfulMockDAO.saveAndFlush(d);
        e = restfulMockDAO.saveAndFlush(e);
        f = restfulMockDAO.saveAndFlush(f);
        g = restfulMockDAO.saveAndFlush(g);
        h = restfulMockDAO.saveAndFlush(h);
        i = restfulMockDAO.saveAndFlush(i);
        j = restfulMockDAO.saveAndFlush(j);
        k = restfulMockDAO.saveAndFlush(k);

    }

    @Test
    void doesMockPathStartWithSegment_exactMatch_Test() {

        // Setup
        final RestfulMock bobMock = SmockinTestUtils.buildRestfulMock("/bob", RestMockTypeEnum.SEQ, 15, RestMethodEnum.GET, RecordStatusEnum.ACTIVE, user);
        restfulMockDAO.saveAndFlush(bobMock);

        // Test
        Assertions.assertTrue(restfulMockDAO.doesMockPathStartWithSegment("bob"));

    }

    @Test
    void doesMockPathStartWithSegment_prefixMatch_Test() {

        // Setup
        final RestfulMock bobMock = SmockinTestUtils.buildRestfulMock("/bob/house", RestMockTypeEnum.SEQ, 15, RestMethodEnum.GET, RecordStatusEnum.ACTIVE, user);
        restfulMockDAO.saveAndFlush(bobMock);

        // Test
        Assertions.assertTrue(restfulMockDAO.doesMockPathStartWithSegment("bob"));

    }

    @Test
    void doesMockPathStartWithSegment_NoMatch_Test() {

        // Setup
        final RestfulMock bobMock = SmockinTestUtils.buildRestfulMock("/bob/house", RestMockTypeEnum.SEQ, 15, RestMethodEnum.GET, RecordStatusEnum.ACTIVE, user);
        restfulMockDAO.saveAndFlush(bobMock);

        // Test
        Assertions.assertFalse(restfulMockDAO.doesMockPathStartWithSegment("mike"));

    }

    private SmockinUser buildPathDuplicatesUser(final String username, final SmockinUserRoleEnum role) {

        SmockinUser user = SmockinTestUtils.buildSmockinUser();
        user.setUsername(username);
        user.setRole(role);
        user.setCtxPath(user.getUsername());
        return smockinUserDAO.saveAndFlush(user);
    }

}
