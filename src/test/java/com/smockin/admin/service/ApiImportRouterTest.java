package com.smockin.admin.service;

import com.smockin.admin.dto.ApiImportDTO;
import com.smockin.admin.dto.MockImportConfigDTO;
import com.smockin.admin.enums.ApiImportTypeEnum;
import com.smockin.admin.exception.MockImportException;
import com.smockin.admin.exception.ValidationException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class ApiImportRouterTest {

    @Mock
    private ApiImportService ramlApiImportService;

    @Mock
    private ApiImportService openApiImportService;

    @Mock
    private MultipartFile mockFile;

    @Mock
    private MockImportConfigDTO mockConfig;

    private ApiImportRouter apiImportRouter;

    private ApiImportDTO dto;
    private final String token = "valid-token";

    @BeforeEach
    void setUp() {
        apiImportRouter = new ApiImportRouter(ramlApiImportService, openApiImportService);
        dto = new ApiImportDTO(mockFile, mockConfig);
    }

    @Test
    void route_OpenApi_Success_Test() throws MockImportException, ValidationException {

        // Test
        apiImportRouter.route(ApiImportTypeEnum.OPENAPI.name(), dto, token);

        // Assertions
        verify(openApiImportService, times(1)).importApiDoc(dto, token);
        verify(ramlApiImportService, times(0)).importApiDoc(any(), anyString());
    }

    @Test
    void route_Raml_Success_Test() throws MockImportException, ValidationException {

        // Test
        apiImportRouter.route(ApiImportTypeEnum.RAML.name(), dto, token);

        // Assertions
        verify(ramlApiImportService, times(1)).importApiDoc(dto, token);
        verify(openApiImportService, times(0)).importApiDoc(any(), anyString());
    }

    @Test
    void validate_NullImportType_Test() {

        // Test & Assertions
        final ValidationException ex = Assertions.assertThrows(ValidationException.class,
                () -> apiImportRouter.validate(null, dto, token));
        Assertions.assertEquals("Import Type is required", ex.getMessage());
    }

    @Test
    void validate_InvalidImportType_Test() {

        // Test & Assertions
        final ValidationException ex = Assertions.assertThrows(ValidationException.class,
                () -> apiImportRouter.validate("INVALID_TYPE", dto, token));
        Assertions.assertEquals("Invalid Import Type: INVALID_TYPE", ex.getMessage());
    }

    @Test
    void validate_NullDto_Test() {

        // Test & Assertions
        final ValidationException ex = Assertions.assertThrows(ValidationException.class,
                () -> apiImportRouter.validate(ApiImportTypeEnum.OPENAPI.name(), null, token));
        Assertions.assertEquals("Inbound dto is undefined", ex.getMessage());
    }

    @Test
    void validate_NullFile_Test() {

        // Setup
        ApiImportDTO dtoWithNullFile = new ApiImportDTO(null, mockConfig);

        // Test & Assertions
        final ValidationException ex = Assertions.assertThrows(ValidationException.class,
                () -> apiImportRouter.validate(ApiImportTypeEnum.OPENAPI.name(), dtoWithNullFile, token));
        Assertions.assertEquals("Inbound file (in dto) is undefined", ex.getMessage());
    }

    @Test
    void validate_NullConfig_Test() {

        // Setup
        ApiImportDTO dtoWithNullConfig = new ApiImportDTO(mockFile, null);

        // Test & Assertions
        final ValidationException ex = Assertions.assertThrows(ValidationException.class,
                () -> apiImportRouter.validate(ApiImportTypeEnum.OPENAPI.name(), dtoWithNullConfig, token));
        Assertions.assertEquals("Inbound config (in dto) is undefined", ex.getMessage());
    }

}
