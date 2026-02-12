package com.smockin.admin.service;

import com.smockin.admin.exception.AuthException;
import com.smockin.admin.exception.RecordNotFoundException;
import com.smockin.admin.exception.ValidationException;
import com.smockin.admin.persistence.dao.ProxyForwardMappingDAO;
import com.smockin.admin.persistence.dao.ProxyForwardUserConfigDAO;
import com.smockin.admin.persistence.dao.RestfulMockDAO;
import com.smockin.admin.persistence.dao.ServerConfigDAO;
import com.smockin.admin.persistence.entity.SmockinUser;
import com.smockin.admin.persistence.enums.ServerTypeEnum;
import com.smockin.admin.persistence.enums.SmockinUserRoleEnum;
import com.smockin.admin.service.utils.UserTokenServiceUtils;
import com.smockin.mockserver.dto.MockServerState;
import com.smockin.mockserver.dto.MockedServerConfigDTO;
import com.smockin.mockserver.engine.MockedRestServerEngine;
import com.smockin.mockserver.exception.MockServerException;
import com.smockin.utils.GeneralUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Created by mgallina on 21/07/17.
 */
@ExtendWith(MockitoExtension.class)
class MockedServerEngineServiceTest {

    @Mock
    private MockedRestServerEngine mockedRestServerEngine;

    @Mock
    private RestfulMockDAO restfulMockDefinitionDAO;

    @Mock
    private ServerConfigDAO serverConfigDAO;

    @Mock
    private ProxyForwardUserConfigDAO proxyForwardUserConfigDAO;

    @Mock
    private SmockinUserService smockinUserService;

    @Mock
    private UserTokenServiceUtils userTokenServiceUtils;

    @Mock
    private ProxyForwardMappingDAO proxyForwardMappingDAO;

    @Mock
    private RestServerManager restServerManager;

    @Mock
    private S3ServerManager s3ServerManager;

    @Mock
    private MailServerManager mailServerManager;

    @Mock
    private ServerConfigManager serverConfigManager;

    @Mock
    private ProxyMappingManager proxyMappingManager;

    @InjectMocks
    private MockedServerEngineServiceImpl mockedServerEngineService = new MockedServerEngineServiceImpl();

    private String token;
    private SmockinUser smockinUser;

    @BeforeEach
    void setUp() throws AuthException, RecordNotFoundException {

        token = GeneralUtils.generateUUID();
        smockinUser = new SmockinUser();
        smockinUser.setRole(SmockinUserRoleEnum.ADMIN);

    }

    @Test
    void loadServerConfig_NotFound_Test() throws RecordNotFoundException {

        Mockito.when(serverConfigManager.loadServerConfig(Mockito.any(ServerTypeEnum.class))).thenThrow(new RecordNotFoundException());

        Assertions.assertThrows(RecordNotFoundException.class,
                () -> mockedServerEngineService.loadServerConfig(ServerTypeEnum.RESTFUL));

    }

    @Test
    void loadServerConfig_Found_Test() throws RecordNotFoundException {

        // Setup
        final MockedServerConfigDTO configDTO = new MockedServerConfigDTO();
        configDTO.setPort(8001);
        configDTO.setMaxThreads(10);
        configDTO.setMinThreads(5);
        configDTO.setTimeOutMillis(30000);
        configDTO.setAutoStart(true);
        configDTO.getNativeProperties().put("serverName", "foo");

        Mockito.when(serverConfigManager.loadServerConfig(Mockito.any(ServerTypeEnum.class))).thenReturn(configDTO);

        // Test
        final MockedServerConfigDTO dto = mockedServerEngineService.loadServerConfig(ServerTypeEnum.RESTFUL);

        // Assertions
        Assertions.assertNotNull(dto);

        Assertions.assertEquals(configDTO.getPort(), dto.getPort());
        Assertions.assertEquals(configDTO.getMaxThreads(), dto.getMaxThreads());
        Assertions.assertEquals(configDTO.getMinThreads(), dto.getMinThreads());
        Assertions.assertEquals(configDTO.getTimeOutMillis(), dto.getTimeOutMillis());
        Assertions.assertEquals(configDTO.isAutoStart(), dto.isAutoStart());

        Assertions.assertEquals(1, dto.getNativeProperties().size());
        Assertions.assertEquals("foo", dto.getNativeProperties().get("serverName"));

    }

    @Test
    void saveServerConfig_Test() throws ValidationException, AuthException, RecordNotFoundException {

        // Setup
        final MockedServerConfigDTO dto = new MockedServerConfigDTO();
        dto.setPort(8001);
        dto.setMaxThreads(10);
        dto.setMinThreads(1);
        dto.setTimeOutMillis(30000);
        dto.setAutoStart(true);
        dto.getNativeProperties().put("serverName", "foo");

        // Test
        mockedServerEngineService.saveServerConfig(ServerTypeEnum.RESTFUL, dto, token);

        // Assertions
        Mockito.verify(serverConfigManager).saveServerConfig(ServerTypeEnum.RESTFUL, dto, token);

    }

    @Test
    void saveServerConfig_ValidationFailure_Test() throws ValidationException, AuthException, RecordNotFoundException {

        Mockito.doThrow(new ValidationException("config is required"))
                .when(serverConfigManager).saveServerConfig(Mockito.eq(ServerTypeEnum.RESTFUL), Mockito.isNull(), Mockito.eq(token));

        // Test & Assertions
        final ValidationException ex = Assertions.assertThrows(ValidationException.class,
                () -> mockedServerEngineService.saveServerConfig(ServerTypeEnum.RESTFUL, null, token));
        Assertions.assertEquals("config is required", ex.getMessage());

    }

    @Test
    void handleServerAutoStartTest() {

        // Test
        mockedServerEngineService.handleServerAutoStart();

        // Assertions
        Mockito.verify(serverConfigManager).handleServerAutoStart();

    }

    @Test
    void updateProxyModeTest() throws AuthException {

        // Test
        mockedServerEngineService.updateProxyMode(true, token);

        // Assertions
        Mockito.verify(proxyMappingManager).updateProxyMode(true, token);

    }

    @Test
    void startRestTest() throws MockServerException, AuthException, RecordNotFoundException {

        // Setup
        final MockedServerConfigDTO configDTO = new MockedServerConfigDTO();
        configDTO.setPort(8001);
        configDTO.setMaxThreads(10);
        configDTO.setMinThreads(5);
        configDTO.setTimeOutMillis(30000);
        configDTO.setAutoStart(true);
        configDTO.getNativeProperties().put("serverName", "foo");

        Mockito.when(restServerManager.startRest(token)).thenReturn(configDTO);

        // Test
        final MockedServerConfigDTO dto = mockedServerEngineService.startRest(token);

        // Assertions
        Assertions.assertNotNull(dto);

        Assertions.assertEquals(configDTO.getPort(), dto.getPort());
        Assertions.assertEquals(configDTO.getMaxThreads(), dto.getMaxThreads());
        Assertions.assertEquals(configDTO.getMinThreads(), dto.getMinThreads());
        Assertions.assertEquals(configDTO.getTimeOutMillis(), dto.getTimeOutMillis());
        Assertions.assertEquals(configDTO.isAutoStart(), dto.isAutoStart());
        Assertions.assertEquals(configDTO.getNativeProperties().size(), dto.getNativeProperties().size());
        Assertions.assertEquals(configDTO.getNativeProperties().get("serverName"), dto.getNativeProperties().get("serverName"));

    }

    @Test
    void startRest_ConfigNotFound_Test() throws MockServerException, AuthException, RecordNotFoundException {

        Mockito.when(restServerManager.startRest(token)).thenThrow(new MockServerException("Missing mock REST server config"));

        // Test & Assertions
        final MockServerException ex = Assertions.assertThrows(MockServerException.class,
                () -> mockedServerEngineService.startRest(token));
        Assertions.assertEquals("Missing mock REST server config", ex.getMessage());

    }

    @Test
    void startRest_GeneralFailure_Test() throws MockServerException, AuthException, RecordNotFoundException {

        Mockito.when(restServerManager.startRest(token)).thenThrow(new MockServerException("Startup Boom"));

        // Test & Assertions
        final MockServerException ex = Assertions.assertThrows(MockServerException.class,
                () -> mockedServerEngineService.startRest(token));
        Assertions.assertEquals("Startup Boom", ex.getMessage());

    }

    @Test
    void shutdownRestTest() throws MockServerException, AuthException, RecordNotFoundException {

        mockedServerEngineService.shutdownRest(token);
        Mockito.verify(restServerManager).shutdownRest(token);

    }

    @Test
    void shutdownRest_GeneralFailure_Test() throws MockServerException, AuthException, RecordNotFoundException {

        // Setup
        Mockito.doThrow(new MockServerException("Shutdown Boom")).when(restServerManager).shutdownRest(token);

        // Test & Assertions
        final MockServerException ex = Assertions.assertThrows(MockServerException.class,
                () -> mockedServerEngineService.shutdownRest(token));
        Assertions.assertEquals("Shutdown Boom", ex.getMessage());

    }

    @Test
    void getRestServerStateTest() throws MockServerException {

        // Setup
        Mockito.when(restServerManager.getRestServerState()).thenReturn(new MockServerState(true, 8001));

        // Test
        final MockServerState mockServerState = mockedServerEngineService.getRestServerState();

        // Assertions
        Assertions.assertNotNull(mockServerState);
        Assertions.assertTrue(mockServerState.isRunning());
        Assertions.assertEquals(8001, mockServerState.getPort());

    }

}
