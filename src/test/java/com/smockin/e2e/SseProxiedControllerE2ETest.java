package com.smockin.e2e;

import com.smockin.E2ETestBase;
import com.smockin.mockserver.service.dto.SseMessageDTO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class SseProxiedControllerE2ETest extends E2ETestBase {

    @Test
    void send_returnsNoContent() {
        SseMessageDTO dto = new SseMessageDTO("/sse/path", "hello");

        ResponseEntity<String> response = restTemplate.postForEntity(
                url("/sse/missing"),
                dto,
                String.class);

        Assertions.assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }
}
