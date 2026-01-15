package com.smockin.admin.controller;

import com.smockin.admin.exception.RecordNotFoundException;
import com.smockin.admin.exception.ValidationException;
import com.smockin.mockserver.service.StatefulService;
import com.smockin.utils.GeneralUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Created by mgallina.
 */
@RestController
public class HttpStatefulController {

    private final StatefulService statefulService;

    public HttpStatefulController(StatefulService statefulService) {
        this.statefulService = statefulService;
    }

    @PostMapping(path="/stateful/{extId}/clear", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> clearDataState(@PathVariable final String extId,
                                                             @RequestHeader(value = GeneralUtils.OAUTH_HEADER_NAME, required = false) final String bearerToken)
                                                            throws RecordNotFoundException, ValidationException {

        statefulService.resetState(extId, GeneralUtils.extractOAuthToken(bearerToken));

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

}
