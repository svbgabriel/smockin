package com.smockin.admin.service;

import com.smockin.admin.dto.ProjectDTO;
import com.smockin.admin.exception.RecordNotFoundException;
import com.smockin.admin.exception.ValidationException;
import com.smockin.admin.persistence.dao.RestfulProjectDAO;
import com.smockin.admin.persistence.entity.RestfulProject;
import com.smockin.admin.service.mapper.ProjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock
    private RestfulProjectDAO restfulProjectDAO;

    @Spy
    private ProjectMapper projectMapper = new ProjectMapper();

    @InjectMocks
    private ProjectServiceImpl projectService;

    @Test
    void create_returnsExternalId() throws RecordNotFoundException, ValidationException {

        // Setup
        final ProjectDTO dto = new ProjectDTO(null, "Project A");

        final RestfulProject saved = new RestfulProject();
        saved.setExtId("proj-1");

        Mockito.when(restfulProjectDAO.save(Mockito.any(RestfulProject.class))).thenReturn(saved);

        // Test
        final String result = projectService.create(dto, "token");

        // Assertions
        Assertions.assertEquals("proj-1", result);
    }
}
