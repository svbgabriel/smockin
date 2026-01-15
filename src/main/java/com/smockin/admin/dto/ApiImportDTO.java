package com.smockin.admin.dto;

import org.springframework.web.multipart.MultipartFile;

public record ApiImportDTO(MultipartFile file, MockImportConfigDTO config) {

}
