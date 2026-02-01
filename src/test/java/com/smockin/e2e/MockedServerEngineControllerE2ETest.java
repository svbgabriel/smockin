package com.smockin.e2e;

import com.smockin.E2ETestBase;
import com.smockin.mockserver.dto.MockServerState;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class MockedServerEngineControllerE2ETest extends E2ETestBase {

    @Test
    void restStatus_returnsStoppedByDefault() {
        ResponseEntity<MockServerState> response = restTemplate.getForEntity(
                url("/mockedserver/rest/status"),
                MockServerState.class);

        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
        Assertions.assertNotNull(response.getBody());
        Assertions.assertFalse(response.getBody().isRunning());
    }
}
