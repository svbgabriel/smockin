package com.smockin.admin.service;

import com.smockin.admin.dto.AuthDTO;
import com.smockin.admin.exception.AuthException;
import com.smockin.admin.exception.ValidationException;
import com.smockin.admin.persistence.dao.SmockinUserDAO;
import com.smockin.admin.persistence.entity.SmockinUser;
import com.smockin.admin.persistence.enums.SmockinUserRoleEnum;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.smockin.admin.config.JwtConfig;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private SmockinUserDAO smockinUserDAO;

    @Mock
    private EncryptionService encryptionService;

    @Spy
    private JwtConfig jwtConfig = new JwtConfig();

    @InjectMocks
    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        authService.init();
    }

    @Test
    void authenticate_validCredentials_returnsTokenAndSavesUser() throws ValidationException, AuthException {

        // Setup
        final AuthDTO dto = new AuthDTO();
        dto.setUsername("bob");
        dto.setPassword("Passw0rd");

        final SmockinUser user = new SmockinUser();
        user.setUsername("bob");
        user.setFullName("Bob Builder");
        user.setPassword("enc");
        user.setRole(SmockinUserRoleEnum.ADMIN);

        Mockito.when(smockinUserDAO.findByUsername("bob")).thenReturn(user);
        Mockito.when(encryptionService.verify("Passw0rd", "enc")).thenReturn(true);

        // Test
        final String token = authService.authenticate(dto);

        // Assertions
        Assertions.assertNotNull(token);

        final ArgumentCaptor<SmockinUser> userCaptor = ArgumentCaptor.forClass(SmockinUser.class);
        Mockito.verify(smockinUserDAO).save(userCaptor.capture());
        Assertions.assertEquals(token, userCaptor.getValue().getSessionToken());
    }
}
