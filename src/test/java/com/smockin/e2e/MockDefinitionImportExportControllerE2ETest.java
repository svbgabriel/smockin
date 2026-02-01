package com.smockin.e2e;

import com.smockin.E2ETestBase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Collections;

class MockDefinitionImportExportControllerE2ETest extends E2ETestBase {

    @Test
    void exportMocks_rejectsInvalidServerType() {
        ResponseEntity<String> response = restTemplate.postForEntity(
                url("/mock/export/INVALID"),
                Collections.emptyList(),
                String.class);

        Assertions.assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }
}
