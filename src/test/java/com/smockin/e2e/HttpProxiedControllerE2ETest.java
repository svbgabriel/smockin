package com.smockin.e2e;

import com.smockin.E2ETestBase;
import com.smockin.admin.persistence.enums.RestMethodEnum;
import com.smockin.mockserver.service.dto.HttpProxiedDTO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class HttpProxiedControllerE2ETest extends E2ETestBase {

    @Test
    void create_returnsNotFoundForUnknownProxy() {
        HttpProxiedDTO dto = new HttpProxiedDTO(RestMethodEnum.GET, 200, "application/json", "{\"ok\":true}");

        ResponseEntity<String> response = restTemplate.postForEntity(
                url("/proxy/missing"),
                dto,
                String.class);

        Assertions.assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }
}
