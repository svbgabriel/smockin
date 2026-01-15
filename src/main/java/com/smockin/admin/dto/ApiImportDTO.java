package com.smockin.admin.dto;

import lombok.Getter;
import org.springframework.web.multipart.MultipartFile;

@Getter
public class ApiImportDTO {

    private final MultipartFile file;
    private final MockImportConfigDTO config;

    public ApiImportDTO(final MultipartFile file, final MockImportConfigDTO config) {
        this.file = file;
        this.config = config;
    }

}
