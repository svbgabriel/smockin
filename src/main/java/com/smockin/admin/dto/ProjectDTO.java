package com.smockin.admin.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ProjectDTO {

    private String extId;
    private String name;

    public ProjectDTO(String extId, String name) {
        this.extId = extId;
        this.name = name;
    }

}
