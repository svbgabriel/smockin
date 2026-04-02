package com.smockin.mockserver.engine;

import com.smockin.admin.persistence.dao.RestfulMockDAO;
import com.smockin.admin.persistence.dao.SmockinUserDAO;
import com.smockin.admin.persistence.entity.RestfulMock;
import com.smockin.admin.persistence.entity.RestfulMockDefinitionOrder;
import com.smockin.admin.persistence.enums.RestMockTypeEnum;
import com.smockin.admin.service.HttpClientService;
import com.smockin.admin.service.SmockinUserService;
import com.smockin.admin.service.utils.MultiUserUtils;
import com.smockin.mockserver.service.*;
import com.smockin.mockserver.service.dto.RestfulResponseDTO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;

import java.util.NoSuchElementException;

/**
 * Created by mgallina.
 */
@ExtendWith(MockitoExtension.class)
class MockedRestServerEngineUtilsTest {

    @Mock
    private RestfulMockDAO restfulMockDAO;

    @Mock
    private RuleEngine ruleEngine;

    @Mock
    private HttpProxyService proxyService;

    @Mock
    private MockOrderingCounterService mockOrderingCounterService;

    @Mock
    private MultiUserUtils multiUserUtils;

    @Mock
    private JavaScriptResponseHandler javaScriptResponseHandler;

    @Mock
    private InboundParamMatchService inboundParamMatchService;

    @Mock
    private ServerSideEventService serverSideEventService;

    @Mock
    private StatefulService statefulService;

    @Mock
    private SmockinUserDAO smockinUserDAO;

    @Mock
    private ProxyMappingCache proxyMappingCache;

    @Mock
    private HttpClientService httpClientService;

    private MockedRestServerEngineUtils engineUtils;

    private RestfulMock restfulMock;
    private RestfulMockDefinitionOrder order1, order2, order3;

    @BeforeEach
    void setUp() {

        final ObjectProvider<HttpClientService> httpClientServiceProvider = Mockito.mock(ObjectProvider.class);
        Mockito.lenient().when(httpClientServiceProvider.getObject()).thenReturn(httpClientService);

        final MockedRestServerEngineUtils realEngineUtils = new MockedRestServerEngineUtils(restfulMockDAO, mockOrderingCounterService, ruleEngine, proxyService, javaScriptResponseHandler, inboundParamMatchService, serverSideEventService, statefulService, httpClientServiceProvider, smockinUserDAO, proxyMappingCache, multiUserUtils);
        engineUtils = Mockito.spy(realEngineUtils);

        restfulMock = new RestfulMock();
        restfulMock.getDefinitions().add(order1 = new RestfulMockDefinitionOrder(restfulMock, 200, "text/html", "HelloWorld 1", 1, 0, false, 0, 0));
        restfulMock.getDefinitions().add(order2 = new RestfulMockDefinitionOrder(restfulMock, 201, "text/html", "HelloWorld 2", 2, 0, false, 0, 0));
        restfulMock.getDefinitions().add(order3 = new RestfulMockDefinitionOrder(restfulMock, 204, "text/html", "HelloWorld 3", 3, 0, false, 0, 0));
    }

    @Test
    void getDefault_Null_Test() {

        // Test & Assertions
        Assertions.assertThrows(NullPointerException.class, () -> engineUtils.getDefault(null));
    }

    @Test
    void getDefault_NoDefinitionsDefined_Test() {

        // Setup
        restfulMock.getDefinitions().clear();

        // Test & Assertions
        Assertions.assertThrows(NoSuchElementException.class, () -> engineUtils.getDefault(restfulMock));
    }

    @Test
    void getDefaultTest() {

        // Test (run 1)
        // Should always be response with 'order No 1'
        final RestfulResponseDTO result1 = engineUtils.getDefault(restfulMock);

        // Assertions
        Assertions.assertNotNull(result1);
        Assertions.assertEquals(order1.getHttpStatusCode(), result1.getHttpStatusCode());
        Assertions.assertEquals(order1.getResponseContentType(), result1.getResponseContentType());
        Assertions.assertEquals(order1.getResponseBody(), result1.getResponseBody());

        // Test (run 2)
        // ... and just to double check...
        final RestfulResponseDTO result2 = engineUtils.getDefault(restfulMock);

        // Assertions
        Assertions.assertNotNull(result2);
        Assertions.assertEquals(order1.getHttpStatusCode(), result2.getHttpStatusCode());
        Assertions.assertEquals(order1.getResponseContentType(), result2.getResponseContentType());
        Assertions.assertEquals(order1.getResponseBody(), result2.getResponseBody());

    }

    @Test
    void getDefault_Proxy_Test() {

        // Setup
        restfulMock.setMockType(RestMockTypeEnum.PROXY_HTTP);

        // Test
        final RestfulResponseDTO result = engineUtils.getDefault(restfulMock);

        // Assertions
        Assertions.assertNotNull(result);
        Assertions.assertEquals(HttpStatus.NOT_FOUND.value(), result.getHttpStatusCode());
        Assertions.assertNull(result.getResponseContentType());
        Assertions.assertNull(result.getResponseBody());
        Assertions.assertTrue(result.getHeaders().isEmpty());
    }

}
