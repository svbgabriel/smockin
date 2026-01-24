package com.smockin.mockserver.service;

import com.smockin.admin.exception.RecordNotFoundException;
import com.smockin.admin.exception.ValidationException;
import com.smockin.admin.persistence.dao.RestfulMockDAO;
import com.smockin.admin.persistence.entity.RestfulMock;
import com.smockin.admin.service.utils.UserTokenServiceUtils;
import com.smockin.admin.websocket.LiveLoggingHandler;
import com.smockin.mockserver.engine.MockedRestServerEngineUtils;
import com.smockin.mockserver.service.dto.SseMessageDTO;
import com.smockin.mockserver.service.dto.PushClientDTO;
import com.smockin.utils.GeneralUtils;
import com.smockin.utils.LiveLoggingUtils;
import org.eclipse.jetty.io.RuntimeIOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by mgallina
 */
@Service
@Transactional
public class ServerSideEventServiceImpl implements ServerSideEventService {

    private final Logger logger = LoggerFactory.getLogger(ServerSideEventServiceImpl.class);

    private final ConcurrentHashMap<String, ClientSseData> clients = new ConcurrentHashMap<>(0);

    private final String messagePrefix = "data: ";
    private final String messageSuffix = "\n\n";

    @Autowired
    private RestfulMockDAO restfulMockDAO;

    @Autowired
    private MockedRestServerEngineUtils mockedRestServerEngineUtils;

    @Autowired
    private UserTokenServiceUtils userTokenServiceUtils;

    @Autowired
    private LiveLoggingHandler liveLoggingHandler;

    @Override
    public void register(final String path,
                         final long heartBeatMillis,
                         final boolean proxyPushIdOnConnect,
                         final HttpServletRequest request,
                         final HttpServletResponse response) throws IOException {
        logger.debug("register called");

        final String clientId = GeneralUtils.generateUUID();
        final String traceId = (String) request.getAttribute(GeneralUtils.LOG_REQ_ID);

        applyHeaders(response);

        // Register the client and build a messages collection
        clients.computeIfAbsent(clientId, k ->
                new ClientSseData(path, Thread.currentThread(), GeneralUtils.getCurrentDate()));

        liveLoggingHandler.broadcast(LiveLoggingUtils.buildLiveLogOutboundDTO(traceId, path, response.getStatus(), null, "SSE established (clientId: " + clientId + ")", false));

        initHeartBeat(clientId, heartBeatMillis, proxyPushIdOnConnect, traceId, path, response);
    }

    @Override
    public List<PushClientDTO> getClientConnections(final String mockExtId, final String token) throws RecordNotFoundException, ValidationException {

        final RestfulMock mock = restfulMockDAO.findByExtId(mockExtId);

        if (mock == null)
            throw new RecordNotFoundException();

        userTokenServiceUtils.validateRecordOwner(mock.getCreatedBy(), token);

        final String prefixedPath = mockedRestServerEngineUtils.buildUserPath(mock);
        final List<PushClientDTO> sessionIds = new ArrayList<>();

        clients.forEach( (id, data) -> {
            if (data.getPath().equals(prefixedPath)) {
                sessionIds.add(new PushClientDTO(id, data.getDateJoined()));
            }
        });

        return sessionIds;
    }

    @Override
    public void addMessage(final String id, final SseMessageDTO dto) {
        logger.debug("addMessage called");

        dto.setBody(GeneralUtils.removeAllLineBreaks(dto.getBody()));

        if (id != null && clients.containsKey(id)) {
            clients.get(id).getMessages().add(dto.getBody());
            return;
        }

        clients.values().forEach(data -> {
            if (data.getPath().equals(dto.getPath())) {
                data.getMessages().add(dto.getBody());
            }
        });
    }

    @Override
    public void interruptAndClearAllHeartBeatThreads() {
        clients.forEach( (key, msgs) -> msgs.getThread().interrupt());
        clients.clear();
    }

    @Override
    public void clearState() {
        clients.clear();
    }

    void applyHeaders(final HttpServletResponse res) {
        res.setHeader(HttpHeaders.CONTENT_TYPE, SSE_EVENT_STREAM_HEADER);
        res.setHeader(HttpHeaders.CACHE_CONTROL, "no-cache");
        res.setHeader(HttpHeaders.CONNECTION, "keep-alive");
    }

    void initHeartBeat(final String clientId,
                       final long heartBeatMillis,
                       final boolean proxyPushIdOnConnect,
                       final String traceId,
                       final String path,
                       final HttpServletResponse response) throws IOException {
        logger.debug("initHeartBeat called");

        final PrintWriter writer = response.getWriter();

        if (proxyPushIdOnConnect) {
            writer.write(messagePrefix + "clientId: " + clientId + messageSuffix);
        }

        while (!Thread.currentThread().isInterrupted()) {

            final ClientSseData clientData = clients.get(clientId);
            if (clientData == null) break;

            final List<String> messages = clientData.getMessages();

            try {

                if (!messages.isEmpty()) {
                    final Iterator<String> it = messages.iterator();
                    while (it.hasNext()) {
                        final String body = messagePrefix + it.next();
                        writer.write(body + messageSuffix);
                        it.remove();
                        liveLoggingHandler.broadcast(LiveLoggingUtils.buildLiveLogOutboundDTO(traceId, path, 200, null, body, false));
                    }
                } else {
                    writer.write(messagePrefix + messageSuffix);
                }

                if (writer.checkError()) {
                    throw new IOException("Writer error detected (client likely disconnected)");
                }

                writer.flush();

            } catch (Exception ex) {
                logger.info("Closing SSE connection for client {}", clientId);
                clients.remove(clientId);
                liveLoggingHandler.broadcast(LiveLoggingUtils.buildLiveLogOutboundDTO(traceId, path, 200, null, "SSE client connection closed", false));
                break;
            }

            try {
                Thread.sleep(heartBeatMillis);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private static final class ClientSseData {
        private final String path;
        private final Thread thread;
        private final Date dateJoined;
        private final List<String> messages = new ArrayList<>();

        public ClientSseData(final String path, final Thread thread, final Date dateJoined) {
            this.path = path;
            this.thread = thread;
            this.dateJoined = dateJoined;
        }
        public String getPath() { return path; }
        public Thread getThread() { return thread; }
        public Date getDateJoined() { return dateJoined; }
        public List<String> getMessages() { return messages; }
    }
}
