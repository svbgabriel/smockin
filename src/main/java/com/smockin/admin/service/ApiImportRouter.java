package com.smockin.admin.service;

import com.smockin.admin.dto.ApiImportDTO;
import com.smockin.admin.enums.ApiImportTypeEnum;
import com.smockin.admin.exception.MockImportException;
import com.smockin.admin.exception.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class ApiImportRouter {

    private final Logger logger = LoggerFactory.getLogger(ApiImportRouter.class);

    private final ApiImportService ramlApiImportService;
    private final ApiImportService openApiImportService;

    public ApiImportRouter(@Qualifier("ramlApiImportService") ApiImportService ramlApiImportService, @Qualifier("openApiImportService") ApiImportService openApiImportService) {
        this.ramlApiImportService = ramlApiImportService;
        this.openApiImportService = openApiImportService;
    }

    public void route(final String importType, final ApiImportDTO dto, final String token) throws MockImportException, ValidationException {
        logger.debug("route called");

        validate(importType, dto, token);

        if (ApiImportTypeEnum.valueOf(importType) == ApiImportTypeEnum.OPENAPI) {
            openApiImportService.importApiDoc(dto, token);
        } else if (ApiImportTypeEnum.valueOf(importType) == ApiImportTypeEnum.RAML) {
            ramlApiImportService.importApiDoc(dto, token);
        } else {
            throw new ValidationException("Unsupported import type");
        }

    }

    void validate(final String importType, final ApiImportDTO dto, final String token) throws ValidationException {

        if (importType == null) {
            throw new ValidationException("Import Type is required");
        }

        try {
            ApiImportTypeEnum.valueOf(importType);
        } catch (Exception ex) {
            throw new ValidationException("Invalid Import Type: " + importType);
        }

        if (dto == null) {
            throw new ValidationException("Inbound dto is undefined");
        }

        if (dto.file() == null) {
            throw new ValidationException("Inbound file (in dto) is undefined");
        }

        if (dto.config() == null) {
            throw new ValidationException("Inbound config (in dto) is undefined");
        }

    }

}
