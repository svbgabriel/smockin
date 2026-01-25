package com.smockin.admin.service;

import com.smockin.admin.dto.ApiImportDTO;
import com.smockin.admin.dto.MockImportConfigDTO;
import com.smockin.admin.dto.RestfulMockDTO;
import com.smockin.admin.exception.MockImportException;
import com.smockin.admin.exception.ValidationException;
import com.smockin.admin.persistence.entity.SmockinUser;
import com.smockin.admin.persistence.enums.RestMethodEnum;
import com.smockin.admin.service.utils.RestfulMockServiceUtils;
import com.smockin.admin.service.utils.UserTokenServiceUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OpenApiImportServiceTest {

    @Mock
    private RestfulMockService restfulMockService;

    @Mock
    private UserTokenServiceUtils userTokenServiceUtils;

    @Mock
    private RestfulMockServiceUtils restfulMockServiceUtils;

    @Mock
    private SmockinUser user;

    @InjectMocks
    private OpenApiImportServiceImpl openApiImportService;

    @Captor
    private ArgumentCaptor<RestfulMockDTO> restfulMockDTOCaptor;

    private final String token = "test-token";

    @BeforeEach
    void setUp() {
        lenient().when(userTokenServiceUtils.loadCurrentActiveUser(anyString())).thenReturn(user);
        lenient().when(user.getSessionToken()).thenReturn("session-token");
    }

    @Test
    void importApiDoc_FullFlow_Success_Test() throws Exception {
        // Scenario: OpenAPI with complex schema, variables in path and headers
        String openApiContent = """
                openapi: 3.0.0
                info:
                  title: Complex API
                  version: 1.0.0
                paths:
                  /users/{id}:
                    get:
                      responses:
                        '200':
                          description: Success
                          headers:
                            X-Rate-Limit:
                              schema:
                                type: integer
                                default: 100
                          content:
                            application/json:
                              schema:
                                type: object
                                properties:
                                  id:
                                    type: integer
                                  name:
                                    type: string
                """;

        MockMultipartFile file = new MockMultipartFile("file", "api.yaml", "text/yaml", openApiContent.getBytes(StandardCharsets.UTF_8));
        ApiImportDTO dto = new ApiImportDTO(file, new MockImportConfigDTO());

        // Action
        openApiImportService.importApiDoc(dto, token);

        // Assertions
        verify(restfulMockService).createEndpoint(restfulMockDTOCaptor.capture(), anyString());
        RestfulMockDTO captured = restfulMockDTOCaptor.getValue();

        Assertions.assertEquals("/users/:id", captured.getPath());
        Assertions.assertEquals(RestMethodEnum.GET, captured.getMethod());
        Assertions.assertEquals(1, captured.getDefinitions().size());

        var definition = captured.getDefinitions().get(0);
        Assertions.assertEquals(200, definition.getHttpStatusCode());
        Assertions.assertEquals("100", definition.getResponseHeaders().get("X-Rate-Limit"));
        // Verify body generation from schema
        Assertions.assertTrue(definition.getResponseBody().contains("\"id\":0"));
        Assertions.assertTrue(definition.getResponseBody().contains("\"name\":\"string\""));
    }

    @Test
    void importApiDoc_WithRefSchema_Test() throws Exception {
        // Scenario: Test reference resolution ($ref)
        String openApiContent = """
                openapi: 3.0.0
                paths:
                  /test:
                    post:
                      responses:
                        '201':
                          content:
                            application/json:
                              schema:
                                $ref: '#/components/schemas/Item'
                components:
                  schemas:
                    Item:
                      type: object
                      properties:
                        code:
                          type: integer
                          example: 123
                """;

        MockMultipartFile file = new MockMultipartFile("file", "ref.yaml", "text/yaml", openApiContent.getBytes(StandardCharsets.UTF_8));
        ApiImportDTO dto = new ApiImportDTO(file, new MockImportConfigDTO());

        openApiImportService.importApiDoc(dto, token);

        verify(restfulMockService).createEndpoint(restfulMockDTOCaptor.capture(), anyString());
        Assertions.assertTrue(restfulMockDTOCaptor.getValue().getDefinitions().get(0).getResponseBody().contains("123"));
    }

    @Test
    void importApiDoc_ArraySchema_Test() throws Exception {
        // Scenario: Test handling of ArraySchema in response body
        String openApiContent = """
                openapi: 3.0.0
                paths:
                  /items:
                    get:
                      responses:
                        '200':
                          content:
                            application/json:
                              schema:
                                type: array
                                items:
                                  type: string
                                  example: "test-item"
                """;

        MockMultipartFile file = new MockMultipartFile("file", "array.yaml", "text/yaml", openApiContent.getBytes(StandardCharsets.UTF_8));
        openApiImportService.importApiDoc(new ApiImportDTO(file, new MockImportConfigDTO()), token);

        verify(restfulMockService).createEndpoint(restfulMockDTOCaptor.capture(), anyString());
        Assertions.assertEquals("[\"test-item\"]", restfulMockDTOCaptor.getValue().getDefinitions().get(0).getResponseBody());
    }

    @Test
    void importApiDoc_ServiceException_Handled_Test() throws Exception {
        // Scenario: Internal error creating endpoint should not stop the process but log error
        String openApiContent = """
                openapi: 3.0.0
                paths:
                  /path1:
                    get:
                      responses:
                        '200':
                          description: ok
                """;

        doThrow(new RuntimeException("Database Error")).when(restfulMockService).createEndpoint(any(), anyString());

        MockMultipartFile file = new MockMultipartFile("file", "api.yaml", "text/yaml", openApiContent.getBytes(StandardCharsets.UTF_8));
        openApiImportService.importApiDoc(new ApiImportDTO(file, new MockImportConfigDTO()), token);

        verify(restfulMockService, times(1)).createEndpoint(any(), anyString());
    }

    @Test
    void importApiDoc_ValidationFailure_Test() {
        // Scenario: Null DTO should throw ValidationException
        final ValidationException ex = Assertions.assertThrows(ValidationException.class,
                () -> openApiImportService.importApiDoc(new ApiImportDTO(null, null), token));
        Assertions.assertEquals("Invalid import data", ex.getMessage());
    }

    @Test
    void importApiDoc_ParsingError_Test() {
        // Scenario: An invalid OpenAPI format should throw MockImportException
        MockMultipartFile file = new MockMultipartFile("file", "invalid.yaml", "text/yaml", "invalid: content: :".getBytes());
        ApiImportDTO dto = new ApiImportDTO(file, new MockImportConfigDTO());

        final MockImportException ex = Assertions.assertThrows(MockImportException.class,
                () -> openApiImportService.importApiDoc(dto, token));
        Assertions.assertEquals("Error parsing OpenAPI file", ex.getMessage());
    }

    @Test
    void importApiDoc_EmptyFile_Test() {
        // Scenario: MultiPartFile with no original filename
        MockMultipartFile file = new MockMultipartFile("file", null, "text/yaml", "".getBytes());
        ApiImportDTO dto = new ApiImportDTO(file, new MockImportConfigDTO());

        final MockImportException ex = Assertions.assertThrows(MockImportException.class,
                () -> openApiImportService.importApiDoc(dto, token));
        Assertions.assertTrue(ex.getMessage().startsWith("Error during import:"));
    }

}
