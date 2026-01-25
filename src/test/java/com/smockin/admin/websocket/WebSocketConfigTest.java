package com.smockin.admin.websocket;

import com.smockin.admin.enums.UserModeEnum;
import com.smockin.admin.persistence.entity.SmockinUser;
import com.smockin.admin.persistence.enums.SmockinUserRoleEnum;
import com.smockin.admin.service.SmockinUserService;
import com.smockin.admin.service.utils.UserTokenServiceUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import jakarta.servlet.ServletContext;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

@ExtendWith(MockitoExtension.class)
class WebSocketConfigTest {

    @Mock
    private ServletContext servletContext;

    @Mock
    private LiveLoggingHandler mockLogFeedHandler;

    @Mock
    private UserTokenServiceUtils userTokenServiceUtils;

    @Mock
    private SmockinUserService smockinUserService;

    @Test
    void userInterceptor_activeUser_populatesAttributes() throws Exception {

        // Setup
        final WebSocketConfig config = new WebSocketConfig(
                servletContext, mockLogFeedHandler, userTokenServiceUtils, smockinUserService);
        final HandshakeInterceptor interceptor = config.userInterceptor();

        final ServerHttpRequest request = Mockito.mock(ServerHttpRequest.class);
        final ServerHttpResponse response = Mockito.mock(ServerHttpResponse.class);
        final WebSocketHandler wsHandler = Mockito.mock(WebSocketHandler.class);
        final Map<String, Object> attributes = new HashMap<>();

        final SmockinUser user = new SmockinUser();
        user.setRole(SmockinUserRoleEnum.SYS_ADMIN);
        user.setCtxPath("bob");
        user.setExtId("user-1");

        Mockito.when(smockinUserService.getUserMode()).thenReturn(UserModeEnum.ACTIVE);
        Mockito.when(request.getURI()).thenReturn(URI.create("/liveLoggingFeed/true/token123"));
        Mockito.when(userTokenServiceUtils.loadCurrentActiveUser("token123")).thenReturn(user);

        // Test
        final boolean result = interceptor.beforeHandshake(request, response, wsHandler, attributes);

        // Assertions
        Assertions.assertTrue(result);
        Assertions.assertEquals(Boolean.TRUE, attributes.get(LiveLoggingHandler.WS_CONNECTED_USER_ADMIN_VIEW_ALL));
        Assertions.assertEquals(SmockinUserRoleEnum.SYS_ADMIN, attributes.get(LiveLoggingHandler.WS_CONNECTED_USER_ROLE));
        Assertions.assertEquals("bob", attributes.get(LiveLoggingHandler.WS_CONNECTED_USER_CTX_PATH));
        Assertions.assertEquals("user-1", attributes.get(LiveLoggingHandler.WS_CONNECTED_USER_ID));
    }

    @Test
    void userInterceptor_inactiveUserMode_allowsHandshake() throws Exception {

        // Setup
        final WebSocketConfig config = new WebSocketConfig(
                servletContext, mockLogFeedHandler, userTokenServiceUtils, smockinUserService);
        final HandshakeInterceptor interceptor = config.userInterceptor();

        final ServerHttpRequest request = Mockito.mock(ServerHttpRequest.class);
        final ServerHttpResponse response = Mockito.mock(ServerHttpResponse.class);
        final WebSocketHandler wsHandler = Mockito.mock(WebSocketHandler.class);
        final Map<String, Object> attributes = new HashMap<>();

        Mockito.when(smockinUserService.getUserMode()).thenReturn(UserModeEnum.INACTIVE);

        // Test
        final boolean result = interceptor.beforeHandshake(request, response, wsHandler, attributes);

        // Assertions
        Assertions.assertTrue(result);
        Mockito.verifyNoInteractions(userTokenServiceUtils);
    }
}
