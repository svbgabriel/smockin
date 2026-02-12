package com.smockin.admin.service;

import com.smockin.admin.dto.ApiImportDTO;
import com.smockin.admin.dto.MockImportConfigDTO;
import com.smockin.admin.dto.RestfulMockDTO;
import com.smockin.admin.dto.RestfulMockDefinitionDTO;
import com.smockin.admin.exception.MockImportException;
import com.smockin.admin.exception.RecordNotFoundException;
import com.smockin.admin.exception.ValidationException;
import com.smockin.admin.persistence.entity.SmockinUser;
import com.smockin.admin.persistence.enums.RecordStatusEnum;
import com.smockin.admin.persistence.enums.RestMethodEnum;
import com.smockin.admin.persistence.enums.RestMockTypeEnum;
import com.smockin.admin.service.utils.RestfulMockServiceUtils;
import com.smockin.admin.service.utils.UserTokenServiceUtils;
import com.smockin.utils.GeneralUtils;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.parser.OpenAPIV3Parser;
import jakarta.transaction.Transactional;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

@Service("openApiImportService")
@Transactional
public class OpenApiImportServiceImpl implements ApiImportService {

    private final Logger logger = LoggerFactory.getLogger(OpenApiImportServiceImpl.class);

    private final RestfulMockService restfulMockService;
    private final UserTokenServiceUtils userTokenServiceUtils;
    private final RestfulMockServiceUtils restfulMockServiceUtils;

    public OpenApiImportServiceImpl(RestfulMockService restfulMockService, UserTokenServiceUtils userTokenServiceUtils, RestfulMockServiceUtils restfulMockServiceUtils) {
        this.restfulMockService = restfulMockService;
        this.userTokenServiceUtils = userTokenServiceUtils;
        this.restfulMockServiceUtils = restfulMockServiceUtils;
    }

    @Override
    public void importApiDoc(final ApiImportDTO dto, final String token) throws MockImportException, ValidationException {

        validate(dto);
        File tempDir = null;

        try {
            tempDir = Files.createTempDirectory("openapi_import_" + System.nanoTime()).toFile();
            File openApiFile = saveUploadedFile(dto.file(), tempDir);

            OpenAPI openAPI = new OpenAPIV3Parser().read(openApiFile.getAbsolutePath());
            if (openAPI == null) {
                throw new MockImportException("Error parsing OpenAPI file");
            }

            final SmockinUser user = userTokenServiceUtils.loadCurrentActiveUser(token);
            final String conflictCtxPath = "api_" + GeneralUtils.createFileNameUniqueTimeStamp();

            parsePaths(openAPI, dto.config(), user, conflictCtxPath);

        } catch (IOException | RecordNotFoundException e) {
            throw new MockImportException("Error during import: " + e.getMessage());
        } finally {
            if (tempDir != null) FileUtils.deleteQuietly(tempDir);
        }
    }

    private void parsePaths(OpenAPI openAPI, MockImportConfigDTO config, SmockinUser user, String conflictCtxPath) {
        openAPI.getPaths().forEach((path, pathItem) -> pathItem.readOperationsMap().forEach((method, operation) -> {

            RestfulMockDTO mockDTO = new RestfulMockDTO(
                    formatPath(path),
                    RestMethodEnum.findByName(method.name()),
                    RecordStatusEnum.ACTIVE,
                    RestMockTypeEnum.SEQ,
                    false, 0, 0, 0, false, false, false, false, 0, 0,
                    null, null, null, null, null);

            processResponses(operation, mockDTO, openAPI);

            try {
                restfulMockServiceUtils.preHandleExistingEndpoints(mockDTO, config, user, conflictCtxPath);
                restfulMockService.createEndpoint(mockDTO, user.getSessionToken());
            } catch (Exception e) {
                logger.error("Error creating endpoint for path: {}", path, e);
            }
        }));
    }

    private void processResponses(Operation operation, RestfulMockDTO mockDTO, OpenAPI openAPI) {
        operation.getResponses().forEach((code, response) -> {
            int statusCode = "default".equals(code) ? 200 : Integer.parseInt(code);

            if (response.getContent() == null || response.getContent().isEmpty()) {
                mockDTO.getDefinitions().add(new RestfulMockDefinitionDTO(1, statusCode, "application/json", null, 1));
                return;
            }

            response.getContent().forEach((mimeType, mediaType) -> {
                String body = extractBody(mediaType, openAPI);
                RestfulMockDefinitionDTO def = new RestfulMockDefinitionDTO(1, statusCode, mimeType, body, 1);

                populateHeaders(response, def);
                mockDTO.getDefinitions().add(def);
            });
        });
    }

    private String extractBody(io.swagger.v3.oas.models.media.MediaType mediaType, OpenAPI openAPI) {
        if (mediaType.getExample() != null) {
            return mediaType.getExample().toString();
        }
        return mediaType.getSchema() != null ? resolveBodyFromSchema(mediaType.getSchema(), openAPI) : null;
    }

    private void populateHeaders(io.swagger.v3.oas.models.responses.ApiResponse response, RestfulMockDefinitionDTO def) {
        if (response.getHeaders() == null) return;

        response.getHeaders().forEach((headerName, header) -> {
            String value = "";
            if (header.getExample() != null) {
                value = header.getExample().toString();
            } else if (header.getSchema() != null) {
                Object defaultValue = header.getSchema().getDefault();
                Object exampleValue = header.getSchema().getExample();
                value = (defaultValue != null) ? defaultValue.toString() : (exampleValue != null ? exampleValue.toString() : "");
            }
            def.getResponseHeaders().put(headerName, value);
        });
    }

    private String resolveBodyFromSchema(Schema<?> schema, OpenAPI openAPI) {

        Object structure = constructSampleStructure(schema, openAPI, 0);

        if (structure == null) {
            return null;
        }

        return (structure instanceof String && ((String) structure).startsWith("{"))
                ? (String) structure
                : com.smockin.utils.GeneralUtils.serialiseJson(structure);
    }

    private Object constructSampleStructure(Schema<?> schema, OpenAPI openAPI, int depth) {

        // Here to avoid infinite recursion
        if (schema == null || depth > 5) {
            return null;
        }

        // Resolve Reference
        if (schema.get$ref() != null) {
            String refName = schema.get$ref().substring(schema.get$ref().lastIndexOf("/") + 1);
            schema = openAPI.getComponents().getSchemas().get(refName);
        }

        // First trys to get the example or default values
        if (schema.getExample() != null) return schema.getExample();
        if (schema.getDefault() != null) return schema.getDefault();

        // 2. Handles Objects (Nested)
        if (schema.getProperties() != null && !schema.getProperties().isEmpty()) {
            Map<String, Object> map = new LinkedHashMap<>();
            for (Object entryObj : schema.getProperties().entrySet()) {
                Map.Entry<String, Schema<?>> entry = (Map.Entry<String, Schema<?>>) entryObj;
                map.put(entry.getKey(), constructSampleStructure(entry.getValue(), openAPI, depth + 1));
            }
            return map;
        }

        // 3. Handles Arrays
        if (schema instanceof ArraySchema arraySchema) {
            Object itemSample = constructSampleStructure(arraySchema.getItems(), openAPI, depth + 1);
            return (itemSample != null) ? Collections.singletonList(itemSample) : Collections.emptyList();
        }

        // 4. Fallback for primitive types
        String type = schema.getType();
        if ("integer".equals(type) || "number".equals(type)) return 0;
        if ("boolean".equals(type)) return true;

        return "string"; // Default for string or unknown types
    }

    private String formatPath(String path) {
        return path.replace("{", ":").replace("}", "");
    }

    private void validate(ApiImportDTO dto) throws ValidationException {
        if (dto == null || dto.file() == null || dto.config() == null)
            throw new ValidationException("Invalid import data");
    }

    private File saveUploadedFile(MultipartFile file, File tempDir) throws IOException {
        if (file.getOriginalFilename() == null) {
            throw new IOException("No file found");
        }

        File uploadedFile = new File(tempDir, file.getOriginalFilename());
        FileUtils.copyInputStreamToFile(file.getInputStream(), uploadedFile);
        return uploadedFile;
    }

}
