package com.smockin.admin.controller;

import com.smockin.admin.dto.ProjectDTO;
import com.smockin.admin.dto.response.SimpleMessageResponseDTO;
import com.smockin.admin.exception.RecordNotFoundException;
import com.smockin.admin.exception.ValidationException;
import com.smockin.admin.service.ProjectService;
import com.smockin.utils.GeneralUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * Created by mgallina.
 */
@RestController
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @PostMapping(path="/project", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<SimpleMessageResponseDTO<String>> create(@RequestBody final ProjectDTO dto,
                                                                                 @RequestHeader(value = GeneralUtils.OAUTH_HEADER_NAME, required = false) final String bearerToken)
                                                                                    throws RecordNotFoundException, ValidationException {
        return new ResponseEntity<>(new SimpleMessageResponseDTO<>(projectService.create(dto, GeneralUtils.extractOAuthToken(bearerToken))), HttpStatus.CREATED);
    }

    @PutMapping(path = "/project/{extId}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> update(@PathVariable final String extId,
                                         @RequestBody final ProjectDTO dto,
                                         @RequestHeader(value = GeneralUtils.OAUTH_HEADER_NAME, required = false) final String bearerToken)
                                                            throws RecordNotFoundException, ValidationException {
        projectService.update(extId, dto, GeneralUtils.extractOAuthToken(bearerToken));
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @DeleteMapping(path = "/project/{extId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> delete(@PathVariable final String extId,
                                         @RequestHeader(value = GeneralUtils.OAUTH_HEADER_NAME, required = false) final String bearerToken)
                                                            throws RecordNotFoundException {
        projectService.delete(extId, GeneralUtils.extractOAuthToken(bearerToken));
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @GetMapping(path="/project", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<ProjectDTO>> get(@RequestHeader(value = GeneralUtils.OAUTH_HEADER_NAME, required = false) final String bearerToken)
                                                                                throws RecordNotFoundException {
        return new ResponseEntity<>(projectService.loadAll(GeneralUtils.extractOAuthToken(bearerToken)), HttpStatus.OK);
    }

}

