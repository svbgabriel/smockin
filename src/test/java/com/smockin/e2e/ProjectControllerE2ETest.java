package com.smockin.e2e;

import com.smockin.E2ETestBase;
import com.smockin.admin.dto.ProjectDTO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

class ProjectControllerE2ETest extends E2ETestBase {

    @Test
    void get_returnsEmptyListWhenNoProjectsExist() {
        ResponseEntity<List<ProjectDTO>> response = restTemplate.exchange(
                url("/project"),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<ProjectDTO>>() {});

        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
        Assertions.assertNotNull(response.getBody());
        Assertions.assertTrue(response.getBody().isEmpty());
    }
}
