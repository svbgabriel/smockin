package com.smockin.admin.service.utils;

import com.smockin.admin.dto.RestfulMockDTO;
import com.smockin.admin.enums.UserModeEnum;
import com.smockin.admin.exception.ValidationException;
import com.smockin.admin.persistence.dao.RestfulMockDAO;
import com.smockin.admin.persistence.dao.SmockinUserDAO;
import com.smockin.admin.service.SmockinUserService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Created by mgallina.
 */
@ExtendWith(MockitoExtension.class)
class RestfulMockServiceUtilsTest {

    @Mock
    private RestfulMockDAO restfulMockDefinitionDAO;

    @Mock
    private RestfulMockSortingUtils restfulMockSortingUtils;

    @Mock
    private SmockinUserService smockinUserService;

    @Mock
    private SmockinUserDAO smockinUserDAO;


    @Spy
    @InjectMocks
    private RestfulMockServiceUtils utils;

    private RestfulMockDTO dto;


    @BeforeEach
    void setUp() {
        dto = new RestfulMockDTO();
    }

    @Test
    void amendPath_PrefixAdded_Test() {

        // Setup
        dto.setPath("foo");

        // Test
        utils.amendPath(dto);

        // Assertions
        Assertions.assertEquals("/foo", dto.getPath());

    }

    @Test
    void amendPath_NothingToChange_Test() {

        // Setup
        dto.setPath("/foo");

        // Test
        utils.amendPath(dto);

        // Assertions
        Assertions.assertEquals("/foo", dto.getPath());

    }

    @Test
    void validateMockPathDoesNotStartWithUsername_1partPath_Test() throws ValidationException {

        // Setup
        Mockito.when(smockinUserService.getUserMode()).thenReturn(UserModeEnum.ACTIVE);
        Mockito.when(smockinUserDAO.existsSmockinUserByUsername(Mockito.anyString())).thenReturn(false);

        // Test
        utils.validateMockPathDoesNotStartWithUsername("/bob");

        // Assertions
        final ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(smockinUserDAO, Mockito.times(1)).existsSmockinUserByUsername(captor.capture());
        final String pathSegment = captor.getValue();
        Assertions.assertNotNull(pathSegment);
        Assertions.assertEquals("bob", pathSegment);

    }

    @Test
    void validateMockPathDoesNotStartWithUsername_1partNonPrefixedPath_Test() throws ValidationException {

        // Setup
        Mockito.when(smockinUserService.getUserMode()).thenReturn(UserModeEnum.ACTIVE);
        Mockito.when(smockinUserDAO.existsSmockinUserByUsername(Mockito.anyString())).thenReturn(false);

        // Test
        utils.validateMockPathDoesNotStartWithUsername("bob");

        // Assertions
        final ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(smockinUserDAO, Mockito.times(1)).existsSmockinUserByUsername(captor.capture());
        final String pathSegment = captor.getValue();
        Assertions.assertNotNull(pathSegment);
        Assertions.assertEquals("bob", pathSegment);

    }

    @Test
    void validateMockPathDoesNotStartWithUsername_2partPath_Test() throws ValidationException {

        // Setup
        Mockito.when(smockinUserService.getUserMode()).thenReturn(UserModeEnum.ACTIVE);
        Mockito.when(smockinUserDAO.existsSmockinUserByUsername(Mockito.anyString())).thenReturn(false);

        // Test
        utils.validateMockPathDoesNotStartWithUsername("/bob/house");

        // Assertions
        final ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(smockinUserDAO, Mockito.times(1)).existsSmockinUserByUsername(captor.capture());
        final String pathSegment = captor.getValue();
        Assertions.assertNotNull(pathSegment);
        Assertions.assertEquals("bob", pathSegment);

    }

    @Test
    void validateMockPathDoesNotStartWithUsername_2partNonPrefixedPath_Test() throws ValidationException {

        // Setup
        Mockito.when(smockinUserService.getUserMode()).thenReturn(UserModeEnum.ACTIVE);
        Mockito.when(smockinUserDAO.existsSmockinUserByUsername(Mockito.anyString())).thenReturn(false);

        // Test
        utils.validateMockPathDoesNotStartWithUsername("bob/house");

        // Assertions
        final ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(smockinUserDAO, Mockito.times(1)).existsSmockinUserByUsername(captor.capture());
        final String pathSegment = captor.getValue();
        Assertions.assertNotNull(pathSegment);
        Assertions.assertEquals("bob", pathSegment);

    }

}
