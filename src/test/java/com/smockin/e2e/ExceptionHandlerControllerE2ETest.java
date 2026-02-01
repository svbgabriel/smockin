package com.smockin.e2e;

import com.smockin.E2ETestBase;
import com.smockin.admin.dto.AuthDTO;
import com.smockin.admin.dto.response.SimpleMessageResponseDTO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class ExceptionHandlerControllerE2ETest extends E2ETestBase {

    @Test
    void handleValidationException_returnsBadRequest() {
        AuthDTO dto = new AuthDTO();

        ResponseEntity<SimpleMessageResponseDTO> response = restTemplate.postForEntity(
                url("/auth"),
                dto,
                SimpleMessageResponseDTO.class);

        Assertions.assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Assertions.assertNotNull(response.getBody());
        Assertions.assertEquals("username is required", String.valueOf(response.getBody().message()));
    }
}
