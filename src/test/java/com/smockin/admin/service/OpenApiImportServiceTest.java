package com.smockin.admin.service;

import com.smockin.admin.dto.ApiImportDTO;
import com.smockin.admin.dto.MockImportConfigDTO;
import com.smockin.admin.dto.RestfulMockDTO;
import com.smockin.admin.exception.RecordNotFoundException;
import com.smockin.admin.exception.ValidationException;
import com.smockin.admin.persistence.entity.SmockinUser;
import com.smockin.admin.persistence.enums.RestMethodEnum;
import com.smockin.admin.service.utils.RestfulMockServiceUtils;
import com.smockin.admin.service.utils.UserTokenServiceUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class OpenApiImportServiceTest {

    @Rule
    public ExpectedException expected = ExpectedException.none();

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

    @Before
    public void setUp() throws RecordNotFoundException {
        when(userTokenServiceUtils.loadCurrentActiveUser(token)).thenReturn(user);
        when(user.getSessionToken()).thenReturn("session-token");
    }

    @Test
    public void importApiDoc_FullFlow_Success_Test() throws Exception {
        // Cenário: OpenAPI com múltiplos métodos, caminhos com variáveis e schemas complexos
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

        // Ação
        openApiImportService.importApiDoc(dto, token);

        // Verificações
        verify(restfulMockService).createEndpoint(restfulMockDTOCaptor.capture(), eq("session-token"));

        RestfulMockDTO captured = restfulMockDTOCaptor.getValue();
        Assert.assertEquals("/users/:id", captured.getPath());
        Assert.assertEquals(RestMethodEnum.GET, captured.getMethod());
        Assert.assertEquals(1, captured.getDefinitions().size());

        var definition = captured.getDefinitions().getFirst();
        Assert.assertEquals(200, definition.getHttpStatusCode());
        Assert.assertEquals("100", definition.getResponseHeaders().get("X-Rate-Limit"));
        // Verifica se o corpo foi gerado a partir do schema (resolveBodyFromSchema)
        Assert.assertTrue(definition.getResponseBody().contains("\"id\":0"));
        Assert.assertTrue(definition.getResponseBody().contains("\"name\":\"string\""));
    }

    @Test
    public void importApiDoc_WithRefSchema_Test() throws Exception {
        // Cenário: Testar a resolução de referências ($ref) e recursão
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
        Assert.assertTrue(restfulMockDTOCaptor.getValue().getDefinitions().getFirst().getResponseBody().contains("123"));
    }

    @Test
    public void importApiDoc_ServiceException_Handled_Test() throws Exception {
        // Cenário: Garantir que erros na criação do endpoint não interrompam o loop (apenas logam erro)
        String openApiContent = """
                openapi: 3.0.0
                paths:
                  /path1:
                    get:
                      responses:
                        '200':
                          description: ok
                """;

        when(restfulMockService.createEndpoint(any(), anyString())).thenThrow(new RuntimeException("DB Error"));

        MockMultipartFile file = new MockMultipartFile("file", "api.yaml", "text/yaml", openApiContent.getBytes(StandardCharsets.UTF_8));
        openApiImportService.importApiDoc(new ApiImportDTO(file, new MockImportConfigDTO()), token);

        // Verifica que tentou criar, mesmo que tenha falhado internamente no parsePaths
        verify(restfulMockService, times(1)).createEndpoint(any(), anyString());
    }

    @Test
    public void importApiDoc_ValidationFailure_Test() throws Exception {
        expected.expect(ValidationException.class);
        expected.expectMessage("Invalid import data");

        openApiImportService.importApiDoc(new ApiImportDTO(null, null), token);
    }
}
