package com.smockin.e2e;

import com.smockin.E2ETestBase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class MailMockMessageControllerE2ETest extends E2ETestBase {

    @Test
    void getInboxMessages_returnsNotFoundForMissingMailbox() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                url("/mailmock/missing/inbox?pageStart=0"),
                String.class);

        Assertions.assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }
}
