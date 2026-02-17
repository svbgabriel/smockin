package com.smockin.admin.websocket;

import com.fasterxml.jackson.core.type.TypeReference;
import com.smockin.admin.dto.LiveLoggingAction;
import com.smockin.admin.dto.LiveLoggingBlockedResponseAmendmentDTO;
import com.smockin.admin.dto.response.LiveLoggingDTO;
import com.smockin.admin.dto.response.LiveLoggingS3DTO;
import com.smockin.admin.dto.response.LiveLoggingTrafficDTO;
import com.smockin.admin.enums.LiveLoggingMessageTypeEnum;
import com.smockin.admin.enums.UserModeEnum;
import com.smockin.admin.persistence.dao.SmockinUserDAO;
import com.smockin.admin.persistence.enums.SmockinUserRoleEnum;
import com.smockin.admin.service.SmockinUserService;
import com.smockin.admin.service.utils.MultiUserUtils;
import com.smockin.mockserver.dto.LiveLoggingUserOverrideResponse;
import com.smockin.mockserver.service.ResponseBlockingService;
import com.smockin.utils.GeneralUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class LiveLoggingHandlerImpl extends TextWebSocketHandler implements LiveLoggingHandler {

    private final Logger logger = LoggerFactory.getLogger(LiveLoggingHandlerImpl.class);

    private static final String ENABLE_LIVE_LOG_BLOCKING = "ENABLE_LIVE_LOG_BLOCKING";
    private static final String DISABLE_LIVE_LOG_BLOCKING = "DISABLE_LIVE_LOG_BLOCKING";
    private static final String LIVE_LOGGING_AMENDMENT = "LIVE_LOGGING_AMENDMENT";

    private final AtomicReference<List<WebSocketSession>> liveSessionsRef = new AtomicReference<>(new ArrayList<>());

    private final ResponseBlockingService responseBlockingService;
    private final MultiUserUtils multiUserUtils;
    private final SmockinUserService smockinUserService;
    private final SmockinUserDAO smockinUserDAO;

    public LiveLoggingHandlerImpl(ResponseBlockingService responseBlockingService, MultiUserUtils multiUserUtils, SmockinUserService smockinUserService, SmockinUserDAO smockinUserDAO) {
        this.responseBlockingService = responseBlockingService;
        this.multiUserUtils = multiUserUtils;
        this.smockinUserService = smockinUserService;
        this.smockinUserDAO = smockinUserDAO;
    }


    @Override
    public void afterConnectionEstablished(WebSocketSession session) {

        liveSessionsRef.get().add(session);
    }

    @Override
    public void afterConnectionClosed(final WebSocketSession session,
                                      final CloseStatus status) throws Exception {

        super.afterConnectionClosed(session, status);

        logger.debug("Live logging WS connection closed");

        liveSessionsRef.get().remove(session);

        stopLiveBlockingMode(session);

    }

    @Override
    protected void handleTextMessage(final WebSocketSession session,
                                     final TextMessage message) {

        if (StringUtils.isBlank(message.getPayload())) {
            return;
        }

        final LiveLoggingAction clientAction
                = GeneralUtils.deserializeJson(message.getPayload(), new TypeReference<LiveLoggingAction<?>>() {
        });

        if (clientAction == null) {
            return;
        }

        final String type = clientAction.getType();

        if (Strings.CS.equals(ENABLE_LIVE_LOG_BLOCKING, type)) {
            responseBlockingService.updateLiveBlockingMode(true);
        } else if (Strings.CS.equals(DISABLE_LIVE_LOG_BLOCKING, type)) {
            stopLiveBlockingMode(session);
        } else if (Strings.CS.equals(LIVE_LOGGING_AMENDMENT, type)) {
            handleLiveLoggingResponseAmendment(message);
        }

    }

    @Override
    public synchronized void broadcast(final LiveLoggingDTO dto) {

        final List<WebSocketSession> sessions = liveSessionsRef.get();

        if (sessions.isEmpty()) {
            return;
        }

        sessions
                .forEach(s ->
                        handleBroadcast(dto, s));

    }

    private void stopLiveBlockingMode(final WebSocketSession session) {

        if (!UserModeEnum.ACTIVE.equals(smockinUserService.getUserMode())
                || liveSessionsRef.get().isEmpty()) {
            responseBlockingService.clearAllPathsFromLiveBlocking();
            responseBlockingService.updateLiveBlockingMode(false);
            return;
        }

        // Release blocked calls just for user
        responseBlockingService.notifyBlockedLiveLoggingCalls(null, GeneralUtils.URL_PATH_SEPARATOR + session.getAttributes().get(WS_CONNECTED_USER_CTX_PATH));
        responseBlockingService.clearAllPathsFromLiveBlockingForUser((String) session.getAttributes().get(WS_CONNECTED_USER_ID));
    }

    private void handleLiveLoggingResponseAmendment(final TextMessage message) {

        final LiveLoggingAction<LiveLoggingBlockedResponseAmendmentDTO> liveLoggingAction
                = GeneralUtils.deserializeJson(message.getPayload(),
                new TypeReference<>() {
                });

        final LiveLoggingBlockedResponseAmendmentDTO amendmentDTO = liveLoggingAction.getPayload();

        responseBlockingService.releaseBlockedLiveLoggingResponse(
                amendmentDTO.getTraceId(),
                Optional.of(new LiveLoggingUserOverrideResponse(
                        amendmentDTO.getStatus(),
                        amendmentDTO.getHeaders(),
                        amendmentDTO.getBody())));

    }

    private TextMessage serialiseMessage(final LiveLoggingDTO dto) {
        return new TextMessage(GeneralUtils.serialiseJson(dto));
    }

    void handleBroadcast(final LiveLoggingDTO dto,
                         final WebSocketSession session) {

        try {

            // Not in multi-user mode, so just send it to the single user
            if (!UserModeEnum.ACTIVE.equals(smockinUserService.getUserMode())) {
                session.sendMessage(serialiseMessage(dto));
                return;
            }

            final boolean adminViewAll = (Boolean) session.getAttributes().get(WS_CONNECTED_USER_ADMIN_VIEW_ALL);

            if (LiveLoggingMessageTypeEnum.TRAFFIC.equals(dto.getType())
                    || LiveLoggingMessageTypeEnum.BLOCKED_RESPONSE.equals(dto.getType())) {

                //
                // Multi user mode logic...
                final String inboundPath = ((LiveLoggingTrafficDTO) dto.getPayload()).getContent().getUrl();
                final String userCtxPath = findUserCtxPath(session);

                if (isSysAdmin(session)) {

                    if (adminViewAll) {
                        session.sendMessage(serialiseMessage(dto));
                        return;
                    }

                    final String userCtxSegmentFromInboundPath = multiUserUtils.extractMultiUserCtxPathSegment(inboundPath);

                    // TODO
                    // This function will eventually move to using a cache, as at the moment we are making a DB call for EVERY SINGLE
                    // live logging broadcast where the admin user DOES NOT want to see calls from other users!
                    if (!multiUserUtils.isInboundPathMultiUserPath(userCtxSegmentFromInboundPath)) {
                        session.sendMessage(serialiseMessage(dto));
                    }

                    return;
                }

                if (Strings.CS.startsWith(inboundPath, userCtxPath)) {
                    session.sendMessage(serialiseMessage(dto));
                }

                return;
            }

            if (LiveLoggingMessageTypeEnum.S3.equals(dto.getType())) {

                if (isSysAdmin(session) && adminViewAll) {

                    session.sendMessage(serialiseMessage(dto));
                    return;
                }

                final String connectedUserId = (String) session.getAttributes().get(WS_CONNECTED_USER_ID);

                if (!Strings.CS.equals(((LiveLoggingS3DTO) dto.getPayload()).getBucketOwnerId(), connectedUserId)) {
                    return;
                }

                session.sendMessage(serialiseMessage(dto));

            }

        } catch (IOException e) {
            logger.error("Error pushing message to connected web socket: {}", session.getId(), e);
        }

    }

    private boolean isSysAdmin(final WebSocketSession session) {

        return SmockinUserRoleEnum.SYS_ADMIN.equals(session.getAttributes().get(WS_CONNECTED_USER_ROLE));
    }

    private String findUserCtxPath(final WebSocketSession session) {

        return isSysAdmin(session)
                ? ""
                : GeneralUtils.URL_PATH_SEPARATOR + session.getAttributes().get(WS_CONNECTED_USER_CTX_PATH);
    }

}