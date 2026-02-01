package com.smockin.e2e;

import com.smockin.E2ETestBase;
import com.smockin.SmockinTestUtils;
import com.smockin.admin.dto.AuthDTO;
import com.smockin.admin.dto.response.SimpleMessageResponseDTO;
import com.smockin.admin.persistence.dao.SmockinUserDAO;
import com.smockin.admin.persistence.entity.SmockinUser;
import com.smockin.admin.service.EncryptionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class AuthControllerE2ETest extends E2ETestBase {

    @Autowired
    private SmockinUserDAO smockinUserDAO;

    @Autowired
    private EncryptionService encryptionService;

    @BeforeEach
    void setUp() {
        String encryptedPassword = encryptionService.encrypt("superSecret");
        SmockinUser smockinUser = SmockinTestUtils.buildAdminSmockinUser("newAdmin", encryptedPassword);
        smockinUserDAO.save(smockinUser);
    }

    @AfterEach
    void tearDown() {
        smockinUserDAO.deleteAll();
    }

    @Test
    void authenticate_returnsTokenForAdmin() {
        AuthDTO dto = new AuthDTO();
        dto.setUsername("newAdmin");
        dto.setPassword("superSecret");

        ResponseEntity<SimpleMessageResponseDTO> response = restTemplate.postForEntity(
                url("/auth"),
                dto,
                SimpleMessageResponseDTO.class);

        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
        Assertions.assertNotNull(response.getBody());
        Assertions.assertNotNull(response.getBody().message());
    }
}
