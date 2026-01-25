package com.smockin.admin.service;

import com.smockin.admin.dto.MockImportConfigDTO;
import com.smockin.admin.dto.ApiImportDTO;
import com.smockin.admin.dto.RestfulMockDTO;
import com.smockin.admin.enums.MockImportKeepStrategyEnum;
import com.smockin.admin.exception.MockImportException;
import com.smockin.admin.exception.RecordNotFoundException;
import com.smockin.admin.exception.ValidationException;
import com.smockin.admin.persistence.dao.RestfulMockDAO;
import com.smockin.admin.persistence.entity.SmockinUser;
import com.smockin.admin.persistence.enums.RestMethodEnum;
import com.smockin.admin.service.utils.RestfulMockServiceUtils;
import com.smockin.admin.service.utils.UserTokenServiceUtils;
import com.smockin.utils.GeneralUtils;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

@ExtendWith(MockitoExtension.class)
class RamlApiImportServiceTest {

    @Mock
    private RestfulMockService restfulMockService;

    @Mock
    private RestfulMockDAO restfulMockDAO;

    @Mock
    private UserTokenServiceUtils userTokenServiceUtils;

    @Mock
    private SmockinUser user;

    @Mock
    private RestfulMockServiceUtils restfulMockServiceUtils;

    @Captor
    private ArgumentCaptor<RestfulMockDTO> argCaptor;

    @Spy
    @InjectMocks
    private ApiImportService apiImportService = new RamlApiImportServiceImpl();

    @Test
    void importApiDocPass() throws MockImportException, ValidationException, RecordNotFoundException, URISyntaxException, IOException {

        // Setup
        final ApiImportDTO importDTO = new ApiImportDTO(buildMockMultiPartFile("raml/raml_100.raml"), new MockImportConfigDTO(MockImportKeepStrategyEnum.RENAME_EXISTING));

        Mockito.when(userTokenServiceUtils.loadCurrentActiveUser(Mockito.anyString())).thenReturn(user);
        Mockito.when(user.getSessionToken()).thenReturn(GeneralUtils.generateUUID());

        // Test
        apiImportService.importApiDoc(importDTO, GeneralUtils.generateUUID());

        // Assertions
        Mockito.verify(restfulMockService, Mockito.times(3)).createEndpoint(argCaptor.capture(), Mockito.anyString());

        final List<RestfulMockDTO> restfulMockDTOs = argCaptor.getAllValues();

        for (RestfulMockDTO mockDTO : restfulMockDTOs) {

            if ("/hello".equals(mockDTO.getPath())) {

                Assertions.assertEquals(RestMethodEnum.GET, mockDTO.getMethod());

                Assertions.assertEquals(1, mockDTO.getDefinitions().size());
                Assertions.assertEquals(200, mockDTO.getDefinitions().get(0).getHttpStatusCode());
                Assertions.assertEquals("application/json", mockDTO.getDefinitions().get(0).getResponseContentType());
                Assertions.assertEquals("{ \"message\": \"helloworld\" }\n", mockDTO.getDefinitions().get(0).getResponseBody());

                Assertions.assertEquals(1, mockDTO.getDefinitions().get(0).getResponseHeaders().size());
                Assertions.assertNotNull(mockDTO.getDefinitions().get(0).getResponseHeaders().get("X-Powered-By"));
                Assertions.assertEquals("FooBar", mockDTO.getDefinitions().get(0).getResponseHeaders().get("X-Powered-By"));

            } else if ("/hello/:name".equals(mockDTO.getPath())) {

                if (RestMethodEnum.GET.equals(mockDTO.getMethod())) {

                    Assertions.assertEquals(3, mockDTO.getDefinitions().size());

                    mockDTO.getDefinitions().stream().forEach(d -> {

                        if (200 == d.getHttpStatusCode()) {

                            Assertions.assertEquals("application/json", d.getResponseContentType());
                            Assertions.assertEquals("{ \"message\": \"hello John!\" }\n", d.getResponseBody());
                            Assertions.assertTrue(d.getResponseHeaders().isEmpty());

                        } else if (404 == d.getHttpStatusCode()) {

                            Assertions.assertEquals("application/json", d.getResponseContentType());
                            Assertions.assertNull(d.getResponseBody());
                            Assertions.assertTrue(d.getResponseHeaders().isEmpty());

                        } else if (400 == d.getHttpStatusCode()) {

                            Assertions.assertEquals("application/json", d.getResponseContentType());
                            Assertions.assertEquals("{ \"message\": \"Missing name!\" }\n", d.getResponseBody());
                            Assertions.assertTrue(d.getResponseHeaders().isEmpty());

                        } else {
                            Assertions.fail();
                        }

                    });

                } else if (RestMethodEnum.POST.equals(mockDTO.getMethod())) {

                    Assertions.assertEquals(1, mockDTO.getDefinitions().size());
                    Assertions.assertEquals(201, mockDTO.getDefinitions().get(0).getHttpStatusCode());
                    Assertions.assertEquals("application/json", mockDTO.getDefinitions().get(0).getResponseContentType());
                    Assertions.assertEquals("{ \"id\" : 1 }\n", mockDTO.getDefinitions().get(0).getResponseBody());

                    Assertions.assertEquals(1, mockDTO.getDefinitions().get(0).getResponseHeaders().size());
                    Assertions.assertNotNull(mockDTO.getDefinitions().get(0).getResponseHeaders().get("X-Powered-By"));
                    Assertions.assertEquals("FooBar", mockDTO.getDefinitions().get(0).getResponseHeaders().get("X-Powered-By"));

                } else {

                    Assertions.fail();

                }

            } else {
                Assertions.fail();
            }


        }

    }

    @Test
    void importApiDoc_NullDto_Fail() throws MockImportException, ValidationException {

        // Test & Assertions
        final ValidationException ex = Assertions.assertThrows(ValidationException.class,
                () -> apiImportService.importApiDoc(null, GeneralUtils.generateUUID()));
        Assertions.assertEquals("No data was provided", ex.getMessage());

    }

    @Test
    void importApiDoc_NullFile_Fail() throws MockImportException, ValidationException {

        // Setup
        final ApiImportDTO importDTO = new ApiImportDTO(null, new MockImportConfigDTO(MockImportKeepStrategyEnum.RENAME_EXISTING));

        // Test & Assertions
        final ValidationException ex = Assertions.assertThrows(ValidationException.class,
                () -> apiImportService.importApiDoc(importDTO, GeneralUtils.generateUUID()));
        Assertions.assertEquals("No file found", ex.getMessage());

    }

    @Test
    void importApiDoc_NullConfig_Fail() throws MockImportException, ValidationException, URISyntaxException, IOException {

        // Setup
        final ApiImportDTO importDTO = new ApiImportDTO(buildMockMultiPartFile("raml/raml_100.raml"), null);

        // Test & Assertions
        final ValidationException ex = Assertions.assertThrows(ValidationException.class,
                () -> apiImportService.importApiDoc(importDTO, GeneralUtils.generateUUID()));
        Assertions.assertEquals("No config found", ex.getMessage());

    }

    @Test
    void importApiDoc_InvalidContent_Fail() throws MockImportException, URISyntaxException, IOException {

        // Setup
        final ApiImportDTO importDTO = new ApiImportDTO(buildMockMultiPartFile("raml/bad_raml_100.raml"), new MockImportConfigDTO());

        // Test & Assertions
        final MockImportException ex = Assertions.assertThrows(MockImportException.class,
                () -> apiImportService.importApiDoc(importDTO, GeneralUtils.generateUUID()));
        Assertions.assertTrue(ex.getMessage().startsWith("/ Unexpected key 'get'. Options are :"));

    }

    MockMultipartFile buildMockMultiPartFile(final String fileName) throws URISyntaxException, IOException {

        final URL ramlUrl = this.getClass().getClassLoader().getResource(fileName);
        final File ramlFile = new File(ramlUrl.toURI());
        final FileInputStream ramlInput = new FileInputStream(ramlFile);

        return new MockMultipartFile(fileName, ramlFile.getName(), "text/plain", IOUtils.toByteArray(ramlInput));
    }

}
