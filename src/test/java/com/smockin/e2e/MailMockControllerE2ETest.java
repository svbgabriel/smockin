package com.smockin.e2e;

import com.smockin.E2ETestBase;
import com.smockin.SmockinTestUtils;
import com.smockin.admin.dto.AuthDTO;
import com.smockin.admin.dto.response.MailMockResponseLiteDTO;
import com.smockin.admin.exception.AuthException;
import com.smockin.admin.exception.ValidationException;
import com.smockin.admin.persistence.dao.SmockinUserDAO;
import com.smockin.admin.persistence.entity.SmockinUser;
import com.smockin.admin.service.AuthService;
import com.smockin.admin.service.EncryptionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;

import java.util.List;

class MailMockControllerE2ETest extends E2ETestBase {

    @Autowired
    private SmockinUserDAO smockinUserDAO;

    @Autowired
    private EncryptionService encryptionService;

    @Autowired
    private AuthService authService;

    String token;

    @BeforeEach
    void setUp() throws ValidationException, AuthException {
        AuthDTO authDTO = new AuthDTO();
        authDTO.setUsername("newAdmin");
        authDTO.setPassword("superSecret");
        String encryptedPassword = encryptionService.encrypt("superSecret");
        SmockinUser smockinUser = SmockinTestUtils.buildAdminSmockinUser("newAdmin", encryptedPassword);
        smockinUserDAO.save(smockinUser);
        token = authService.authenticate(authDTO);
    }

    @AfterEach
    void tearDown() {
        smockinUserDAO.deleteAll();
        token = null;
    }

    @Test
    void getAll_returnsEmptyListWhenNoMocksExist() {
        RequestEntity<Void> request = RequestEntity.get(url("/mailmock")).header("Authorization", "Bearer " + token).build();

        ResponseEntity<List<MailMockResponseLiteDTO>> response = restTemplate.exchange(request,
                new ParameterizedTypeReference<>() {
                }
        );

        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
        Assertions.assertNotNull(response.getBody());
        Assertions.assertTrue(response.getBody().isEmpty());
    }
}
