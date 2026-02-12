package com.smockin.mockserver.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.smockin.admin.exception.RecordNotFoundException;
import com.smockin.admin.exception.ValidationException;
import com.smockin.admin.persistence.dao.RestfulMockDAO;
import com.smockin.admin.persistence.entity.RestfulMock;
import com.smockin.admin.persistence.entity.RestfulMockStatefulMeta;
import com.smockin.admin.persistence.enums.RestMethodEnum;
import com.smockin.admin.service.SmockinUserService;
import com.smockin.admin.service.utils.UserTokenServiceUtils;
import com.smockin.mockserver.service.dto.RestfulResponseDTO;
import com.smockin.mockserver.service.enums.PatchCommandEnum;
import com.smockin.utils.GeneralUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@Transactional
public class StatefulServiceImpl implements StatefulService {

    private final Logger logger = LoggerFactory.getLogger(StatefulServiceImpl.class);

    /*
        Key: RestfulMock.externalId of stateful parent.
        Value: JSON Data List
    */
    private final Map<String, List<Map<String, Object>>> state = new ConcurrentHashMap<>();

    @Autowired
    private RestfulMockDAO restfulMockDAO;

    @Autowired
    private UserTokenServiceUtils userTokenServiceUtils;

    @Autowired
    private SmockinUserService smockinUserService;

    @Autowired
    private StatefulJsonHandler statefulJsonHandler;

    @Autowired
    private StatefulPatchHandler statefulPatchHandler;

    @Override
    public RestfulResponseDTO process(final HttpServletRequest req, final RestfulMock mock) {

        final RestfulMock parent = loadStatefulParent(mock);

        final String sanitizedInboundPath = GeneralUtils.sanitizeMultiUserPath(smockinUserService.getUserMode(), req.getPathInfo(), mock.getCreatedBy().getCtxPath());

        final List<Map<String, Object>> mockStateContent = loadStateForMock(parent);
        final Map<String, String> pathVars = GeneralUtils.findAllPathVars(sanitizedInboundPath, mock.getPath());
        final String fieldId = parent.getRestfulMockStatefulMeta().getIdFieldName();
        final String dataId = pathVars.get(fieldId);

        StatefulResponse statefulResponse;

        try {

            statefulResponse = switch (RestMethodEnum.findByName(req.getMethod())) {
                case GET -> handleGet(dataId, mockStateContent, parent.getRestfulMockStatefulMeta());
                case POST ->
                        handlePost(parent.getExtId(), GeneralUtils.extractRequestBody(req), mockStateContent, parent.getRestfulMockStatefulMeta());
                case PUT ->
                        handlePut(dataId, parent.getExtId(), GeneralUtils.extractRequestBody(req), mockStateContent, parent.getRestfulMockStatefulMeta());
                case PATCH ->
                        handlePatch(dataId, parent.getExtId(), GeneralUtils.extractRequestBody(req), mockStateContent, parent.getRestfulMockStatefulMeta());
                case DELETE ->
                        handleDelete(dataId, parent.getExtId(), mockStateContent, parent.getRestfulMockStatefulMeta());
                default -> new StatefulResponse(HttpStatus.SC_NOT_FOUND, "Invalid JSON in request body");
            };

        } catch (StatefulValidationException ex) {

            final int status = (ex.getStatus() != null)
                    ? ex.getStatus()
                    : HttpStatus.SC_BAD_REQUEST;

            statefulResponse = (ex.getMessage() != null)
                    ? new StatefulResponse(status, ex.getMessage())
                    : new StatefulResponse(status);
        }

        return new RestfulResponseDTO(statefulResponse.httpResponseCode(),
                ContentType.APPLICATION_JSON.getMimeType(),
                statefulResponse.responseBody());
    }

    @Override
    public void resetState(final String externalId, final String userToken) throws RecordNotFoundException, ValidationException {
        logger.debug("resetState called");

        final RestfulMock restfulMock = restfulMockDAO.findByExtId(externalId);

        if (restfulMock == null) {
            throw new RecordNotFoundException();
        }

        final RestfulMock parent = loadStatefulParent(restfulMock);

        userTokenServiceUtils.validateRecordOwner(parent.getCreatedBy(), userToken);

        state.remove(parent.getExtId());

    }

    StatefulResponse handleGet(final String dataId, final List<Map<String, Object>> currentStateContentForMock, final RestfulMockStatefulMeta restfulMockStatefulMeta) {

        // GET All
        if (dataId == null) {
            return new StatefulResponse(HttpStatus.SC_OK,
                    GeneralUtils.serialiseJson(currentStateContentForMock));
        }

        // GET by ID
        final Optional<Map<String, Object>> stateDataOpt =
                findStatefulDataById(dataId, currentStateContentForMock, restfulMockStatefulMeta);

        if (!stateDataOpt.isPresent()) {
            return new StatefulResponse(HttpStatus.SC_NOT_FOUND);
        }

        return new StatefulResponse(HttpStatus.SC_OK,
                GeneralUtils.serialiseJson(stateDataOpt.get()));
    }

    StatefulResponse handlePost(final String parentExtId, final String requestBody, final List<Map<String, Object>> currentStateContentForMock, final RestfulMockStatefulMeta restfulMockStatefulMeta) {

        // Validate is valid json body
        final Optional<Map<String, Object>> requestDataMapOpt = convertToJsonMap(requestBody);

        if (requestDataMapOpt.isEmpty()) {
            return new StatefulResponse(HttpStatus.SC_BAD_REQUEST,
                    "Invalid JSON in request body");
        }

        final Map<String, Object> requestDataMap = requestDataMapOpt.get();

        // TODO amend id handler here to add id according to the path...
        statefulJsonHandler.appendIdToJson(requestDataMap, restfulMockStatefulMeta);

        final String fieldIdPathPattern = restfulMockStatefulMeta.getIdFieldLocation();

        if (statefulJsonHandler.isComplexJsonStructure(fieldIdPathPattern)) {

            // TODO
            // Amend POST to add items according to path...

            state.put(parentExtId, currentStateContentForMock); // TODO use merge

        } else {

            state.merge(parentExtId, currentStateContentForMock, (currentValue, p) -> {
                currentValue.add(requestDataMap);
                return currentValue;
            });

        }

        return new StatefulResponse(HttpStatus.SC_CREATED);
    }

    StatefulResponse handleDelete(final String dataId,
                                  final String parentExtId,
                                  final List<Map<String, Object>> currentStateContentForMock,
                                  final RestfulMockStatefulMeta restfulMockStatefulMeta) {

        if (dataId == null) {
            return new StatefulResponse(HttpStatus.SC_BAD_REQUEST);
        }

        final String fieldIdPathPattern = restfulMockStatefulMeta.getIdFieldLocation();

        if (statefulJsonHandler.isComplexJsonStructure(fieldIdPathPattern)) {

            final Optional<StatefulJsonHandler.StatefulPath> pathOpt =
                    statefulJsonHandler.findDataStateRecordPath(currentStateContentForMock,
                            StringUtils.split(fieldIdPathPattern, "."),
                            dataId);

            if (pathOpt.isEmpty()) {
                return new StatefulResponse(HttpStatus.SC_NOT_FOUND);
            }

            // Drills down into the path and removes a specific object.
            statefulJsonHandler.removeDataStateRecordByPath(currentStateContentForMock, pathOpt.get().path());

            state.put(parentExtId, currentStateContentForMock); // TODO use merge

        } else {

            final String fieldId = restfulMockStatefulMeta.getIdFieldName();
            final int originalDataStateSize = currentStateContentForMock.size();

            final List<Map<String, Object>> filteredCurrentStateContentForMock
                    = state.merge(parentExtId, currentStateContentForMock, (currentValue, p) ->
                    currentValue
                            .stream()
                            .filter(f -> !(Strings.CS.equals(dataId, (String) f.get(fieldId))))
                            .toList()
            );

            if (filteredCurrentStateContentForMock.size() == originalDataStateSize) {
                return new StatefulResponse(HttpStatus.SC_NOT_FOUND);
            }

        }

        return new StatefulResponse(HttpStatus.SC_NO_CONTENT);
    }

    StatefulResponse handlePut(final String dataId,
                               final String parentExtId,
                               final String requestBody,
                               final List<Map<String, Object>> currentStateContentForMock,
                               final RestfulMockStatefulMeta restfulMockStatefulMeta) {

        if (dataId == null) {
            return new StatefulResponse(HttpStatus.SC_BAD_REQUEST);
        }

        // Ensure the JSON body is valid
        final Optional<Map<String, Object>> requestDataMapOpt = convertToJsonMap(requestBody);

        if (requestDataMapOpt.isEmpty()) {
            return new StatefulResponse(HttpStatus.SC_BAD_REQUEST,
                    "Invalid JSON in request body");
        }

        final String fieldIdPathPattern = restfulMockStatefulMeta.getIdFieldLocation();

        if (statefulJsonHandler.isComplexJsonStructure(fieldIdPathPattern)) {

            final Optional<StatefulJsonHandler.StatefulPath> pathOpt =
                    statefulJsonHandler.findDataStateRecordPath(currentStateContentForMock,
                            StringUtils.split(fieldIdPathPattern, "."),
                            dataId);

            if (pathOpt.isEmpty()) {
                return new StatefulResponse(HttpStatus.SC_NOT_FOUND);
            }

            currentStateContentForMock.remove(pathOpt.get().index().intValue());
            currentStateContentForMock.add(pathOpt.get().index(), requestDataMapOpt.get());

            state.put(parentExtId, currentStateContentForMock); // TODO use merge

        } else {

            final String fieldId = restfulMockStatefulMeta.getIdFieldName();
            final Object bodyId = requestDataMapOpt.get().get(fieldId);

            // Ensure ids in url and body match
            if (!(bodyId instanceof String)
                    || !Strings.CS.equals((String) bodyId, dataId)) {
                return new StatefulResponse(HttpStatus.SC_BAD_REQUEST);
            }

            final AtomicBoolean recordFound = new AtomicBoolean(false);

            state.merge(parentExtId, currentStateContentForMock, (currentValue, p) ->
                    currentValue
                            .stream()
                            .map(m -> {

                                final boolean match = Strings.CS.equals(dataId, (String) m.get(fieldId));

                                if (match) {
                                    recordFound.set(true);
                                }

                                return (match)
                                        ? requestDataMapOpt.get()
                                        : m;
                            })
                            .toList()
            );

            if (!recordFound.get()) {
                return new StatefulResponse(HttpStatus.SC_NOT_FOUND);
            }

        }

        return new StatefulResponse(HttpStatus.SC_NO_CONTENT);
    }

    // https://sookocheff.com/post/api/understanding-json-patch/
    // https://www.baeldung.com/spring-rest-json-patch

    // Valid PATCH operations are add, remove, replace, move, copy and test. Any other operation is considered an error.
    StatefulResponse handlePatch(final String dataId,
                                 final String parentExtId,
                                 final String requestBody,
                                 final List<Map<String, Object>> currentStateContentForMock,
                                 final RestfulMockStatefulMeta restfulMockStatefulMeta) throws StatefulValidationException {

        if (dataId == null) {
            return new StatefulResponse(HttpStatus.SC_BAD_REQUEST);
        }

        // TODO Should only accept following header content type, but will ignore this rule for now...
        // -H "Content-Type: application/json-patch+json"

        final Optional<Map<String, Object>> requestDataMapOpt = convertToJsonMap(requestBody);

        if (requestDataMapOpt.isEmpty()) {
            return new StatefulResponse(HttpStatus.SC_BAD_REQUEST,
                    "Invalid JSON in request body");
        }

        final Map<String, Object> requestDataMap = requestDataMapOpt.get();
        final String op = (String) requestDataMap.get("op");
        final String prefixedPath = (String) requestDataMap.get("path");
        final String prefixedFrom = (String) requestDataMap.get("from");
        final Object value = requestDataMap.get("value");

        if (op == null || prefixedPath == null) {
            return new StatefulResponse(HttpStatus.SC_BAD_REQUEST,
                    "Invalid JSON in request body, required 'op' and 'path' fields are missing");
        }

        final PatchCommandEnum patchCommand;

        try {
            patchCommand = PatchCommandEnum.valueOf(op);
        } catch (IllegalArgumentException ex) {
            return new StatefulResponse(HttpStatus.SC_BAD_REQUEST,
                    String.format(StatefulValidationException.INVALID_PATCH_INSTRUCTION, "'op' is not a valid value"));
        }

        if (!prefixedPath.startsWith(GeneralUtils.URL_PATH_SEPARATOR)) {
            return new StatefulResponse(HttpStatus.SC_BAD_REQUEST,
                    String.format(StatefulValidationException.INVALID_PATCH_INSTRUCTION, "path should begin with '/' (e.g '/age'"));
        }

        final String path = prefixedPath.substring(1);
        final String fieldIdPathPattern = restfulMockStatefulMeta.getIdFieldLocation();

        if (statefulJsonHandler.isComplexJsonStructure(fieldIdPathPattern)) {

            final Optional<StatefulJsonHandler.StatefulPath> pathOpt =
                    statefulJsonHandler.findDataStateRecordPath(currentStateContentForMock,
                            StringUtils.split(fieldIdPathPattern, "."),
                            dataId);

            if (pathOpt.isEmpty()) {
                return new StatefulResponse(HttpStatus.SC_NOT_FOUND);
            }

            final Optional<Map<String, Object>> currentDataOpt = statefulJsonHandler.findDataStateRecordByPath(currentStateContentForMock, pathOpt.get().path());

            if (currentDataOpt.isEmpty()) {
                return new StatefulResponse(HttpStatus.SC_NOT_FOUND);
            }

            // TODO

            state.put(parentExtId, currentStateContentForMock); // TODO use merge

        } else {

            final String fieldId = restfulMockStatefulMeta.getIdFieldName();
            final AtomicBoolean recordFound = new AtomicBoolean(false);

            switch (patchCommand) {
                case ADD:

                    if (value == null) {
                        return new StatefulResponse(HttpStatus.SC_BAD_REQUEST,
                                String.format(StatefulValidationException.INVALID_PATCH_INSTRUCTION, "'value' is required"));
                    }

                    state.merge(parentExtId, currentStateContentForMock, (currentValue, nu) ->
                            currentValue
                                    .stream()
                                    .map(m -> {

                                        final boolean matchOnIdMade = Strings.CS.equals(dataId, (String) m.get(fieldId));

                                        if (matchOnIdMade) {
                                            recordFound.set(true);
                                        }

                                        if (matchOnIdMade) {

                                            statefulPatchHandler.patchAddOperation(path, m, value, false);

                                            return m;
                                        } else {
                                            return m;
                                        }
                                    })
                                    .toList()
                    );

                    break;
                case REMOVE:

                    state.merge(parentExtId, currentStateContentForMock, (currentValue, nu) ->
                            currentValue
                                    .stream()
                                    .map(m -> {

                                        final boolean matchOnIdMade = Strings.CS.equals(dataId, (String) m.get(fieldId));

                                        if (matchOnIdMade) {
                                            recordFound.set(true);
                                        }

                                        if (matchOnIdMade) {

                                            statefulPatchHandler.patchRemoveOperation(path, m);

                                            return m;
                                        } else {
                                            return m;
                                        }
                                    })
                                    .toList()
                    );

                    break;
                case REPLACE:

                    if (value == null) {
                        return new StatefulResponse(HttpStatus.SC_BAD_REQUEST,
                                String.format(StatefulValidationException.INVALID_PATCH_INSTRUCTION, "'value' is required"));
                    }

                    state.merge(parentExtId, currentStateContentForMock, (currentValue, nu) ->
                            currentValue
                                    .stream()
                                    .map(m -> {

                                        final boolean matchOnIdMade = Strings.CS.equals(dataId, (String) m.get(fieldId));

                                        if (matchOnIdMade) {
                                            recordFound.set(true);
                                        }

                                        if (matchOnIdMade) {

                                            statefulPatchHandler.addReplaceOperation(path, m, value);

                                            return m;
                                        } else {
                                            return m;
                                        }
                                    })
                                    .toList()
                    );

                    break;
                case COPY:

                    if (prefixedFrom == null) {
                        return new StatefulResponse(HttpStatus.SC_BAD_REQUEST,
                                String.format(StatefulValidationException.INVALID_PATCH_INSTRUCTION, "'from' is required"));
                    }

                    if (!prefixedFrom.startsWith(GeneralUtils.URL_PATH_SEPARATOR)) {
                        return new StatefulResponse(HttpStatus.SC_BAD_REQUEST,
                                String.format(StatefulValidationException.INVALID_PATCH_INSTRUCTION, "'from' should begin with '/' (e.g '/age'"));
                    }

                    final String fromInCopyOp = prefixedFrom.substring(1);

                    state.merge(parentExtId, currentStateContentForMock, (currentValue, nu) ->
                            currentValue
                                    .stream()
                                    .map(m -> {

                                        final boolean matchOnIdMade = Strings.CS.equals(dataId, (String) m.get(fieldId));

                                        if (matchOnIdMade) {
                                            recordFound.set(true);
                                        }

                                        if (matchOnIdMade) {

                                            statefulPatchHandler.patchCopyOperation(fromInCopyOp, m, path);

                                            return m;
                                        } else {
                                            return m;
                                        }
                                    })
                                    .toList()
                    );

                    break;
                case MOVE:

                    if (prefixedFrom == null) {
                        return new StatefulResponse(HttpStatus.SC_BAD_REQUEST,
                                String.format(StatefulValidationException.INVALID_PATCH_INSTRUCTION, "'from' is required"));
                    }

                    if (!prefixedFrom.startsWith(GeneralUtils.URL_PATH_SEPARATOR)) {
                        return new StatefulResponse(HttpStatus.SC_BAD_REQUEST,
                                String.format(StatefulValidationException.INVALID_PATCH_INSTRUCTION, "'from' should begin with '/' (e.g '/age'"));
                    }

                    final String fromInMoveOp = prefixedFrom.substring(1);


                    state.merge(parentExtId, currentStateContentForMock, (currentValue, nu) ->
                            currentValue
                                    .stream()
                                    .map(m -> {

                                        final boolean matchOnIdMade = Strings.CS.equals(dataId, (String) m.get(fieldId));

                                        if (matchOnIdMade) {
                                            recordFound.set(true);
                                        }

                                        if (matchOnIdMade) {

                                            statefulPatchHandler.patchMoveOperation(fromInMoveOp, m, path);

                                            return m;
                                        } else {
                                            return m;
                                        }
                                    })
                                    .toList()
                    );

                    break;
                case TEST:

                    return new StatefulResponse(HttpStatus.SC_NOT_IMPLEMENTED, "PATCH 'TEST' operation is not supported");
                default:

                    return new StatefulResponse(HttpStatus.SC_NOT_IMPLEMENTED, "PATCH operation is not supported");
            }


            if (!recordFound.get()) {
                return new StatefulResponse(HttpStatus.SC_NOT_FOUND);
            }

        }

        return new StatefulResponse(HttpStatus.SC_NO_CONTENT);
    }


    Optional<Map<String, Object>> findStatefulDataById(final String id,
                                                       final List<Map<String, Object>> currentStateContent,
                                                       final RestfulMockStatefulMeta restfulMockStatefulMeta) {


        final String fieldIdPathPattern = restfulMockStatefulMeta.getIdFieldLocation();

        if (statefulJsonHandler.isComplexJsonStructure(fieldIdPathPattern)) {

            return statefulJsonHandler.findDataStateRecord(currentStateContent, fieldIdPathPattern, id);

        } else {

            final String fieldId = restfulMockStatefulMeta.getIdFieldName();

            return currentStateContent
                    .stream()
                    .filter(f -> (Strings.CS.equals(id, String.valueOf(f.get(fieldId)))))
                    .findFirst();

        }

    }


    List<Map<String, Object>> loadStateForMock(final RestfulMock parent) {

        return state.computeIfAbsent(parent.getExtId(), k -> {

            final String initialBody = parent.getRestfulMockStatefulMeta().getInitialResponseBody();

            return (initialBody != null)
                    ? GeneralUtils.deserializeJson(initialBody, new TypeReference<>() {
            })
                    : new ArrayList<>();
        });

    }

    RestfulMock loadStatefulParent(final RestfulMock mock) {

        return (mock.getStatefulParent() != null)
                ? mock.getStatefulParent()
                : mock;
    }

    Optional<Map<String, Object>> convertToJsonMap(final String json) {

        try {
            final Map<String, Object> dataMap = (Map<String, Object>) GeneralUtils.deserialiseJSONToMap(json, false);
            return Optional.of(dataMap);
        } catch (Throwable ex) {
            return Optional.empty();
        }

    }

}
