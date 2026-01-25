package com.smockin.admin.service;

import com.github.alexdlaird.ngrok.NgrokClient;
import com.github.alexdlaird.ngrok.process.NgrokProcess;
import com.github.alexdlaird.ngrok.protocol.CreateTunnel;
import com.github.alexdlaird.ngrok.protocol.Tunnel;
import com.smockin.admin.dto.TunnelRequestDTO;
import com.smockin.admin.dto.response.TunnelResponseDTO;
import com.smockin.admin.exception.AuthException;
import com.smockin.admin.exception.TunnelException;
import com.smockin.admin.exception.ValidationException;
import com.smockin.admin.persistence.entity.SmockinUser;
import com.smockin.admin.persistence.enums.ServerTypeEnum;
import com.smockin.admin.service.utils.UserTokenServiceUtils;
import com.smockin.mockserver.dto.MockServerState;
import com.smockin.mockserver.dto.MockedServerConfigDTO;
import com.smockin.utils.GeneralUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class TunnelServiceTest {

    @Mock
    private SmockinUserService smockinUserService;

    @Mock
    private UserTokenServiceUtils userTokenServiceUtils;

    @Mock
    private MockedServerEngineService mockedServerEngineService;

    @Mock
    private NgrokClient ngrokClient;

    @Mock
    private NgrokProcess ngrokProcess;

    @Mock
    private Tunnel tunnel;

    @Mock
    private SmockinUser smockinUser;

    @Mock
    private MockServerState serverState;

    private TunnelServiceImpl tunnelService;

    @BeforeEach
    void setUp() {

        tunnelService = Mockito.spy(new TunnelServiceImpl(
                smockinUserService,
                userTokenServiceUtils,
                mockedServerEngineService));
    }

    @Test
    void load_NotInstanced_Pass() {

        // Setup
        Mockito.doReturn(null)
                .when(tunnelService)
                .getNgrokClientInstance();

        // Test
        final TunnelResponseDTO responseDTO = tunnelService.load(UUID.randomUUID().toString());

        // Assertions
        Assertions.assertNotNull(responseDTO);
        Assertions.assertFalse(responseDTO.isEnabled());
        Assertions.assertNull(responseDTO.getUri());
    }

    @Test
    void load_Instanced_Pass() {

        // Setup
        final String uri = "https://123.smockin-test.com";

        Mockito.when(ngrokProcess.isRunning())
                .thenReturn(true);
        Mockito.when(ngrokClient.getNgrokProcess())
                .thenReturn(ngrokProcess);
        Mockito.when(tunnel.getPublicUrl())
                .thenReturn(uri);
        Mockito.when(ngrokClient.getTunnels())
                .thenReturn(Arrays.asList(tunnel));
        Mockito.doReturn(ngrokClient)
                .when(tunnelService)
                .getNgrokClientInstance();

        // Test
        final TunnelResponseDTO responseDTO = tunnelService.load(UUID.randomUUID().toString());

        // Assertions
        Assertions.assertNotNull(responseDTO);
        Assertions.assertTrue(responseDTO.isEnabled());
        Assertions.assertEquals(uri, responseDTO.getUri());
    }

    @Test
    void load_InstancedNotNotRunning_Pass() {

        // Setup
        Mockito.when(ngrokProcess.isRunning())
                .thenReturn(false);
        Mockito.when(ngrokClient.getNgrokProcess())
                .thenReturn(ngrokProcess);
        Mockito.doReturn(ngrokClient)
                .when(tunnelService)
                .getNgrokClientInstance();

        // Test
        final TunnelResponseDTO responseDTO = tunnelService.load(UUID.randomUUID().toString());

        // Assertions
        Assertions.assertNotNull(responseDTO);
        Assertions.assertFalse(responseDTO.isEnabled());
        Assertions.assertNull(responseDTO.getUri());
    }

    @Test
    void load_MissingTunnel_Fail() {

        // Setup
        Mockito.when(ngrokProcess.isRunning())
                .thenReturn(true);
        Mockito.when(ngrokClient.getNgrokProcess())
                .thenReturn(ngrokProcess);
        Mockito.when(ngrokClient.getTunnels())
                .thenReturn(Arrays.asList());
        Mockito.doReturn(ngrokClient)
                .when(tunnelService)
                .getNgrokClientInstance();

        // Test && Assertion
        final TunnelException tunnelException = Assertions.assertThrows(
                TunnelException.class,
                () -> tunnelService.load(UUID.randomUUID().toString()));
        Assertions.assertEquals("ngrok Tunnel is missing", tunnelException.getMessage());
    }

    @Test
    void load_MultipleTunnels_Fail() {

        // Setup
        Mockito.when(ngrokProcess.isRunning())
                .thenReturn(true);
        Mockito.when(ngrokClient.getNgrokProcess())
                .thenReturn(ngrokProcess);
        Mockito.when(ngrokClient.getTunnels())
                .thenReturn(Arrays.asList(tunnel, tunnel));
        Mockito.doReturn(ngrokClient)
                .when(tunnelService)
                .getNgrokClientInstance();

        // Test && Assertion
        final TunnelException tunnelException = Assertions.assertThrows(
                TunnelException.class,
                () -> tunnelService.load(UUID.randomUUID().toString()));
        Assertions.assertEquals("Multiple ngrok tunnels were found", tunnelException.getMessage());
    }

    @Test
    void update_newInstance_Pass() throws AuthException, ValidationException {

        // Setup
        final String uri = "https://123.smockin-test.com";

        Mockito.when(userTokenServiceUtils.loadCurrentActiveUser(Mockito.anyString()))
                .thenReturn(smockinUser);
        Mockito.doNothing()
                .when(smockinUserService).assertCurrentUserIsAdmin(Mockito.any(SmockinUser.class));
        Mockito.doReturn(null)
                .when(tunnelService)
                .getNgrokClientInstance();
        Mockito.doReturn(ngrokClient)
                .when(tunnelService)
                .instanceNgrokClient();
        Mockito.when(ngrokProcess.isRunning())
                .thenReturn(false)
                .thenReturn(true);
        Mockito.when(ngrokClient.getNgrokProcess())
                .thenReturn(ngrokProcess);
        Mockito.when(tunnel.getPublicUrl())
                .thenReturn(uri);
        Mockito.when(serverState.getPort())
                .thenReturn(8001);
        Mockito.when(mockedServerEngineService.getRestServerState())
                .thenReturn(serverState);
        Mockito.when(ngrokClient.connect(Mockito.any(CreateTunnel.class)))
                .thenReturn(tunnel);
        final MockedServerConfigDTO mockedServerConfig = new MockedServerConfigDTO();
        mockedServerConfig.getNativeProperties().put(GeneralUtils.NGROK_AUTH_TOKEN, "123456");
        Mockito.when(mockedServerEngineService
                        .loadServerConfig(Mockito.any(ServerTypeEnum.class)))
                .thenReturn(mockedServerConfig);


        // Test
        final TunnelResponseDTO responseDTO = tunnelService.update(new TunnelRequestDTO(true), UUID.randomUUID().toString());

        // Assertions
        Assertions.assertNotNull(responseDTO);
        Assertions.assertTrue(responseDTO.isEnabled());
        Assertions.assertEquals(uri, responseDTO.getUri());

        Mockito.verify(tunnelService, Mockito.times(1))
                .instanceNgrokClient();
        Mockito.verify(ngrokClient, Mockito.times(1))
                .connect(Mockito.any(CreateTunnel.class));
    }

    @Test
    void update_killTunnel_Pass() throws AuthException, ValidationException {

        // Setup
        final String uri = "https://123.smockin-test.com";

        Mockito.when(userTokenServiceUtils.loadCurrentActiveUser(Mockito.anyString()))
                .thenReturn(smockinUser);
        Mockito.doNothing()
                .when(smockinUserService).assertCurrentUserIsAdmin(Mockito.any(SmockinUser.class));
        Mockito.doReturn(ngrokClient)
                .when(tunnelService)
                .getNgrokClientInstance();
        Mockito.when(ngrokProcess.isRunning())
                .thenReturn(true)
                .thenReturn(false);
        Mockito.when(ngrokClient.getNgrokProcess())
                .thenReturn(ngrokProcess);
        Mockito.doNothing()
                .when(ngrokClient)
                .kill();

        // Test
        final TunnelResponseDTO responseDTO = tunnelService.update(new TunnelRequestDTO(false), UUID.randomUUID().toString());

        // Assertions
        Assertions.assertNotNull(responseDTO);
        Assertions.assertFalse(responseDTO.isEnabled());
        Assertions.assertNull(responseDTO.getUri());

        Mockito.verify(ngrokClient, Mockito.times(1))
                .kill();
    }

    @Test
    void update_alreadyRunning_Pass() throws AuthException, ValidationException {

        // Setup
        final String uri = "https://123.smockin-test.com";

        Mockito.when(userTokenServiceUtils.loadCurrentActiveUser(Mockito.anyString()))
                .thenReturn(smockinUser);
        Mockito.doNothing()
                .when(smockinUserService).assertCurrentUserIsAdmin(Mockito.any(SmockinUser.class));
        Mockito.doReturn(ngrokClient)
                .when(tunnelService)
                .getNgrokClientInstance();
        Mockito.when(ngrokProcess.isRunning())
                .thenReturn(true);
        Mockito.when(ngrokClient.getNgrokProcess())
                .thenReturn(ngrokProcess);
        Mockito.when(tunnel.getPublicUrl())
                .thenReturn(uri);
        Mockito.when(ngrokClient.getTunnels())
                .thenReturn(Arrays.asList(tunnel));

        // Test
        final TunnelResponseDTO responseDTO = tunnelService.update(new TunnelRequestDTO(true), UUID.randomUUID().toString());

        // Assertions
        Assertions.assertNotNull(responseDTO);
        Assertions.assertTrue(responseDTO.isEnabled());
        Assertions.assertEquals(uri, responseDTO.getUri());
    }

    @Test
    void update_newInstanceButNotEnabled_Pass() throws AuthException, ValidationException {

        // Setup
        Mockito.when(userTokenServiceUtils.loadCurrentActiveUser(Mockito.anyString()))
                .thenReturn(smockinUser);
        Mockito.doNothing()
                .when(smockinUserService).assertCurrentUserIsAdmin(Mockito.any(SmockinUser.class));
        Mockito.doReturn(null)
                .when(tunnelService)
                .getNgrokClientInstance();
        Mockito.doReturn(ngrokClient)
                .when(tunnelService)
                .instanceNgrokClient();
        Mockito.when(ngrokProcess.isRunning())
                .thenReturn(false);
        Mockito.when(ngrokClient.getNgrokProcess())
                .thenReturn(ngrokProcess);

        // Test
        final TunnelResponseDTO responseDTO = tunnelService.update(new TunnelRequestDTO(false), UUID.randomUUID().toString());

        // Assertions
        Assertions.assertNotNull(responseDTO);
        Assertions.assertFalse(responseDTO.isEnabled());
        Assertions.assertNull(responseDTO.getUri());

        Mockito.verify(tunnelService, Mockito.times(1))
                .instanceNgrokClient();
        Mockito.verify(ngrokClient, Mockito.never())
                .connect(Mockito.any(CreateTunnel.class));
        Mockito.verify(ngrokProcess, Mockito.times(2))
                .isRunning();
    }

}
