package com.smockin.admin.websocket;

import com.smockin.admin.dto.LiveLoggingAction;
import com.smockin.admin.dto.LiveLoggingBlockedResponseAmendmentDTO;
import com.smockin.admin.dto.response.LiveLoggingDTO;
import com.smockin.admin.dto.response.LiveLoggingInboundContentDTO;
import com.smockin.admin.dto.response.LiveLoggingTrafficDTO;
import com.smockin.admin.enums.LiveLoggingDirectionEnum;
import com.smockin.admin.enums.LiveLoggingMessageTypeEnum;
import com.smockin.admin.enums.UserModeEnum;
import com.smockin.admin.persistence.dao.SmockinUserDAO;
import com.smockin.admin.service.SmockinUserService;
import com.smockin.admin.service.utils.MultiUserUtils;
import com.smockin.mockserver.dto.LiveLoggingUserOverrideResponse;
import com.smockin.mockserver.service.ResponseBlockingService;
import com.smockin.utils.GeneralUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class LiveLoggingHandlerImplTest {

    @Mock
    private ResponseBlockingService responseBlockingService;

    @Mock
    private MultiUserUtils multiUserUtils;

    @Mock
    private SmockinUserService smockinUserService;

    @Mock
    private SmockinUserDAO smockinUserDAO;

    @InjectMocks
    private LiveLoggingHandlerImpl liveLoggingHandler;

    @Test
    void broadcast_inactiveUserMode_sendsMessage() throws Exception {

        // Setup
        final WebSocketSession session = Mockito.mock(WebSocketSession.class);
        liveLoggingHandler.afterConnectionEstablished(session);
        Mockito.when(smockinUserService.getUserMode()).thenReturn(UserModeEnum.INACTIVE);

        final LiveLoggingInboundContentDTO content =
                new LiveLoggingInboundContentDTO(Map.of("h", "v"), "GET", "/path", "body", Map.of());
        final LiveLoggingTrafficDTO payload = new LiveLoggingTrafficDTO("1", LiveLoggingDirectionEnum.REQUEST, false, content);
        final LiveLoggingDTO dto = new LiveLoggingDTO(LiveLoggingMessageTypeEnum.TRAFFIC, payload);

        // Test
        liveLoggingHandler.broadcast(dto);

        // Assertions
        Mockito.verify(session).sendMessage(Mockito.any(TextMessage.class));
    }

    @Test
    void handleTextMessage_enableLiveBlocking_callsEngine() {

        // Setup
        final WebSocketSession session = Mockito.mock(WebSocketSession.class);
        final LiveLoggingAction<Void> action = new LiveLoggingAction<>();
        action.setType("ENABLE_LIVE_LOG_BLOCKING");

        // Test
        liveLoggingHandler.handleTextMessage(session, new TextMessage(GeneralUtils.serialiseJson(action)));

        // Assertions
        Mockito.verify(responseBlockingService).updateLiveBlockingMode(true);
    }

    @Test
    void handleTextMessage_disableLiveBlocking_clearsState() {

        // Setup
        final WebSocketSession session = Mockito.mock(WebSocketSession.class);
        final LiveLoggingAction<Void> action = new LiveLoggingAction<>();
        action.setType("DISABLE_LIVE_LOG_BLOCKING");

        Mockito.when(smockinUserService.getUserMode()).thenReturn(UserModeEnum.INACTIVE);

        // Test
        liveLoggingHandler.handleTextMessage(session, new TextMessage(GeneralUtils.serialiseJson(action)));

        // Assertions
        Mockito.verify(responseBlockingService).clearAllPathsFromLiveBlocking();
        Mockito.verify(responseBlockingService).updateLiveBlockingMode(false);
    }

    @Test
    void handleTextMessage_liveLoggingAmendment_releasesResponse() {

        // Setup
        final WebSocketSession session = Mockito.mock(WebSocketSession.class);

        final LiveLoggingBlockedResponseAmendmentDTO payload = new LiveLoggingBlockedResponseAmendmentDTO();
        payload.setTraceId("trace-1");
        payload.setStatus(201);
        payload.setHeaders(Map.of("X-Test", "1"));
        payload.setBody("ok");

        final LiveLoggingAction<LiveLoggingBlockedResponseAmendmentDTO> action = new LiveLoggingAction<>();
        action.setType("LIVE_LOGGING_AMENDMENT");
        action.setPayload(payload);

        // Test
        liveLoggingHandler.handleTextMessage(session, new TextMessage(GeneralUtils.serialiseJson(action)));

        // Assertions
        Mockito.verify(responseBlockingService).releaseBlockedLiveLoggingResponse(
                Mockito.eq("trace-1"),
                Mockito.argThat(opt ->
                        opt.isPresent()
                                && opt.get().equals(new LiveLoggingUserOverrideResponse(201, Map.of("X-Test", "1"), "ok"))));
    }
}
