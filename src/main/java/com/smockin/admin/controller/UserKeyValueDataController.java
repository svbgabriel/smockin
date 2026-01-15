package com.smockin.admin.controller;

import com.smockin.admin.dto.UserKeyValueDataDTO;
import com.smockin.admin.dto.response.SimpleMessageResponseDTO;
import com.smockin.admin.exception.RecordNotFoundException;
import com.smockin.admin.exception.ValidationException;
import com.smockin.admin.service.UserKeyValueDataService;
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
public class UserKeyValueDataController {

    private final UserKeyValueDataService userKeyValueDataService;

    public UserKeyValueDataController(UserKeyValueDataService userKeyValueDataService) {
        this.userKeyValueDataService = userKeyValueDataService;
    }

    @GetMapping(path="/keyvaluedata/{extId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<UserKeyValueDataDTO> get(@PathVariable final String extId,
                                                                 @RequestHeader(value = GeneralUtils.OAUTH_HEADER_NAME, required = false) final String bearerToken)
            throws RecordNotFoundException, ValidationException {
        return new ResponseEntity<>(userKeyValueDataService.loadById(extId, GeneralUtils.extractOAuthToken(bearerToken)), HttpStatus.OK);
    }

    @PostMapping(path="/keyvaluedata", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<SimpleMessageResponseDTO<String>> create(@RequestBody final List<UserKeyValueDataDTO> dtos,
                                                                                 @RequestHeader(value = GeneralUtils.OAUTH_HEADER_NAME, required = false) final String bearerToken)
                                                                                    throws RecordNotFoundException, ValidationException {

        userKeyValueDataService.save(dtos, GeneralUtils.extractOAuthToken(bearerToken));

        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @PutMapping(path = "/keyvaluedata/{extId}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> update(@PathVariable final String extId,
                                                     @RequestBody final UserKeyValueDataDTO dto,
                                                     @RequestHeader(value = GeneralUtils.OAUTH_HEADER_NAME, required = false) final String bearerToken)
            throws RecordNotFoundException, ValidationException {
        userKeyValueDataService.update(extId, dto, GeneralUtils.extractOAuthToken(bearerToken));
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @DeleteMapping(path = "/keyvaluedata/{extId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> delete(@PathVariable final String extId,
                                                       @RequestHeader(value = GeneralUtils.OAUTH_HEADER_NAME, required = false) final String bearerToken)
                                                            throws RecordNotFoundException, ValidationException {
        userKeyValueDataService.delete(extId, GeneralUtils.extractOAuthToken(bearerToken));
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @GetMapping(path="/keyvaluedata", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<UserKeyValueDataDTO>> getAll(@RequestHeader(value = GeneralUtils.OAUTH_HEADER_NAME, required = false) final String bearerToken)
                                                                                throws RecordNotFoundException {
        return new ResponseEntity<>(userKeyValueDataService.loadAll(GeneralUtils.extractOAuthToken(bearerToken)), HttpStatus.OK);
    }

}

