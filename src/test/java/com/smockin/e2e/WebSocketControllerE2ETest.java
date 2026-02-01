package com.smockin.e2e;

import com.smockin.E2ETestBase;
import com.smockin.mockserver.service.dto.WebSocketDTO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class WebSocketControllerE2ETest extends E2ETestBase {

    @Test
    void sendMessage_returnsNoContent() {
        WebSocketDTO dto = new WebSocketDTO("/ws/path", "hello");

        ResponseEntity<String> response = restTemplate.postForEntity(
                url("/ws/missing"),
                dto,
                String.class);

        Assertions.assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }
}
