package com.smockin.admin.service;

import com.smockin.admin.exception.RecordNotFoundException;
import com.smockin.admin.exception.ValidationException;
import com.smockin.admin.persistence.dao.ServerConfigDAO;
import com.smockin.admin.persistence.entity.ServerConfig;
import com.smockin.admin.persistence.enums.ServerTypeEnum;
import com.smockin.admin.service.utils.UserTokenServiceUtils;
import com.smockin.mockserver.dto.MockedServerConfigDTO;
import com.smockin.mockserver.exception.MockServerException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import java.util.Arrays;

@ExtendWith(MockitoExtension.class)
class ServerConfigManagerTest {

    @Mock
    private ServerConfigDAO serverConfigDAO;

    @Mock
    private RestServerManager restServerManager;

    @Mock
    private S3ServerManager s3ServerManager;

    @Mock
    private MailServerManager mailServerManager;

    @Mock
    private SmockinUserService smockinUserService;

    @Mock
    private UserTokenServiceUtils userTokenServiceUtils;

    private ServerConfigManager serverConfigManager;

    @BeforeEach
    public void setUp() {
        final ObjectProvider<RestServerManager> restServerManagerProvider = Mockito.mock(ObjectProvider.class);
        final ObjectProvider<S3ServerManager> s3ServerManagerProvider = Mockito.mock(ObjectProvider.class);
        final ObjectProvider<MailServerManager> mailServerManagerProvider = Mockito.mock(ObjectProvider.class);

        Mockito.lenient().when(restServerManagerProvider.getObject()).thenReturn(restServerManager);
        Mockito.lenient().when(s3ServerManagerProvider.getObject()).thenReturn(s3ServerManager);
        Mockito.lenient().when(mailServerManagerProvider.getObject()).thenReturn(mailServerManager);

        serverConfigManager = new ServerConfigManager(serverConfigDAO, smockinUserService, userTokenServiceUtils, restServerManagerProvider, s3ServerManagerProvider, mailServerManagerProvider);
    }

    @Test
    void validateServerConfig_Null_Test() {
        final ValidationException ex = Assertions.assertThrows(ValidationException.class,
                () -> serverConfigManager.validateServerConfig(null));
        Assertions.assertEquals("config is required", ex.getMessage());
    }

    @Test
    void validateServerConfig_MissingPort_Test() {
        final ValidationException ex = Assertions.assertThrows(ValidationException.class,
                () -> serverConfigManager.validateServerConfig(new MockedServerConfigDTO()));
        Assertions.assertEquals("'port' config value is required", ex.getMessage());
    }

    @Test
    void validateServerConfig_MissingMaxThreads_Test() {
        final MockedServerConfigDTO dto = new MockedServerConfigDTO();
        dto.setPort(8001);
        final ValidationException ex = Assertions.assertThrows(ValidationException.class,
                () -> serverConfigManager.validateServerConfig(dto));
        Assertions.assertEquals("'maxThreads' config value is required", ex.getMessage());
    }

    @Test
    void validateServerConfig_MissingMinThreads_Test() {
        final MockedServerConfigDTO dto = new MockedServerConfigDTO();
        dto.setPort(8001);
        dto.setMaxThreads(10);
        final ValidationException ex = Assertions.assertThrows(ValidationException.class,
                () -> serverConfigManager.validateServerConfig(dto));
        Assertions.assertEquals("'minThreads' config value is required", ex.getMessage());
    }

    @Test
    void validateServerConfig_MissingTimeOutMillis_Test() {
        final MockedServerConfigDTO dto = new MockedServerConfigDTO();
        dto.setPort(8001);
        dto.setMaxThreads(10);
        dto.setMinThreads(1);
        final ValidationException ex = Assertions.assertThrows(ValidationException.class,
                () -> serverConfigManager.validateServerConfig(dto));
        Assertions.assertEquals("'timeOutMillis' config value is required", ex.getMessage());
    }

    @Test
    void validateServerConfigTest() throws ValidationException {
        final MockedServerConfigDTO dto = new MockedServerConfigDTO();
        dto.setPort(8001);
        dto.setMaxThreads(10);
        dto.setMinThreads(1);
        dto.setTimeOutMillis(30000);
        serverConfigManager.validateServerConfig(dto);
    }

    @Test
    void autoStartManagerTest() throws MockServerException {
        serverConfigManager.autoStartManager(ServerTypeEnum.RESTFUL);
        Mockito.verify(restServerManager, Mockito.times(1)).startRest();
    }

    @Test
    void autoStartManager_Null_Test() throws MockServerException {
        serverConfigManager.autoStartManager(null);
        Mockito.verify(restServerManager, Mockito.never()).startRest();
    }

    @Test
    void handleServerAutoStartTest() throws MockServerException {
        final ServerConfig sc1 = new ServerConfig(ServerTypeEnum.RESTFUL);
        sc1.setAutoStart(true);
        final ServerConfig sc2 = new ServerConfig(ServerTypeEnum.S3);
        sc2.setAutoStart(false);

        Mockito.when(serverConfigDAO.findAll()).thenReturn(Arrays.asList(sc1, sc2));

        serverConfigManager.handleServerAutoStart();

        Mockito.verify(restServerManager, Mockito.times(1)).startRest();
        Mockito.verify(s3ServerManager, Mockito.never()).startS3();
    }

}
