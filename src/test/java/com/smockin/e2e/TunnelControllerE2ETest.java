package com.smockin.e2e;

import com.smockin.E2ETestBase;
import com.smockin.admin.dto.response.TunnelResponseDTO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class TunnelControllerE2ETest extends E2ETestBase {

    @Test
    void get_returnsDisabledWhenNoTunnelConfigured() {
        ResponseEntity<TunnelResponseDTO> response = restTemplate.getForEntity(
                url("/tunnel"),
                TunnelResponseDTO.class);

        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
        Assertions.assertNotNull(response.getBody());
        Assertions.assertFalse(response.getBody().isEnabled());
        Assertions.assertNull(response.getBody().getUri());
    }
}
