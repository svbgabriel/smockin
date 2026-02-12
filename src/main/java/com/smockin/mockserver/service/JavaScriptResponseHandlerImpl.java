package com.smockin.mockserver.service;

import com.smockin.admin.dto.UserKeyValueDataDTO;
import com.smockin.admin.persistence.entity.RestfulMock;
import com.smockin.admin.service.SmockinUserService;
import com.smockin.admin.service.UserKeyValueDataService;
import com.smockin.mockserver.service.dto.RestfulResponseDTO;
import com.smockin.utils.GeneralUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.RegExUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.script.*;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Service
@Transactional
public class JavaScriptResponseHandlerImpl implements JavaScriptResponseHandler {

    private final Logger logger = LoggerFactory.getLogger(JavaScriptResponseHandlerImpl.class);

    @Autowired
    private SmockinUserService smockinUserService;

    @Autowired
    private UserKeyValueDataService userKeyValueDataService;

    private static final String CARRIAGE_RETURN_REGEX = "\\r\\n|\\r|\\n";

    @Override
    public RestfulResponseDTO executeUserResponse(final HttpServletRequest req, final RestfulMock mock) {
        logger.debug("executeUserResponse called");

        Object engineResponse;

        try {

            engineResponse = executeJS(
                    defaultRequestObject
                            + populateRequestObjectWithInbound(req, mock.getPath(), mock.getCreatedBy().getCtxPath())
                            + populateKVPs(req, mock)
                            + keyValuePairFindFunc
                            + defaultResponseObject
                            + userResponseFunctionInvoker
                            + mock.getJavaScriptHandler().getSyntax());

        } catch (ScriptException ex) {
            return new RestfulResponseDTO(500, "text/plain", "Looks like there is an issue with the Javascript driving this mock " + ex.getMessage());
        }

        if (!(engineResponse instanceof Map)) {
            return new RestfulResponseDTO(500, "text/plain", "Looks like there is an issue with the Javascript driving this mock!");
        }

        final Map<String, Object> response = (Map<String, Object>) engineResponse;

        return new RestfulResponseDTO(
                (int) response.get("status"),
                (String) response.get("contentType"),
                (String) response.get("body"),
                convertResponseHeaders(response));
    }

    Object executeJS(final String js) throws ScriptException {
        logger.debug(js);
        return buildEngine().eval(js);
    }

    String populateRequestObjectWithInbound(final HttpServletRequest req, final String mockPath, final String ctxPath) {

        final Map<String, String> reqHeaders = new HashMap<>();
        Collections.list(req.getHeaderNames()).forEach(h -> reqHeaders.put(h, req.getHeader(h)));

        final StringBuilder reqObject = new StringBuilder();

        reqObject.append("request.path=")
                .append("'").append(req.getPathInfo()).append("'")
                .append("; ");

        final String body = GeneralUtils.extractRequestBody(req);
        if (StringUtils.isNotBlank(body)) {
            reqObject.append("request.body=")
                    .append("'").append(removeLineBreaks(body)).append("'")
                    .append(";");
        }

        final String sanitizedInboundPath = GeneralUtils.sanitizeMultiUserPath(smockinUserService.getUserMode(), req.getPathInfo(), ctxPath);
        applyMapValuesToStringBuilder("request.pathVars", GeneralUtils.findAllPathVars(sanitizedInboundPath, mockPath), reqObject);
        applyMapValuesToStringBuilder("request.parameters", GeneralUtils.extractAllRequestParams(req), reqObject);
        applyMapValuesToStringBuilder("request.headers", reqHeaders, reqObject);

        return reqObject.toString();
    }

    void applyMapValuesToStringBuilder(final String field, final Map<String, String> values, final StringBuilder reqObject) {

        if (values == null || values.isEmpty()) {
            return;
        }

        values.forEach((key, value) -> reqObject.append(" ")
                .append(field)
                .append("['").append(key).append("']")
                .append("=")
                .append("'").append(value).append("'")
                .append(";"));
    }

    Set<Map.Entry<String, String>> convertResponseHeaders(final Map<String, Object> response) {

        final Object headersJS = response.get("headers");
        final Map<String, String> responseHeaders = new HashMap<>();

        if (headersJS instanceof Map) {
            ((Map<String, Object>) headersJS).forEach((key, value) -> responseHeaders.put(key, (String) value));
        }

        return responseHeaders.entrySet();
    }

    String populateKVPs(final HttpServletRequest req, final RestfulMock mock) throws ScriptException {

        final String handleResponseFunc = GeneralUtils.removeJsComments(mock.getJavaScriptHandler().getSyntax());
        final long mockOwnerUserId = mock.getCreatedBy().getId();

        final int MAX_PASSES = 500;
        int currentPos = 0;
        final String keyValuePairFuncPrefix = keyValuePairFindFuncName + "(";

        final Map<String, String> kvps = new HashMap<>();

        for (int i=0; i < MAX_PASSES; i++) {

            final int startPos = Strings.CS.indexOf(handleResponseFunc, keyValuePairFuncPrefix, currentPos);

            if (startPos == -1) {
                break;
            }

            final int closingParenthesisPos = Strings.CS.indexOf(handleResponseFunc, ")", startPos);
            final String sanitizedKey = findKvpKey(startPos, closingParenthesisPos, req, mock, keyValuePairFuncPrefix, handleResponseFunc);

            if (sanitizedKey != null) {
                final UserKeyValueDataDTO userKeyValueDataDTO = userKeyValueDataService.loadByKey(sanitizedKey, mockOwnerUserId);
                kvps.put(sanitizedKey, (userKeyValueDataDTO != null) ? userKeyValueDataDTO.getValue() : "");
            }

            currentPos = closingParenthesisPos;
        }

        if (!kvps.isEmpty()) {
            return defaultKeyValuePairStoreObjectStart + GeneralUtils.serialiseJson(kvps) + ";";
        }

        return defaultKeyValuePairStoreObject;
    }

    private String findKvpKey(final int startPos, final int closingParenthesisPos, final HttpServletRequest req, final RestfulMock mock, final String keyValuePairFuncPrefix, final String handleResponseFunc)
            throws ScriptException {

        final String invalidMsgPrefix = "Invalid lookUpKvp(...) syntax. ";

        if (closingParenthesisPos == -1) {
            throw new ScriptException(invalidMsgPrefix + "Unable to determine closing parenthesis position");
        }

        final String keyName = StringUtils.substring(handleResponseFunc, (startPos + keyValuePairFuncPrefix.length()), closingParenthesisPos);

        if (StringUtils.isBlank(keyName)) {
            throw new ScriptException(invalidMsgPrefix + "key within find parenthesis is undefined");
        }

        final String sanitizedKey;

        if (keyName.startsWith("'") && keyName.endsWith("'")) {
            sanitizedKey = Strings.CS.remove(keyName, "'");
        } else if (keyName.startsWith("\"") && keyName.endsWith("\"")) {
            sanitizedKey = Strings.CS.remove(keyName, "\"");
        } else if (keyName.contains("request.")) {

            final String requestObjectField = Strings.CS.remove(keyName, "request.").trim();

            if (requestObjectField.startsWith("pathVars")) {
                final String pathVarsObjectField = Strings.CS.remove(requestObjectField, "pathVars").trim();
                final String sanitizedInboundPath = GeneralUtils.sanitizeMultiUserPath(smockinUserService.getUserMode(), req.getPathInfo(), mock.getCreatedBy().getCtxPath());
                sanitizedKey = GeneralUtils.findAllPathVars(sanitizedInboundPath, mock.getPath()).get(extractObjectField(StringUtils.lowerCase(pathVarsObjectField)));
            } else if ("body".equals(requestObjectField)) {
                final String body = GeneralUtils.extractRequestBody(req);
                if (StringUtils.isBlank(body)) {
                    throw new ScriptException(invalidMsgPrefix + "request.body is undefined");
                }
                sanitizedKey = removeLineBreaks(body);
            } else if (requestObjectField.startsWith("headers")) {
                sanitizedKey = GeneralUtils.findHeaderIgnoreCase(req, extractObjectField(Strings.CS.remove(requestObjectField, "headers").trim()));
            } else if (requestObjectField.startsWith("parameters")) {
                sanitizedKey = GeneralUtils.extractRequestParamByName(req, extractObjectField(Strings.CS.remove(requestObjectField, "parameters").trim()));
            } else {
                throw new ScriptException(invalidMsgPrefix + "Unable to determine request based key look up");
            }
        } else {
            throw new ScriptException(invalidMsgPrefix + "Unable to determine key lookup type");
        }

        return (sanitizedKey != null) ? sanitizedKey.trim() : null;
    }

    private String extractObjectField(final String objectField) {

        if (Strings.CS.startsWith(objectField, ".")) {
            return Strings.CS.remove(objectField, ".");
        } else if (Strings.CS.startsWith(objectField, "[")) {
            final String objectFieldP1 = Strings.CS.remove(objectField, "['");
            return Strings.CS.remove(objectFieldP1, "']");
        }

        return null;
    }

    private ScriptEngine buildEngine() {

        final ScriptEngine engine = new ScriptEngineManager().getEngineByName("graal.js");
        loadEngineExtensions(engine);
        applyEngineBindings(engine);

        return engine;
    }

    private void loadEngineExtensions(final ScriptEngine engine) {
        try {
            engine.eval(new FileReader(getExtensionsFilePath("from-xml.min.js")));
        } catch (ScriptException | FileNotFoundException e) {
            logger.error("Error loading JS extensions", e);
        }
    }

    private String getExtensionsFilePath(final String extensionsFileName) {
        String extensionsDir = "js-extensions/";
        return getClass().getClassLoader().getResource(extensionsDir + extensionsFileName).getFile();
    }

    private void applyEngineBindings(final ScriptEngine engine) {
        final Bindings bindings = engine.getBindings(ScriptContext.ENGINE_SCOPE);
        bindings.remove("exit");
        bindings.remove("java");
        bindings.remove("javax");
        bindings.remove("sun");
    }

    String removeLineBreaks(final String input) {
        return RegExUtils.replaceAll(input, CARRIAGE_RETURN_REGEX, "");
    }

}
