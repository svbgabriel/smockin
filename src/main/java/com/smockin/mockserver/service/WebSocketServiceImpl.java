package com.smockin.mockserver.service;

import com.smockin.admin.exception.RecordNotFoundException;
import com.smockin.admin.exception.ValidationException;
import com.smockin.admin.persistence.dao.RestfulMockDAO;
import com.smockin.admin.persistence.entity.RestfulMock;
import com.smockin.admin.persistence.entity.RestfulMockDefinitionOrder;
import com.smockin.admin.persistence.enums.RestMethodEnum;
import com.smockin.admin.persistence.enums.RestMockTypeEnum;
import com.smockin.admin.service.utils.UserTokenServiceUtils;
import com.smockin.mockserver.engine.MockedRestServerEngineUtils;
import com.smockin.mockserver.exception.MockServerException;
import com.smockin.mockserver.service.dto.PushClientDTO;
import com.smockin.mockserver.service.dto.RestfulResponseDTO;
import com.smockin.mockserver.service.dto.WebSocketDTO;
import com.smockin.utils.GeneralUtils;
import org.eclipse.jetty.websocket.api.Callback;
import org.eclipse.jetty.websocket.api.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by mgallina.
 */
@Service
@Transactional
public class WebSocketServiceImpl implements WebSocketService {

    private final Logger logger = LoggerFactory.getLogger(WebSocketServiceImpl.class);

    private static final String WS_HAND_SHAKE_KEY = "Sec-WebSocket-Accept";

    @Autowired
    private RestfulMockDAO restfulMockDAO;

    @Autowired
    private MockedRestServerEngineUtils mockedRestServerEngineUtils;

    @Autowired
    private UserTokenServiceUtils userTokenServiceUtils;

//    @Autowired
//    private LiveLoggingHandler liveLoggingHandler;

    @Autowired
    private RuleEngine ruleEngine;

    private final ConcurrentHashMap<String, Set<SessionIdWrapper>> sessionMap = new ConcurrentHashMap<>();

    public void registerSession(final Session session, final boolean isMultiUserMode) {
        logger.debug("registerSession called");

        final String wsPath = session.getUpgradeRequest().getRequestURI().getPath();
        final List<RestMockTypeEnum> types = Arrays.asList(RestMockTypeEnum.PROXY_WS, RestMockTypeEnum.RULE_WS);

        final RestfulMock wsMock = (isMultiUserMode)
                ? restfulMockDAO.findActiveByMethodAndPathPatternAndTypesForMultiUser(RestMethodEnum.GET, wsPath, types)
                : restfulMockDAO.findActiveByMethodAndPathPatternAndTypesForSingleUser(RestMethodEnum.GET, wsPath, types);

        if (wsMock == null) {
            if (session.isOpen()) {
                session.sendText("No suitable mock found for " + wsPath, Callback.from(
                        session::close,
                        t -> {
                            logger.error("Error sending error message", t);
                            session.close();
                        }
                ));
            }
            return;
        }

        Duration timeout = (wsMock.getWebSocketTimeoutInMillis() > 0) ? Duration.of(wsMock.getWebSocketTimeoutInMillis(), ChronoUnit.MILLIS) : Duration.of(MAX_IDLE_TIMEOUT_MILLIS, ChronoUnit.MILLIS);
        final String path = mockedRestServerEngineUtils.buildUserPath(wsMock);
        session.setIdleTimeout(timeout);

        final Set<SessionIdWrapper> sessions = sessionMap.computeIfAbsent(path, k -> Collections.synchronizedSet(new HashSet<>()));
        final String assignedId = GeneralUtils.generateUUID();
        final String traceId = session.getUpgradeResponse().getHeader(GeneralUtils.LOG_REQ_ID);

        sessions.add(new SessionIdWrapper(assignedId, traceId, session, GeneralUtils.getCurrentDate()));

        if (wsMock.isProxyPushIdOnConnect()) {
            sendMessage(assignedId, new WebSocketDTO(path, "clientId: " + assignedId));
        }

        if (wsMock.getMockType() == RestMockTypeEnum.RULE_WS && !wsMock.getDefinitions().isEmpty()) {
            RestfulMockDefinitionOrder order = wsMock.getDefinitions().getFirst();
            if (order.getResponseBody() != null) {
                sendMessage(assignedId, new WebSocketDTO(path, order.getResponseBody()));
            }
        }
    }

    public void respondToMessage(final Session session, final String message) {
        logger.debug("respondToMessage called");

        final String wsPath = session.getUpgradeRequest().getRequestURI().getPath();

        if (session.isOpen()) {
            final String sessionHandshake = session.getUpgradeResponse().getHeader(WS_HAND_SHAKE_KEY);
            Set<SessionIdWrapper> sessions = sessionMap.get(wsPath);

            final RestfulMock wsMock = restfulMockDAO.findActiveByMethodAndPathPatternAndTypesForSingleUser(
                    RestMethodEnum.GET, wsPath, List.of(RestMockTypeEnum.RULE_WS));

            if (wsMock != null && !wsMock.getDefinitions().isEmpty()) {
                RestfulMockDefinitionOrder order = wsMock.getDefinitions().get(0);

                HttpServletRequest req = new sMockinRequest(message, wsPath);
                RestfulResponseDTO response = ruleEngine.process(req, wsMock.getRules());

                String bodyToSend = (response != null && response.getResponseBody() != null)
                        ? response.getResponseBody()
                        : order.getResponseBody();

                if (bodyToSend != null && sessions != null) {
                    sessions.stream()
                            .filter(w -> sessionHandshake.equals(w.getSession().getUpgradeResponse().getHeader(WS_HAND_SHAKE_KEY)))
                            .findFirst()
                            .ifPresent(w -> sendMessage(w.getId(), new WebSocketDTO(wsPath, bodyToSend)));
                }
            }
        }
    }

    public void removeSession(final Session session) {
        final String sessionHandshake = session.getUpgradeResponse().getHeader(WS_HAND_SHAKE_KEY);
        sessionMap.values().forEach(set ->
                set.removeIf(s -> s.getSession().getUpgradeResponse().getHeader(WS_HAND_SHAKE_KEY).equals(sessionHandshake))
        );
    }

    public void sendMessage(final String id, final WebSocketDTO dto) {
        dto.setBody(GeneralUtils.removeAllLineBreaks(dto.getBody()));
        Set<SessionIdWrapper> sessions = sessionMap.get(dto.getPath());

        if (sessions != null) {
            sessions.stream()
                    .filter(s -> s.getId().equals(id))
                    .findFirst()
                    .ifPresent(s -> {
                        s.getSession().sendText(dto.getBody(), Callback.from(
                                () -> {},
                                t -> logger.error("Error sending message to session " + id, t)
                        ));
                    });
        }
    }

    public List<PushClientDTO> getClientConnections(final String mockExtId, final String token) throws RecordNotFoundException, ValidationException {
        final RestfulMock mock = restfulMockDAO.findByExtId(mockExtId);
        if (mock == null) throw new RecordNotFoundException();

        userTokenServiceUtils.validateRecordOwner(mock.getCreatedBy(), token);
        final String prefixedPath = mockedRestServerEngineUtils.buildUserPath(mock);

        List<PushClientDTO> connections = new ArrayList<>();
        Optional.ofNullable(sessionMap.get(prefixedPath))
                .ifPresent(set -> set.forEach(s -> connections.add(new PushClientDTO(s.getId(), s.getDateJoined()))));

        return connections;
    }

    public String getExternalId(final Session session) {
        final String wsPath = session.getUpgradeRequest().getRequestURI().getPath();
        final String sessionHandshake = session.getUpgradeResponse().getHeader(WS_HAND_SHAKE_KEY);

        return Optional.ofNullable(sessionMap.get(wsPath))
                .flatMap(set -> set.stream()
                        .filter(sw -> sw.getSession().getUpgradeResponse().getHeader(WS_HAND_SHAKE_KEY).equals(sessionHandshake))
                        .map(SessionIdWrapper::getId)
                        .findFirst())
                .orElse(null);
    }

    /**
     * Implementação simulada de HttpServletRequest para que a RuleEngine
     * consiga processar mensagens WebSocket como se fossem corpos de requisição HTTP.
     */
    private static class sMockinRequest extends HttpServletRequestWrapper {
        private final String body;
        private final String path;

        sMockinRequest(String value, String path) {
            super(null); // Não precisamos de uma requisição real delegada aqui
            this.body = value;
            this.path = path;
        }

        @Override
        public String getPathInfo() {
            return path;
        }

        @Override
        public String getMethod() {
            return "POST";
        }

        @Override
        public ServletInputStream getInputStream() {
            final ByteArrayInputStream bais = new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8));
            return new ServletInputStream() {
                @Override
                public int read() {
                    return bais.read();
                }

                @Override
                public boolean isFinished() {
                    return bais.available() == 0;
                }

                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setReadListener(ReadListener readListener) {
                }
            };
        }

        // Métodos obrigatórios por interface que não serão usados pela RuleEngine
        @Override
        public String getHeader(String name) {
            return null;
        }

        @Override
        public Map<String, String[]> getParameterMap() {
            return Collections.emptyMap();
        }
    }

    private static final class SessionIdWrapper {
        private final String id;
        private final String traceId;
        private final Session session;
        private final Date dateJoined;

        public SessionIdWrapper(String id, String traceId, Session session, Date dateJoined) {
            this.id = id;
            this.traceId = traceId;
            this.session = session;
            this.dateJoined = dateJoined;
        }

        public String getId() {
            return id;
        }

        public String getTraceId() {
            return traceId;
        }

        public Session getSession() {
            return session;
        }

        public Date getDateJoined() {
            return dateJoined;
        }
    }

    public void clearSession() {
        sessionMap.clear();
    }
}
