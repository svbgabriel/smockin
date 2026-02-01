package com.smockin.e2e;

import com.smockin.E2ETestBase;
import com.smockin.admin.dto.HttpClientCallDTO;
import com.smockin.admin.dto.response.HttpClientResponseDTO;
import com.smockin.admin.persistence.enums.RestMethodEnum;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class HttpClientControllerE2ETest extends E2ETestBase {

    @Test
    void httpClientCall_returnsNotFoundWhenMockServerIsStopped() {
        HttpClientCallDTO request = new HttpClientCallDTO("/status", RestMethodEnum.GET);

        ResponseEntity<HttpClientResponseDTO> response = restTemplate.postForEntity(
                url("/httpclientcall"),
                request,
                HttpClientResponseDTO.class);

        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
        Assertions.assertNotNull(response.getBody());
        Assertions.assertEquals(404, response.getBody().getStatus());
    }
}
