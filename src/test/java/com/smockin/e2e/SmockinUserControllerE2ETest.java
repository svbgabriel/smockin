package com.smockin.e2e;

import com.smockin.E2ETestBase;
import com.smockin.admin.dto.response.SimpleMessageResponseDTO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class SmockinUserControllerE2ETest extends E2ETestBase {

    @Test
    void getUserMode_returnsInactiveByDefault() {
        ResponseEntity<SimpleMessageResponseDTO> response = restTemplate.getForEntity(
                url("/user/mode"),
                SimpleMessageResponseDTO.class);

        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
        Assertions.assertNotNull(response.getBody());
        Assertions.assertEquals("INACTIVE", String.valueOf(response.getBody().message()));
    }
}
