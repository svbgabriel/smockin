package com.smockin.mockserver.service.ws;

import com.smockin.mockserver.service.WebSocketService;
import com.smockin.mockserver.service.enums.WebSocketCommandEnum;
import org.eclipse.jetty.websocket.api.Callback;
import org.eclipse.jetty.websocket.api.Session;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;

@ExtendWith(MockitoExtension.class)
class SparkWebSocketEchoServiceTest {

    @Mock
    private WebSocketService webSocketService;

    @Mock
    private Session session;

    @Test
    void message_sendsClientIdForCommand() throws IOException {

        // Setup
        final SparkWebSocketEchoService service = new SparkWebSocketEchoService(webSocketService, false);
        Mockito.when(webSocketService.getExternalId(session)).thenReturn("client-1");

        // Test
        service.message(session, WebSocketCommandEnum.SMOCKIN_ID.name());

        // Assertions
        Mockito.verify(session).sendText(Mockito.eq("client-1"), Mockito.any(Callback.class));
        Mockito.verify(webSocketService, Mockito.never()).respondToMessage(Mockito.any(), Mockito.any());
    }
}
