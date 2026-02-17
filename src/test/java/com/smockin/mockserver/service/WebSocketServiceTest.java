package com.smockin.mockserver.service;

import com.smockin.admin.persistence.dao.RestfulMockDAO;
import com.smockin.admin.persistence.entity.RestfulMock;
import com.smockin.admin.persistence.entity.SmockinUser;
import com.smockin.admin.persistence.enums.RestMethodEnum;
import com.smockin.admin.persistence.enums.RestMockTypeEnum;
import com.smockin.admin.persistence.enums.SmockinUserRoleEnum;
import com.smockin.admin.service.utils.UserTokenServiceUtils;
import com.smockin.utils.GeneralUtils;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.UpgradeRequest;
import org.eclipse.jetty.websocket.api.UpgradeResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;
import java.time.Duration;

@ExtendWith(MockitoExtension.class)
class WebSocketServiceTest {

    @Mock
    private RestfulMockDAO restfulMockDAO;

    @Mock
    private UserTokenServiceUtils userTokenServiceUtils;

    @Mock
    private RuleEngine ruleEngine;

    @InjectMocks
    private WebSocketServiceImpl webSocketService;

    @Test
    void registerSession_tracksSessionAndExposesExternalId() {

        // Setup
        final Session session = Mockito.mock(Session.class);
        final UpgradeRequest upgradeRequest = Mockito.mock(UpgradeRequest.class);
        final UpgradeResponse upgradeResponse = Mockito.mock(UpgradeResponse.class);

        Mockito.when(session.getUpgradeRequest()).thenReturn(upgradeRequest);
        Mockito.when(upgradeRequest.getRequestURI()).thenReturn(URI.create("/ws"));
        Mockito.when(session.getUpgradeResponse()).thenReturn(upgradeResponse);
        Mockito.when(upgradeResponse.getHeader(GeneralUtils.LOG_REQ_ID)).thenReturn("trace-1");
        Mockito.when(upgradeResponse.getHeader("Sec-WebSocket-Accept")).thenReturn("handshake-1");
        Mockito.doNothing().when(session).setIdleTimeout(Mockito.any(Duration.class));

        final SmockinUser user = new SmockinUser();
        user.setRole(SmockinUserRoleEnum.SYS_ADMIN);

        final RestfulMock wsMock = new RestfulMock();
        wsMock.setPath("/ws");
        wsMock.setMockType(RestMockTypeEnum.PROXY_WS);
        wsMock.setProxyPushIdOnConnect(false);
        wsMock.setWebSocketTimeoutInMillis(0);
        wsMock.setCreatedBy(user);

        Mockito.when(restfulMockDAO.findActiveByMethodAndPathPatternAndTypesForSingleUser(
                Mockito.eq(RestMethodEnum.GET),
                Mockito.eq("/ws"),
                Mockito.anyList())).thenReturn(wsMock);

        // Test
        webSocketService.registerSession(session, false);
        final String externalId = webSocketService.getExternalId(session);

        // Assertions
        Assertions.assertNotNull(externalId);
    }
}
