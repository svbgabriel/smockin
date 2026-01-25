package com.smockin.admin.service;

import com.smockin.admin.dto.SmockinNewUserDTO;
import com.smockin.admin.exception.AuthException;
import com.smockin.admin.exception.RecordNotFoundException;
import com.smockin.admin.exception.ValidationException;
import com.smockin.admin.persistence.dao.AppConfigDAO;
import com.smockin.admin.persistence.dao.RestfulMockDAO;
import com.smockin.admin.persistence.dao.SmockinUserDAO;
import com.smockin.admin.persistence.entity.SmockinUser;
import com.smockin.admin.persistence.enums.SmockinUserRoleEnum;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SmockinUserServiceTest {

    @Mock
    private EncryptionService encryptionService;

    @Mock
    private SmockinUserDAO smockinUserDAO;

    @Mock
    private AppConfigDAO appConfigDAO;

    @Mock
    private RestfulMockDAO restfulMockDAO;

    @InjectMocks
    private SmockinUserServiceImpl smockinUserService;

    @Test
    void createUser_returnsExternalId() throws ValidationException, RecordNotFoundException, AuthException {

        // Setup
        final SmockinNewUserDTO dto = new SmockinNewUserDTO("bob", "Bob Builder", SmockinUserRoleEnum.REGULAR, "Passw0rd");

        final SmockinUser adminUser = new SmockinUser();
        adminUser.setRole(SmockinUserRoleEnum.ADMIN);

        final SmockinUser savedUser = new SmockinUser();
        savedUser.setExtId("user-1");

        Mockito.when(smockinUserDAO.findBySessionToken("token")).thenReturn(adminUser);
        Mockito.when(restfulMockDAO.doesMockPathStartWithSegment("bob")).thenReturn(false);
        Mockito.when(encryptionService.encrypt("Passw0rd")).thenReturn("enc");
        Mockito.when(smockinUserDAO.save(Mockito.any(SmockinUser.class))).thenReturn(savedUser);

        // Test
        final String result = smockinUserService.createUser(dto, "token");

        // Assertions
        Assertions.assertEquals("user-1", result);
    }
}
