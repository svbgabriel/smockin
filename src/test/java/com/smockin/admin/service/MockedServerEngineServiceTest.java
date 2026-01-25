package com.smockin.admin.service;

import com.smockin.admin.exception.AuthException;
import com.smockin.admin.exception.RecordNotFoundException;
import com.smockin.admin.exception.ValidationException;
import com.smockin.admin.persistence.dao.ProxyForwardMappingDAO;
import com.smockin.admin.persistence.dao.ProxyForwardUserConfigDAO;
import com.smockin.admin.persistence.dao.RestfulMockDAO;
import com.smockin.admin.persistence.dao.ServerConfigDAO;
import com.smockin.admin.persistence.entity.ServerConfig;
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

import java.util.Arrays;

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

    @Spy
    @InjectMocks
    private MockedServerEngineService mockedServerEngineService = new MockedServerEngineServiceImpl();

    @Spy
    @InjectMocks
    private MockedServerEngineServiceImpl mockedServerEngineServiceImpl = new MockedServerEngineServiceImpl();

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

        Assertions.assertThrows(RecordNotFoundException.class,
                () -> mockedServerEngineService.loadServerConfig(ServerTypeEnum.RESTFUL));

    }

    @Test
    void loadServerConfig_Found_Test() throws RecordNotFoundException {

        // Setup
        final ServerConfig serverConfig = new ServerConfig(ServerTypeEnum.RESTFUL);
        serverConfig.setPort(8001);
        serverConfig.setMaxThreads(10);
        serverConfig.setMinThreads(5);
        serverConfig.setTimeOutMillis(30000);
        serverConfig.setAutoStart(true);
        serverConfig.getNativeProperties().put("serverName", "foo");

        Mockito.when(serverConfigDAO.findByServerType(Mockito.any(ServerTypeEnum.class))).thenReturn(serverConfig);

        // Test
        final MockedServerConfigDTO dto = mockedServerEngineService.loadServerConfig(ServerTypeEnum.RESTFUL);

        // Assertions
        Assertions.assertNotNull(dto);

        Assertions.assertEquals(serverConfig.getPort(), dto.getPort());
        Assertions.assertEquals(serverConfig.getMaxThreads(), dto.getMaxThreads());
        Assertions.assertEquals(serverConfig.getMinThreads(), dto.getMinThreads());
        Assertions.assertEquals(serverConfig.getTimeOutMillis(), dto.getTimeOutMillis());
        Assertions.assertEquals(serverConfig.isAutoStart(), dto.isAutoStart());

        Assertions.assertEquals(1, dto.getNativeProperties().size());
        Assertions.assertEquals("foo", dto.getNativeProperties().get("serverName"));

    }

    @Test
    void saveServerConfig_NotFoundCreateNew_Test() throws ValidationException, AuthException, RecordNotFoundException {

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
        final ArgumentCaptor<ServerConfig> argument = ArgumentCaptor.forClass(ServerConfig.class);
        Mockito.verify(serverConfigDAO).saveAndFlush(argument.capture());

        Assertions.assertEquals(dto.getPort(), argument.getValue().getPort());
        Assertions.assertEquals(dto.getMaxThreads(), argument.getValue().getMaxThreads());
        Assertions.assertEquals(dto.getMinThreads(), argument.getValue().getMinThreads());
        Assertions.assertEquals(dto.getTimeOutMillis(), argument.getValue().getTimeOutMillis());
        Assertions.assertEquals(dto.isAutoStart(), argument.getValue().isAutoStart());
        Assertions.assertEquals(dto.isAutoStart(), argument.getValue().isAutoStart());

        Assertions.assertNotNull(argument.getValue().getNativeProperties());
        Assertions.assertEquals(1, argument.getValue().getNativeProperties().size());
        Assertions.assertEquals(dto.getNativeProperties().get("serverName"), argument.getValue().getNativeProperties().get("serverName"));

    }

    @Test
    void saveServerConfig_UpdateExisting_Test() throws ValidationException, AuthException, RecordNotFoundException {

        // Setup
        final MockedServerConfigDTO dto = new MockedServerConfigDTO();
        dto.setPort(8001);
        dto.setMaxThreads(10);
        dto.setMinThreads(1);
        dto.setTimeOutMillis(30000);
        dto.setAutoStart(true);
        dto.getNativeProperties().put("serverName", "foo");

        final ServerConfig serverConfig = new ServerConfig();
        Mockito.when(serverConfigDAO.findByServerType(Mockito.any(ServerTypeEnum.class))).thenReturn(serverConfig);

        // Test
        mockedServerEngineService.saveServerConfig(ServerTypeEnum.RESTFUL, dto, token);

        // Assertions
        Assertions.assertEquals(dto.getPort(), serverConfig.getPort());
        Assertions.assertEquals(dto.getMaxThreads(), serverConfig.getMaxThreads());
        Assertions.assertEquals(dto.getMinThreads(), serverConfig.getMinThreads());
        Assertions.assertEquals(dto.getTimeOutMillis(), serverConfig.getTimeOutMillis());
        Assertions.assertEquals(dto.isAutoStart(), serverConfig.isAutoStart());
        Assertions.assertEquals(dto.isAutoStart(), serverConfig.isAutoStart());

        Assertions.assertNotNull(serverConfig.getNativeProperties());
        Assertions.assertEquals(1, serverConfig.getNativeProperties().size());
        Assertions.assertEquals(dto.getNativeProperties().get("serverName"), serverConfig.getNativeProperties().get("serverName"));

    }

    @Test
    void saveServerConfig_ValidationFailure_Test() throws ValidationException, AuthException, RecordNotFoundException {

        // Test & Assertions
        final ValidationException ex = Assertions.assertThrows(ValidationException.class,
                () -> mockedServerEngineService.saveServerConfig(ServerTypeEnum.RESTFUL, null, token));
        Assertions.assertEquals("config is required", ex.getMessage());

    }

    @Test
    void validateServerConfig_Null_Test() throws ValidationException {

        // Test & Assertions
        final ValidationException ex = Assertions.assertThrows(ValidationException.class,
                () -> mockedServerEngineServiceImpl.validateServerConfig(null));
        Assertions.assertEquals("config is required", ex.getMessage());

    }

    @Test
    void validateServerConfig_MissingPort_Test() throws ValidationException {

        // Test & Assertions
        final ValidationException ex = Assertions.assertThrows(ValidationException.class,
                () -> mockedServerEngineServiceImpl.validateServerConfig(new MockedServerConfigDTO()));
        Assertions.assertEquals("'port' config value is required", ex.getMessage());

    }

    @Test
    void validateServerConfig_MissingMaxThreads_Test() throws ValidationException {

        // Setup
        final MockedServerConfigDTO dto = new MockedServerConfigDTO();
        dto.setPort(8001);

        // Test & Assertions
        final ValidationException ex = Assertions.assertThrows(ValidationException.class,
                () -> mockedServerEngineServiceImpl.validateServerConfig(dto));
        Assertions.assertEquals("'maxThreads' config value is required", ex.getMessage());

    }

    @Test
    void validateServerConfig_MissingMinThreads_Test() throws ValidationException {

        // Setup
        final MockedServerConfigDTO dto = new MockedServerConfigDTO();
        dto.setPort(8001);
        dto.setMaxThreads(10);

        // Test & Assertions
        final ValidationException ex = Assertions.assertThrows(ValidationException.class,
                () -> mockedServerEngineServiceImpl.validateServerConfig(dto));
        Assertions.assertEquals("'minThreads' config value is required", ex.getMessage());

    }

    @Test
    void validateServerConfig_MissingTimeOutMillis_Test() throws ValidationException {

        // Setup
        final MockedServerConfigDTO dto = new MockedServerConfigDTO();
        dto.setPort(8001);
        dto.setMaxThreads(10);
        dto.setMinThreads(1);

        // Test & Assertions
        final ValidationException ex = Assertions.assertThrows(ValidationException.class,
                () -> mockedServerEngineServiceImpl.validateServerConfig(dto));
        Assertions.assertEquals("'timeOutMillis' config value is required", ex.getMessage());

    }

    @Test
    void validateServerConfigTest() throws ValidationException {

        // Setup
        final MockedServerConfigDTO dto = new MockedServerConfigDTO();
        dto.setPort(8001);
        dto.setMaxThreads(10);
        dto.setMinThreads(1);
        dto.setTimeOutMillis(30000);

        // Test
        mockedServerEngineServiceImpl.validateServerConfig(dto);

    }

    @Test
    void startRestTest() throws MockServerException, AuthException, RecordNotFoundException {

        // Setup
        final ServerConfig serverConfig = new ServerConfig(ServerTypeEnum.RESTFUL);
        serverConfig.setPort(8001);
        serverConfig.setMaxThreads(10);
        serverConfig.setMinThreads(5);
        serverConfig.setTimeOutMillis(30000);
        serverConfig.setAutoStart(true);
        serverConfig.getNativeProperties().put("serverName", "foo");

        Mockito.when(serverConfigDAO.findByServerType(Mockito.any(ServerTypeEnum.class))).thenReturn(serverConfig);
        Mockito.when(proxyForwardUserConfigDAO.findAll()).thenReturn(Arrays.asList());

        // Test
        final MockedServerConfigDTO dto = mockedServerEngineService.startRest(token);

        // Assertions
        Assertions.assertNotNull(dto);

        Assertions.assertEquals(serverConfig.getPort(), dto.getPort());
        Assertions.assertEquals(serverConfig.getMaxThreads(), dto.getMaxThreads());
        Assertions.assertEquals(serverConfig.getMinThreads(), dto.getMinThreads());
        Assertions.assertEquals(serverConfig.getTimeOutMillis(), dto.getTimeOutMillis());
        Assertions.assertEquals(serverConfig.isAutoStart(), dto.isAutoStart());
        Assertions.assertEquals(serverConfig.getNativeProperties().size(), dto.getNativeProperties().size());
        Assertions.assertEquals(serverConfig.getNativeProperties().get("serverName"), dto.getNativeProperties().get("serverName"));

    }

    @Test
    void startRest_ConfigNotFound_Test() throws MockServerException, AuthException, RecordNotFoundException {

        // Test & Assertions
        final MockServerException ex = Assertions.assertThrows(MockServerException.class,
                () -> mockedServerEngineService.startRest(token));
        Assertions.assertEquals("Missing mock REST server config", ex.getMessage());

    }

    @Test
    void startRest_GeneralFailure_Test() throws MockServerException, AuthException, RecordNotFoundException {

        // Setup
        final ServerConfig serverConfig = Mockito.mock(ServerConfig.class);
        Mockito.when(serverConfigDAO.findByServerType(Mockito.any(ServerTypeEnum.class))).thenReturn(serverConfig);
        Mockito.doThrow(new MockServerException("Startup Boom")).when(mockedRestServerEngine).start(Mockito.any(MockedServerConfigDTO.class), Mockito.anyList());
        Mockito.when(proxyForwardUserConfigDAO.findAll()).thenReturn(Arrays.asList());

        // Test & Assertions
        final MockServerException ex = Assertions.assertThrows(MockServerException.class,
                () -> mockedServerEngineService.startRest(token));
        Assertions.assertEquals("Startup Boom", ex.getMessage());

    }

    @Test
    void shutdownRestTest() throws MockServerException, AuthException, RecordNotFoundException {

        mockedServerEngineService.shutdownRest(token);

    }

    @Test
    void shutdownRest_GeneralFailure_Test() throws MockServerException, AuthException, RecordNotFoundException {

        // Setup
        Mockito.doThrow(new MockServerException("Shutdown Boom")).when(mockedRestServerEngine).shutdown();

        // Test & Assertions
        final MockServerException ex = Assertions.assertThrows(MockServerException.class,
                () -> mockedServerEngineService.shutdownRest(token));
        Assertions.assertEquals("Shutdown Boom", ex.getMessage());

    }

    @Test
    void autoStartManagerTest() throws MockServerException {

        // Setup
        final ServerConfig serverConfig = Mockito.mock(ServerConfig.class);
        Mockito.when(serverConfigDAO.findByServerType(Mockito.any(ServerTypeEnum.class))).thenReturn(serverConfig);
        Mockito.when(proxyForwardUserConfigDAO.findAll()).thenReturn(Arrays.asList());

        // Test
        mockedServerEngineServiceImpl.autoStartManager(ServerTypeEnum.RESTFUL);

        // Assertions
        Mockito.verify(mockedRestServerEngine, Mockito.times(1)).start(Mockito.any(MockedServerConfigDTO.class), Mockito.anyList());

    }

    @Test
    void autoStartManager_Null_Test() throws MockServerException {

        // Test
        mockedServerEngineServiceImpl.autoStartManager(null);

        // Assertions
        Mockito.verify(mockedRestServerEngine, Mockito.never()).start(Mockito.any(MockedServerConfigDTO.class), Mockito.anyList());
    }

    @Test
    void getRestServerStateTest() throws MockServerException {

        // Setup
        Mockito.when(mockedRestServerEngine.getCurrentState()).thenReturn(new MockServerState(true, 8001));

        // Test
        final MockServerState mockServerState = mockedServerEngineService.getRestServerState();

        // Assertions
        Assertions.assertNotNull(mockServerState);
        Assertions.assertTrue(mockServerState.isRunning());
        Assertions.assertEquals(8001, mockServerState.getPort());

    }

}
