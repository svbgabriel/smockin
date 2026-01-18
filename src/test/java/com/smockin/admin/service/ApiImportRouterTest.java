package com.smockin.admin.service;

import com.smockin.admin.dto.ApiImportDTO;
import com.smockin.admin.dto.MockImportConfigDTO;
import com.smockin.admin.enums.ApiImportTypeEnum;
import com.smockin.admin.exception.MockImportException;
import com.smockin.admin.exception.ValidationException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.web.multipart.MultipartFile;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

@RunWith(MockitoJUnitRunner.class)
public class ApiImportRouterTest {

    @Mock
    private ApiImportService ramlApiImportService;

    @Mock
    private ApiImportService openApiImportService;

    @Mock
    private MultipartFile mockFile;

    @Mock
    private MockImportConfigDTO mockConfig;

    private ApiImportRouter apiImportRouter;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private ApiImportDTO dto;
    private final String token = "valid-token";

    @Before
    public void setUp() {
        apiImportRouter = new ApiImportRouter(ramlApiImportService, openApiImportService);
        dto = new ApiImportDTO(mockFile, mockConfig);
    }

    @Test
    public void route_OpenApi_Success_Test() throws MockImportException, ValidationException {

        // Test
        apiImportRouter.route(ApiImportTypeEnum.OPENAPI.name(), dto, token);

        // Assertions
        verify(openApiImportService, times(1)).importApiDoc(dto, token);
        verify(ramlApiImportService, times(0)).importApiDoc(any(), anyString());
    }

    @Test
    public void route_Raml_Success_Test() throws MockImportException, ValidationException {

        // Test
        apiImportRouter.route(ApiImportTypeEnum.RAML.name(), dto, token);

        // Assertions
        verify(ramlApiImportService, times(1)).importApiDoc(dto, token);
        verify(openApiImportService, times(0)).importApiDoc(any(), anyString());
    }

    @Test
    public void validate_NullImportType_Test() throws ValidationException {

        // Assertions
        thrown.expect(ValidationException.class);
        thrown.expectMessage("Import Type is required");

        // Test
        apiImportRouter.validate(null, dto, token);
    }

    @Test
    public void validate_InvalidImportType_Test() throws ValidationException {

        // Assertions
        thrown.expect(ValidationException.class);
        thrown.expectMessage("Invalid Import Type: INVALID_TYPE");

        // Test
        apiImportRouter.validate("INVALID_TYPE", dto, token);
    }

    @Test
    public void validate_NullDto_Test() throws ValidationException {

        // Assertions
        thrown.expect(ValidationException.class);
        thrown.expectMessage("Inbound dto is undefined");

        // Test
        apiImportRouter.validate(ApiImportTypeEnum.OPENAPI.name(), null, token);
    }

    @Test
    public void validate_NullFile_Test() throws ValidationException {

        // Setup
        ApiImportDTO dtoWithNullFile = new ApiImportDTO(null, mockConfig);

        // Assertions
        thrown.expect(ValidationException.class);
        thrown.expectMessage("Inbound file (in dto) is undefined");

        // Test
        apiImportRouter.validate(ApiImportTypeEnum.OPENAPI.name(), dtoWithNullFile, token);
    }

    @Test
    public void validate_NullConfig_Test() throws ValidationException {

        // Setup
        ApiImportDTO dtoWithNullConfig = new ApiImportDTO(mockFile, null);

        // Assertions
        thrown.expect(ValidationException.class);
        thrown.expectMessage("Inbound config (in dto) is undefined");

        // Test
        apiImportRouter.validate(ApiImportTypeEnum.OPENAPI.name(), dtoWithNullConfig, token);
    }

}
