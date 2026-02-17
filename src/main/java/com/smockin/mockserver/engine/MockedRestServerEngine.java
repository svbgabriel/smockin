package com.smockin.mockserver.engine;

import com.smockin.admin.enums.UserModeEnum;
import com.smockin.admin.exception.ValidationException;
import com.smockin.admin.persistence.enums.RestMethodEnum;
import com.smockin.admin.service.SmockinUserService;
import com.smockin.admin.websocket.LiveLoggingHandler;
import com.smockin.mockserver.dto.*;
import com.smockin.mockserver.exception.MockServerException;
import com.smockin.mockserver.service.*;
import com.smockin.utils.GeneralUtils;
import com.smockin.utils.LiveLoggingUtils;
import jakarta.servlet.MultipartConfigElement;
import jakarta.servlet.ServletRegistration;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.embedded.jetty.JettyServletWebServerFactory;
import org.springframework.boot.web.server.WebServer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Created by mgallina.
 */
@Service
@Transactional(readOnly = true)
public class MockedRestServerEngine {

    private final Logger logger = LoggerFactory.getLogger(MockedRestServerEngine.class);

    @Autowired
    private RuleEngine ruleEngine;
    @Autowired
    private HttpProxyService proxyService;
    @Autowired
    private MockOrderingCounterService mockOrderingCounterService;
    @Autowired
    private InboundParamMatchService inboundParamMatchService;
    @Autowired
    private WebSocketService webSocketService;
    @Autowired
    private ServerSideEventService serverSideEventService;
    @Autowired
    private MockedRestServerEngineUtils mockedRestServerEngineUtils;
    @Autowired
    private LiveLoggingHandler liveLoggingHandler;
    @Autowired
    private SmockinUserService smockinUserService;
    @Autowired
    private ProxyMappingCache proxyMappingCache;
    @Autowired
    private ResponseBlockingService responseBlockingService;

    private WebServer server;
    private final Object serverStateMonitor = new Object();
    private final MockServerState serverState = new MockServerState(false, 0);

    private final AtomicBoolean proxyModeEnabled = new AtomicBoolean();

    public void start(final MockedServerConfigDTO config,
                      final List<ProxyForwardConfigCacheDTO> allProxyForwardConfig) throws MockServerException {

        logger.debug("start called");

        synchronized (serverStateMonitor) {
            if (serverState.isRunning()) {
                shutdown();
            }
        }

        updateProxyMode(config.isProxyMode());
        proxyMappingCache.init(allProxyForwardConfig);

        final boolean isMultiUserMode = UserModeEnum.ACTIVE.equals(smockinUserService.getUserMode());

        initServer(config, isMultiUserMode);
    }

    private void initServer(final MockedServerConfigDTO config, final boolean isMultiUserMode) throws MockServerException {
        try {
            JettyServletWebServerFactory factory = new JettyServletWebServerFactory(config.getPort());

            factory.addServerCustomizers(jettyServer -> {
                if (jettyServer.getThreadPool() instanceof org.eclipse.jetty.util.thread.QueuedThreadPool pool) {
                    pool.setMaxThreads(config.getMaxThreads());
                    pool.setMinThreads(config.getMinThreads());
                    pool.setIdleTimeout(config.getTimeOutMillis());
                }
            });

            HttpServlet mockHandler = new HttpServlet() {
                @Override
                protected void service(HttpServletRequest req, HttpServletResponse resp) throws IOException {
                    handleRequest(req, resp, isMultiUserMode, config);
                }
            };

            factory.addInitializers(servletContext -> {
                ServletRegistration.Dynamic registration = servletContext.addServlet("mockServlet", mockHandler);
                registration.addMapping("/*");

                registration.setMultipartConfig(new MultipartConfigElement(
                        System.getProperty("java.io.tmpdir"),
                        -1L,
                        -1L,
                        0));
            });

            this.server = factory.getWebServer();
            this.server.start();

            synchronized (serverStateMonitor) {
                serverState.setRunning(true);
                serverState.setPort(server.getPort());
            }
        } catch (Exception ex) {
            throw new MockServerException(ex);
        }
    }

    private void handleRequest(HttpServletRequest request, HttpServletResponse response, boolean isMultiUserMode, MockedServerConfigDTO config) throws IOException {

        final String traceId = GeneralUtils.generateUUID();
        request.setAttribute(GeneralUtils.LOG_REQ_ID, traceId);
        response.addHeader(GeneralUtils.LOG_REQ_ID, traceId);

        // CORS
        handleCORS(request, response, config);
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            response.setStatus(HttpStatus.OK.value());
            return;
        }

        // Live Logging - Inbound
        broadcastInboundLogging(request, traceId);

        try {
            // Mock Processing
            Optional<String> mockResponseOpt = mockedRestServerEngineUtils.loadMockedResponse(request, response, isMultiUserMode, proxyModeEnabled.get());
            String responseBody = mockResponseOpt.orElse("");

            if (mockResponseOpt.isEmpty()) {
                response.setStatus(HttpStatus.NOT_FOUND.value());
            }

            // Blocking logic
            Optional<String> amendment = checkForAndHandleBlockSwapAndMock(request, response, responseBody, isMultiUserMode);
            final String finalBody = amendment.orElse(responseBody);

            // Live Logging - Outbound
            broadcastOutboundLogging(request, response, finalBody, traceId);

            response.getWriter().write(finalBody);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
    }

    private void broadcastInboundLogging(HttpServletRequest request, String traceId) {
        Map<String, String> reqHeaders = new HashMap<>();
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String h = headerNames.nextElement();
            reqHeaders.put(h, request.getHeader(h));
        }
        reqHeaders.put(GeneralUtils.LOG_REQ_ID, traceId);

        liveLoggingHandler.broadcast(LiveLoggingUtils.buildLiveLogInboundDTO(
                traceId,
                request.getMethod(),
                request.getPathInfo(),
                reqHeaders,
                GeneralUtils.extractRequestBody(request),
                proxyModeEnabled.get(),
                GeneralUtils.extractAllRequestParams(request)));
    }

    private void broadcastOutboundLogging(HttpServletRequest request, HttpServletResponse response, String body, String traceId) {
        if (ServerSideEventService.SSE_EVENT_STREAM_HEADER.equals(response.getHeader(HttpHeaders.CONTENT_TYPE))) {
            return;
        }

        Map<String, String> respHeaders = mockedRestServerEngineUtils.extractResponseHeadersAsMap(response);
        respHeaders.put(GeneralUtils.LOG_REQ_ID, traceId);

        liveLoggingHandler.broadcast(LiveLoggingUtils.buildLiveLogOutboundDTO(
                traceId,
                request.getPathInfo(),
                response.getStatus(),
                respHeaders,
                body,
                proxyModeEnabled.get()));
    }

    private Optional<String> checkForAndHandleBlockSwapAndMock(HttpServletRequest request, HttpServletResponse response, String currentBody, boolean isMultiUserMode) throws InterruptedException {

        if (blockLoggingResponse(request, response, currentBody)) {
            synchronized (responseBlockingService.getResponseBlockingMonitor()) {
                while (true) {
                    responseBlockingService.getResponseBlockingMonitor().wait();
                    String traceId = (String) request.getAttribute(GeneralUtils.LOG_REQ_ID);

                    if (!responseBlockingService.isLiveBlockingModeEnabled()) break;

                    if (isMultiUserMode && responseBlockingService.shouldReleaseUserCall(request)) break;

                    if (!responseBlockingService.hasAmendment(traceId)) continue;

                    Optional<LiveLoggingUserOverrideResponse> amendmentOpt = responseBlockingService.getAndRemoveAmendment(traceId);
                    if (amendmentOpt.isPresent()) {
                        return Optional.of(amendResponse(amendmentOpt.get(), response));
                    }
                    break;
                }
            }
        }
        return Optional.empty();
    }

    private String amendResponse(LiveLoggingUserOverrideResponse amendment, HttpServletResponse response) {
        amendment.getResponseHeaders().forEach(response::setHeader);
        response.setStatus(amendment.getStatus());
        return amendment.getBody();
    }

    private boolean blockLoggingResponse(HttpServletRequest request, HttpServletResponse response, String body) {
        if (responseBlockingService.isLiveBlockingModeEnabled()) {
            boolean match = responseBlockingService.isPathBlocked(request.getMethod(), request.getPathInfo());

            if (match) {
                liveLoggingHandler.broadcast(LiveLoggingUtils.buildLiveLogInterceptedResponseDTO(
                        (String) request.getAttribute(GeneralUtils.LOG_REQ_ID),
                        request.getPathInfo(),
                        response.getStatus(),
                        mockedRestServerEngineUtils.extractResponseHeadersAsMap(response),
                        body,
                        proxyModeEnabled.get()));
                return true;
            }
        }
        return false;
    }

    private void handleCORS(HttpServletRequest request, HttpServletResponse response, MockedServerConfigDTO config) {
        final String enableCors = config.getNativeProperties().get(GeneralUtils.ENABLE_CORS_PARAM);
        if (!Boolean.TRUE.toString().equalsIgnoreCase(enableCors)) return;

        response.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            String reqHeaders = request.getHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS);
            if (reqHeaders != null) response.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, reqHeaders);
            String reqMethod = request.getHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD);
            if (reqMethod != null) response.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, reqMethod);
        }
    }

    public void shutdown() throws MockServerException {
        try {
            serverSideEventService.interruptAndClearAllHeartBeatThreads();
            if (server != null) {
                server.stop();
            }
            synchronized (serverStateMonitor) {
                serverState.setRunning(false);
            }
            clearState();
        } catch (Exception ex) {
            throw new MockServerException(ex);
        }
    }

    public MockServerState getCurrentState() throws MockServerException {
        synchronized (serverStateMonitor) {
            return serverState;
        }
    }

    public void updateProxyMode(final boolean enable) {
        proxyModeEnabled.set(enable);
    }

    public void releaseBlockedLiveLoggingResponse(final String traceId, final Optional<LiveLoggingUserOverrideResponse> responseAmendmentOpt) {
        responseBlockingService.releaseBlockedLiveLoggingResponse(traceId, responseAmendmentOpt);
    }

    public void updateLiveBlockingMode(final boolean liveBlockEnabled) {
        responseBlockingService.updateLiveBlockingMode(liveBlockEnabled);
    }

    public void notifyBlockedLiveLoggingCalls(final RestMethodEnum method, final String userCtxOrFullPath) {
        responseBlockingService.notifyBlockedLiveLoggingCalls(method, userCtxOrFullPath);
    }

    public void addPathToLiveBlocking(final RestMethodEnum method, final String path, final String ownerUserId) throws ValidationException {
        responseBlockingService.addPathToLiveBlocking(method, path, ownerUserId);
    }

    public void removePathFromLiveBlocking(final RestMethodEnum method, final String path, final String ownerUserId) {
        responseBlockingService.removePathFromLiveBlocking(method, path, ownerUserId);
    }

    public long countLiveBlockingPathsForUser(final RestMethodEnum method, final String path, final String ownerUserId) {
        return responseBlockingService.countLiveBlockingPathsForUser(method, path, ownerUserId);
    }

    public void clearAllPathsFromLiveBlocking() {
        responseBlockingService.clearAllPathsFromLiveBlocking();
    }

    public void clearAllPathsFromLiveBlockingForUser(final String ownerUserId) {
        responseBlockingService.clearAllPathsFromLiveBlockingForUser(ownerUserId);
    }

    void clearState() {
        webSocketService.clearSession();
        proxyService.clearAllSessions();
        mockOrderingCounterService.clearState();
        serverSideEventService.clearState();
    }
}
