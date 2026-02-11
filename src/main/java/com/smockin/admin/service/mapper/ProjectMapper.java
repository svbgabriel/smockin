package com.smockin.admin.service.mapper;

import com.smockin.admin.dto.ProjectDTO;
import com.smockin.admin.persistence.entity.RestfulProject;
import org.springframework.stereotype.Component;

@Component
public class ProjectMapper {

    public ProjectDTO toProjectDTO(final RestfulProject project) {
        return new ProjectDTO(project.getExtId(), project.getName());
    }

    public RestfulProject toRestfulProject(final ProjectDTO projectDTO) {
        final RestfulProject project = new RestfulProject();
        project.setName(projectDTO.getName());
        return project;
    }

}
