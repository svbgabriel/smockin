package com.smockin.admin.service.utils;

import com.smockin.admin.dto.MockImportConfigDTO;
import com.smockin.admin.dto.RestfulMockDTO;
import com.smockin.admin.dto.response.RestfulMockResponseDTO;
import com.smockin.admin.enums.UserModeEnum;
import com.smockin.admin.exception.ValidationException;
import com.smockin.admin.persistence.dao.RestfulMockDAO;
import com.smockin.admin.persistence.dao.SmockinUserDAO;
import com.smockin.admin.persistence.entity.RestfulMock;
import com.smockin.admin.persistence.entity.RestfulMockJavaScriptHandler;
import com.smockin.admin.persistence.entity.RestfulMockStatefulMeta;
import com.smockin.admin.persistence.entity.SmockinUser;
import com.smockin.admin.persistence.enums.RestMethodEnum;
import com.smockin.admin.persistence.enums.RestMockTypeEnum;
import com.smockin.admin.service.ProjectService;
import com.smockin.admin.service.SmockinUserService;
import com.smockin.admin.service.mapper.RestfulMockMapper;
import com.smockin.mockserver.engine.MockedRestServerEngine;
import com.smockin.mockserver.service.MockOrderingCounterService;
import com.smockin.utils.GeneralUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by mgallina.
 */
@Component
public class RestfulMockServiceUtils {

    @Autowired
    private RestfulMockDAO restfulMockDAO;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private RestfulMockSortingUtils restfulMockSortingUtils;

    @Autowired
    private MockedRestServerEngine mockedRestServerEngine;

    @Autowired
    private MockOrderingCounterService mockOrderingCounterService;

    @Autowired
    private SmockinUserService smockinUserService;

    @Autowired
    private SmockinUserDAO smockinUserDAO;

    @Autowired
    private RestfulMockMapper restfulMockMapper;

    @Autowired
    private PathVariableUtils pathVariableUtils;


    @Transactional
    public List<RestfulMockResponseDTO> buildRestfulMockDefinitionDTOs(final List<RestfulMock> restfulMockDefinitions) {

        return restfulMockDefinitions
                .stream()
                .map(this::buildRestfulMockDefinitionDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public RestfulMockResponseDTO buildRestfulMockDefinitionDTO(final RestfulMock rmd) {

        final RestfulMockResponseDTO dto = restfulMockMapper.toRestfulMockResponseDTO(rmd);

        dto.setPath(pathVariableUtils.formatOutboundPathVarArgs(rmd.getPath()));

        return dto;
    }

    public void handleCustomJsSyntax(final RestfulMockDTO dto, final RestfulMock mock) throws ValidationException {

        if (!RestMockTypeEnum.CUSTOM_JS.equals(dto.getMockType())) {
            mock.setJavaScriptHandler(null);
            return;
        }

        if (StringUtils.isBlank(dto.getCustomJsSyntax())) {
            throw new ValidationException("Missing javaScript logic");
        }

        final RestfulMockJavaScriptHandler javaScriptHandler = new RestfulMockJavaScriptHandler();
        javaScriptHandler.setRestfulMock(mock);
        javaScriptHandler.setSyntax(dto.getCustomJsSyntax());

        mock.setJavaScriptHandler(javaScriptHandler);
    }

    public RestfulMock handleCreateStatefulMockType(final RestfulMockDTO dto, RestfulMock mainMock, final SmockinUser smockinUser) {

        if (!RestMockTypeEnum.STATEFUL.equals(dto.getMockType())) {
            return mainMock;
        }

        applyRestfulMockStatefulMeta(dto, mainMock);

        mainMock.setMethod(RestMethodEnum.GET);
        mainMock = restfulMockDAO.save(mainMock);

        createStatefulChildMocks(dto, mainMock, smockinUser);

        return mainMock;
    }

    public void handleDeleteStatefulMock(final RestfulMock mock) {

        if (!RestMockTypeEnum.STATEFUL.equals(mock.getMockType())) {
            return;
        }

        final RestfulMock parent = loadStatefulParent(mock);

        parent.getStatefulChildren().clear();
        restfulMockDAO.saveAndFlush(parent);
    }

    @Transactional
    public void handleEndpointOrdering() {

        // Load all restful mocks
        final List<RestfulMock> allRestfulMocks = restfulMockDAO.findAll();

        // Alphanumerically order the mocks by endpoint path. This also updates the initializationOrder field of each record.
        restfulMockSortingUtils.autoOrderEndpointPaths(allRestfulMocks);

        // Save all
        restfulMockDAO.saveAll(allRestfulMocks);
    }

    public void amendPath(final RestfulMockDTO dto) {
        dto.setPath(GeneralUtils.prefixPath(dto.getPath()));
    }

    public void preHandleExistingEndpoints(final RestfulMockDTO dto,
                                           final MockImportConfigDTO apiImportConfig,
                                           final SmockinUser user,
                                           final String conflictCtxPath) {

        final RestfulMock existingRestFulMock = restfulMockDAO.findByPathAndMethodAndUser(dto.getPath(), dto.getMethod(), user);

        if (existingRestFulMock == null) {
            return;
        }

        if (!apiImportConfig.isKeepExisting()) {
            restfulMockDAO.delete(existingRestFulMock);
            restfulMockDAO.flush();
            return;
        }

        switch (apiImportConfig.getKeepStrategy()) {
            case RENAME_EXISTING:
                existingRestFulMock.setPath(GeneralUtils.URL_PATH_SEPARATOR + conflictCtxPath + existingRestFulMock.getPath());
                restfulMockDAO.save(existingRestFulMock);
                break;
            case RENAME_NEW:
                dto.setPath(GeneralUtils.URL_PATH_SEPARATOR + conflictCtxPath + dto.getPath());
                break;
        }

    }

    void updateExistingStatefulMockTypeFields(final RestfulMockDTO dto, final RestfulMock mock) {

        final RestMethodEnum method = mock.getMethod();
        final String originalPath = dto.getPath();
        final boolean isParent = (mock.getStatefulParent() == null);

        applyRestfulMockStatefulMeta(dto, mock);

        final String idFieldName = (!isParent)
                ? mock.getStatefulParent().getRestfulMockStatefulMeta().getIdFieldName()
                : null; // if not the parent then we don't need the idFieldName anyway
        final String varPath = dto.getPath() + "/:" + idFieldName;

        if (!isParent
                && (RestMethodEnum.GET.equals(method)
                || RestMethodEnum.PUT.equals(method)
                || RestMethodEnum.PATCH.equals(method)
                || RestMethodEnum.DELETE.equals(method))) {
            mock.setPath(pathVariableUtils.formatInboundPathVarArgs(varPath));
        } else {
            mock.setPath(pathVariableUtils.formatInboundPathVarArgs(originalPath));
        }

        mock.setStatus(dto.getStatus());

        restfulMockDAO.save(mock);
    }

    public void createStatefulChildMocks(final RestfulMockDTO dto, final RestfulMock mainMock, final SmockinUser smockinUser) {

        final String originalPath = dto.getPath();
        final String idFieldName = mainMock.getRestfulMockStatefulMeta().getIdFieldName();
        final String varPath = dto.getPath() + "/:" + idFieldName;

        for (RestMethodEnum method : RestMethodEnum.values()) {

            if (RestMethodEnum.GET.equals(method)
                    || RestMethodEnum.PUT.equals(method)
                    || RestMethodEnum.PATCH.equals(method)
                    || RestMethodEnum.DELETE.equals(method)) {
                dto.setPath(varPath);
            } else if (!RestMethodEnum.HEAD.equals(method)) {
                dto.setPath(originalPath);
            }

            final RestfulMock mock = buildRestfulMock(dto, smockinUser);
            mock.setMethod(method);
            mock.setStatefulParent(mainMock);
            mock.setRestfulMockStatefulMeta(null); // only set this in the parent

            restfulMockDAO.save(mock);
        }

    }

    RestfulMock loadStatefulParent(final RestfulMock mock) {

        return (mock.getStatefulParent() != null)
                ? mock.getStatefulParent()
                : mock;
    }

    public void handleExistingStatefulMockUpdate(final RestfulMockDTO dto, final RestfulMock mock) throws ValidationException {

        final boolean mockTypeChanged = (!mock.getMockType().equals(dto.getMockType()));

        final RestfulMock parent = loadStatefulParent(mock);

        if (mockTypeChanged) {

            parent.getStatefulChildren().clear();
            restfulMockDAO.saveAndFlush(parent);

            handleMockFieldsUpdate(dto, parent);

        } else {

            updateExistingStatefulMockTypeFields(dto, parent);

            parent.getStatefulChildren()
                    .stream()
                    .forEach(c ->
                            updateExistingStatefulMockTypeFields(dto, c));

        }

    }

    public RestfulMock buildRestfulMock(final RestfulMockDTO dto, final SmockinUser smockinUser) {

        final RestfulMock mock = restfulMockMapper.toRestfulMock(dto, smockinUser);

        mock.setPath(pathVariableUtils.formatInboundPathVarArgs(dto.getPath()));

        return mock;
    }

    public void handleMockFieldsUpdate(final RestfulMockDTO dto, final RestfulMock mock)
            throws ValidationException {

        mock.getDefinitions().clear();
        mock.getRules().clear();
        restfulMockDAO.saveAndFlush(mock);

        mock.setMockType(dto.getMockType());
        mock.setPath(pathVariableUtils.formatInboundPathVarArgs(dto.getPath()));
        mock.setMethod(dto.getMethod());
        mock.setStatus(dto.getStatus());
        mock.setProxyTimeOutInMillis(dto.getProxyTimeoutInMillis());
        mock.setWebSocketTimeoutInMillis(dto.getWebSocketTimeoutInMillis());
        mock.setSseHeartBeatInMillis(dto.getSseHeartBeatInMillis());
        mock.setProxyPushIdOnConnect(dto.isProxyPushIdOnConnect());
        mock.setRandomiseDefinitions(dto.isRandomiseDefinitions());
        mock.setProxyForwardWhenNoRuleMatch(dto.isProxyForwardWhenNoRuleMatch());
        mock.setLastUpdated(GeneralUtils.getCurrentDate()); // force update to lastUpdated, as changes to child records do not otherwise change this
        mock.setRandomiseLatency(dto.isRandomiseLatency());
        mock.setRandomiseLatencyRangeMinMillis(dto.getRandomiseLatencyRangeMinMillis());
        mock.setRandomiseLatencyRangeMaxMillis(dto.getRandomiseLatencyRangeMaxMillis());

        applyRestfulMockStatefulMeta(dto, mock);

        if (dto.getProjectId() != null)
            mock.setProject(projectService.loadByExtId(dto.getProjectId()));

        handleCustomJsSyntax(dto, mock);

        restfulMockMapper.populateEndpointDefinitionsAndRules(dto, mock);

        restfulMockDAO.save(mock);

        if (RestMockTypeEnum.SEQ.equals(mock.getMockType())) {
            mockOrderingCounterService.clearMockStateById(mock.getExtId());
        }

    }

    private void applyRestfulMockStatefulMeta(final RestfulMockDTO dto, final RestfulMock mock) {

        if (!RestMockTypeEnum.STATEFUL.equals(mock.getMockType())
                || mock.getStatefulParent() != null) {
            mock.setRestfulMockStatefulMeta(null);
            return;
        }

        final RestfulMockStatefulMeta restfulMockStatefulMeta = (mock.getRestfulMockStatefulMeta() != null)
                ? mock.getRestfulMockStatefulMeta()
                : new RestfulMockStatefulMeta();

        restfulMockStatefulMeta.setIdFieldName(dto.getStatefulIdFieldName());
        restfulMockStatefulMeta.setInitialResponseBody(formatInitialStateBody(dto.getStatefulDefaultResponseBody()));
        restfulMockStatefulMeta.setIdFieldLocation(dto.getStatefulIdFieldLocation());
        restfulMockStatefulMeta.setRestfulMock(mock);

        mock.setRestfulMockStatefulMeta(restfulMockStatefulMeta);
    }

    String formatInitialStateBody(String initialBody) {

        initialBody = StringUtils.trim(initialBody);

        if (initialBody.startsWith("{")
                && initialBody.endsWith("}")) {
            return "[" + initialBody  + "]";
        }

        return initialBody;
    }

    @Transactional
    public void validateMockPathDoesNotStartWithUsername(final String path) throws ValidationException {

        if (UserModeEnum.INACTIVE.equals(smockinUserService.getUserMode())) {
            return;
        }

        // As long as path is not null this will always return at least 1 element.
        final String[] pathSegments = StringUtils.split(path, GeneralUtils.URL_PATH_SEPARATOR);

        if (smockinUserDAO.existsSmockinUserByUsername(pathSegments[0])) {
            throw new ValidationException("A mock cannot begin with an existing user's username");
        }

    }

}
