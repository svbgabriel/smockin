package com.smockin.mockserver.engine;

import com.smockin.admin.dto.HttpClientCallDTO;
import com.smockin.admin.dto.response.HttpClientResponseDTO;
import com.smockin.admin.persistence.dao.RestfulMockDAO;
import com.smockin.admin.persistence.dao.SmockinUserDAO;
import com.smockin.admin.persistence.entity.RestfulMock;
import com.smockin.admin.persistence.entity.RestfulMockDefinitionOrder;
import com.smockin.admin.persistence.entity.RestfulMockDefinitionRule;
import com.smockin.admin.persistence.enums.ProxyModeTypeEnum;
import com.smockin.admin.persistence.enums.RestMethodEnum;
import com.smockin.admin.persistence.enums.RestMockTypeEnum;
import com.smockin.admin.service.HttpClientService;
import com.smockin.admin.service.utils.MultiUserUtils;
import com.smockin.mockserver.dto.ProxyForwardConfigCacheDTO;
import com.smockin.mockserver.dto.ProxyForwardMappingDTO;
import com.smockin.mockserver.exception.InboundParamMatchException;
import com.smockin.mockserver.service.*;
import com.smockin.mockserver.service.dto.RestfulResponseDTO;
import com.smockin.utils.GeneralUtils;
import com.smockin.utils.MockUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.*;

/**
 * Created by mgallina.
 */
@Service
@Transactional(readOnly = true)
public class MockedRestServerEngineUtils {

    private final Logger logger = LoggerFactory.getLogger(MockedRestServerEngineUtils.class);

    private final RestfulMockDAO restfulMockDAO;
    private final MockOrderingCounterService mockOrderingCounterService;
    private final RuleEngine ruleEngine;
    private final HttpProxyService proxyService;
    private final JavaScriptResponseHandler javaScriptResponseHandler;
    private final InboundParamMatchService inboundParamMatchService;
    private final ServerSideEventService serverSideEventService;
    private final StatefulService statefulService;
    private final ObjectProvider<HttpClientService> httpClientServiceProvider;
    private final SmockinUserDAO smockinUserDAO;
    private final ProxyMappingCache proxyMappingCache;
    private final MultiUserUtils multiUserUtils;

    @Autowired
    public MockedRestServerEngineUtils(RestfulMockDAO restfulMockDAO,
                                       MockOrderingCounterService mockOrderingCounterService,
                                       RuleEngine ruleEngine,
                                       HttpProxyService proxyService,
                                       JavaScriptResponseHandler javaScriptResponseHandler,
                                       InboundParamMatchService inboundParamMatchService,
                                       ServerSideEventService serverSideEventService,
                                       StatefulService statefulService,
                                       ObjectProvider<HttpClientService> httpClientServiceProvider,
                                       SmockinUserDAO smockinUserDAO,
                                       ProxyMappingCache proxyMappingCache,
                                       MultiUserUtils multiUserUtils) {
        this.restfulMockDAO = restfulMockDAO;
        this.mockOrderingCounterService = mockOrderingCounterService;
        this.ruleEngine = ruleEngine;
        this.proxyService = proxyService;
        this.javaScriptResponseHandler = javaScriptResponseHandler;
        this.inboundParamMatchService = inboundParamMatchService;
        this.serverSideEventService = serverSideEventService;
        this.statefulService = statefulService;
        this.httpClientServiceProvider = httpClientServiceProvider;
        this.smockinUserDAO = smockinUserDAO;
        this.proxyMappingCache = proxyMappingCache;
        this.multiUserUtils = multiUserUtils;
    }

    public Optional<String> loadMockedResponse(final HttpServletRequest request,
                                               final HttpServletResponse response,
                                               final boolean isMultiUserMode,
                                               final boolean isProxyMode) {

        debugInboundRequest(request);

        return (isProxyMode)
                ? handleProxyInterceptorMode(isMultiUserMode, request, response)
                : handleMockLookup(request, response, isMultiUserMode, false);
    }

    Optional<String> handleMockLookup(final HttpServletRequest request,
                                      final HttpServletResponse response,
                                      final boolean isMultiUserMode,
                                      final boolean ignore404MockResponses) {

        try {
            RestMethodEnum method = RestMethodEnum.findByName(request.getMethod());

            if (RestMethodEnum.HEAD.equals(method)) {
                method = RestMethodEnum.GET;
            }

            final List<RestMockTypeEnum> types = Arrays.asList(
                    RestMockTypeEnum.PROXY_SSE, RestMockTypeEnum.PROXY_HTTP,
                    RestMockTypeEnum.SEQ, RestMockTypeEnum.RULE,
                    RestMockTypeEnum.STATEFUL, RestMockTypeEnum.CUSTOM_JS);

            final RestfulMock mock = (isMultiUserMode)
                    ? restfulMockDAO.findActiveByMethodAndPathPatternAndTypesForMultiUser(method, request.getPathInfo(), types)
                    : restfulMockDAO.findActiveByMethodAndPathPatternAndTypesForSingleUser(method, request.getPathInfo(), types);

            if (mock == null) {
                return Optional.empty();
            }

            debugLoadedMock(mock);

            if (RestMockTypeEnum.PROXY_SSE.equals(mock.getMockType())) {
                return Optional.of(processSSERequest(mock, request, response));
            }

            removeSuspendedResponses(mock);

            final String responseBody = processRequest(mock, request, response, ignore404MockResponses);

            return (responseBody != null) ? Optional.of(responseBody) : Optional.empty();

        } catch (Exception ex) {
            return handleFailure(ex, response);
        }
    }

    private String amendPathForMultiUser(final HttpServletRequest request, final boolean isMultiUserMode) {
        String inboundPath = request.getPathInfo();
        if (isMultiUserMode) {
            final String userCtxPathSegment = multiUserUtils.extractMultiUserCtxPathSegment(inboundPath);
            if (multiUserUtils.isInboundPathMultiUserPath(userCtxPathSegment)) {
                return Strings.CS.remove(inboundPath, GeneralUtils.URL_PATH_SEPARATOR + userCtxPathSegment);
            }
        }
        return inboundPath;
    }

    Optional<String> handleProxyInterceptorMode(final boolean isMultiUserMode,
                                                final HttpServletRequest request,
                                                final HttpServletResponse response) {
        try {
            final String amendedInboundPath = amendPathForMultiUser(request, isMultiUserMode);
            final String inboundPath = request.getPathInfo();
            final String userCtxPath = (!Strings.CS.equals(inboundPath, amendedInboundPath))
                    ? multiUserUtils.extractMultiUserCtxPathSegment(inboundPath) : "";

            final Optional<ProxyForwardConfigCacheDTO> configOpt = proxyMappingCache.find(userCtxPath);
            String proxyDownstreamURL = configOpt.map(c -> lookUpProxyMappingDownstreamUrl(amendedInboundPath, c.getProxyForwardMappings())).orElse(null);

            if (proxyDownstreamURL == null && configOpt.isPresent()) {
                proxyDownstreamURL = lookUpDefaultProxyMappingDownstreamUrl(configOpt.get().getProxyForwardMappings());
            }

            if (proxyDownstreamURL == null) {
                return handleMockLookup(request, response, isMultiUserMode, false);
            }

            if (ProxyModeTypeEnum.ACTIVE.equals(configOpt.get().getProxyModeType())) {
                final Optional<String> result = handleMockLookup(request, response, isMultiUserMode, !configOpt.get().isDoNotForwardWhen404Mock());
                if (result.isPresent()) return result;

                return handleClientDownstreamProxyCallResponse(
                        executeClientDownstreamProxyCall(amendedInboundPath, request, proxyDownstreamURL),
                        response, proxyDownstreamURL);
            }

            final Optional<HttpClientResponseDTO> httpClientResponse = executeClientDownstreamProxyCall(amendedInboundPath, request, proxyDownstreamURL);
            if (httpClientResponse.isPresent() && HttpStatus.NOT_FOUND.value() == httpClientResponse.get().getStatus()) {
                return handleMockLookup(request, response, isMultiUserMode, false);
            }

            return handleClientDownstreamProxyCallResponse(httpClientResponse, response, proxyDownstreamURL);
        } catch (Exception ex) {
            return handleFailure(ex, response);
        }
    }

    Optional<HttpClientResponseDTO> executeClientDownstreamProxyCall(final String inboundPath,
                                                                     final HttpServletRequest request,
                                                                     final String proxyDownstreamURL) {
        if (proxyDownstreamURL == null) return Optional.empty();

        final HttpClientCallDTO httpClientCallDTO = new HttpClientCallDTO();
        final String reqParams = (request.getQueryString() != null) ? ("?" + request.getQueryString()) : "";

        httpClientCallDTO.setUrl(proxyDownstreamURL + inboundPath + reqParams);
        httpClientCallDTO.setMethod(RestMethodEnum.valueOf(request.getMethod()));
        httpClientCallDTO.setBody(GeneralUtils.extractRequestBody(request));

        Map<String, String> headers = new HashMap<>();
        Collections.list(request.getHeaderNames()).forEach(h -> headers.put(h, request.getHeader(h)));
        httpClientCallDTO.setHeaders(headers);
        httpClientCallDTO.getHeaders().put(HttpHeaders.HOST, sanitizeHost(proxyDownstreamURL));

        try {
            return Optional.of(httpClientServiceProvider.getObject().handleExternalCall(httpClientCallDTO));
        } catch (Throwable ex) {
            logger.error("Error making proxy downstream call: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    String sanitizeHost(final String proxyDownstreamURL) {
        String host = Strings.CS.remove(proxyDownstreamURL, HttpClientService.HTTPS_PROTOCOL);
        host = Strings.CS.remove(host, HttpClientService.HTTP_PROTOCOL);
        return StringUtils.removeAll(host, GeneralUtils.URL_PATH_SEPARATOR);
    }

    Optional<String> handleClientDownstreamProxyCallResponse(final Optional<HttpClientResponseDTO> httpClientResponseOpt,
                                                             final HttpServletResponse response,
                                                             final String proxyDownstreamURL) {
        if (httpClientResponseOpt.isEmpty()) return Optional.empty();

        final HttpClientResponseDTO httpClientResponse = httpClientResponseOpt.get();
        response.setStatus(httpClientResponse.getStatus());
        response.setContentType(httpClientResponse.getContentType());
        applyHeadersToResponse(httpClientResponse.getHeaders(), response);
        response.addHeader(GeneralUtils.PROXIED_DOWNSTREAM_URL_HEADER, proxyDownstreamURL);

        return Optional.of(StringUtils.defaultIfBlank(httpClientResponse.getBody(), ""));
    }

    String processRequest(final RestfulMock mock,
                          final HttpServletRequest req,
                          final HttpServletResponse res,
                          final boolean ignore404MockResponses) {
        RestfulResponseDTO outcome;
        switch (mock.getMockType()) {
            case RULE -> outcome = ruleEngine.process(req, mock.getRules());
            case PROXY_HTTP -> outcome = proxyService.waitForResponse(req.getPathInfo(), mock);
            case CUSTOM_JS -> outcome = javaScriptResponseHandler.executeUserResponse(req, mock);
            case STATEFUL -> outcome = statefulService.process(req, mock);
            default -> outcome = mockOrderingCounterService.process(mock);
        }

        if (outcome == null) {
            outcome = getDefault(mock);
        } else if (ignore404MockResponses && HttpStatus.NOT_FOUND.value() == outcome.getHttpStatusCode()) {
            return null;
        }

        debugOutcome(outcome);
        res.setStatus(outcome.getHttpStatusCode());
        res.setContentType(outcome.getResponseContentType());
        applyHeadersToResponse(outcome.getHeaders(), res);

        try {
            String response = inboundParamMatchService.enrichWithInboundParamMatches(req, mock.getPath(), outcome.getResponseBody(), mock.getCreatedBy().getCtxPath(), mock.getCreatedBy().getId());
            handleLatency(mock);
            return StringUtils.defaultIfBlank(response, "");
        } catch (InboundParamMatchException e) {
            res.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            return e.getMessage();
        }
    }

    RestfulResponseDTO getDefault(final RestfulMock restfulMock) {
        if (RestMockTypeEnum.PROXY_HTTP.equals(restfulMock.getMockType())) {
            return new RestfulResponseDTO(HttpStatus.NOT_FOUND.value());
        }
        final RestfulMockDefinitionOrder mockDefOrder = restfulMock.getDefinitions().getFirst();
        return new RestfulResponseDTO(mockDefOrder.getHttpStatusCode(), mockDefOrder.getResponseContentType(), mockDefOrder.getResponseBody(), mockDefOrder.getResponseHeaders().entrySet());
    }

    void removeSuspendedResponses(final RestfulMock mock) {
        mock.getDefinitions().removeIf(RestfulMockDefinitionOrder::isSuspend);
        mock.getRules().removeIf(RestfulMockDefinitionRule::isSuspend);
    }

    String processSSERequest(final RestfulMock mock, final HttpServletRequest req, final HttpServletResponse res) {
        try {
            serverSideEventService.register(MockUtils.buildUserPath(mock), mock.getSseHeartBeatInMillis(), mock.isProxyPushIdOnConnect(), req, res);
        } catch (IOException e) {
            logger.error("Error registering SSE client", e);
        }
        return "";
    }

    private void handleLatency(final RestfulMock mock) {
        if (!mock.isRandomiseLatency()) return;
        long min = (mock.getRandomiseLatencyRangeMinMillis() > 0) ? mock.getRandomiseLatencyRangeMinMillis() : 1000;
        long max = (mock.getRandomiseLatencyRangeMaxMillis() > 0) ? mock.getRandomiseLatencyRangeMaxMillis() : 5000;
        try {
            Thread.sleep(RandomUtils.nextLong(min, (max + 1)));
        } catch (InterruptedException ex) {
            logger.error("Latency interrupted", ex);
        }
    }

    Optional<String> handleFailure(final Exception ex, final HttpServletResponse response) {
        logger.error("Error processing mock", ex);
        response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        String msg = (ex instanceof IllegalArgumentException) ? ex.getMessage() : "Oops, looks like something went wrong!";
        try {
            response.getWriter().write(msg);
        } catch (IOException ignored) {
            // Not needed
        }
        return Optional.of("Oops");
    }

    String lookUpDefaultProxyMappingDownstreamUrl(final List<ProxyForwardMappingDTO> mappings) {
        return lookUpProxyMappingDownstreamUrl(GeneralUtils.PATH_WILDCARD, mappings);
    }

    String lookUpProxyMappingDownstreamUrl(final String path, final List<ProxyForwardMappingDTO> mappings) {
        return mappings.stream()
                .filter(p -> (Strings.CS.endsWith(p.getPath(), GeneralUtils.PATH_WILDCARD) && Strings.CS.startsWith(path, Strings.CS.removeEnd(p.getPath(), GeneralUtils.PATH_WILDCARD))) || Strings.CS.equals(path, p.getPath()))
                .map(ProxyForwardMappingDTO::getProxyForwardUrl)
                .findFirst().orElse(null);
    }

    void applyHeadersToResponse(final Map<String, String> headers, final HttpServletResponse response) {
        headers.forEach(response::addHeader);
    }

    public Map<String, String> extractResponseHeadersAsMap(final HttpServletResponse response) {
        Map<String, String> map = new HashMap<>();
        response.getHeaderNames().forEach(h -> map.put(h, response.getHeader(h)));
        return map;
    }

    private void debugInboundRequest(final HttpServletRequest request) {
        logger.debug("URL: {}", request.getRequestURL());
        logger.debug("Method: {}", request.getMethod());
        logger.debug("Path: {}", request.getPathInfo());
    }

    private void debugLoadedMock(final RestfulMock mock) {
        logger.debug("Mock: {} ({})", mock.getExtId(), mock.getMockType());
    }

    private void debugOutcome(final RestfulResponseDTO outcome) {
        logger.debug("Status: {}", outcome.getHttpStatusCode());
    }
}
