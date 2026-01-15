package com.smockin.admin.controller;

import com.smockin.admin.exception.RecordNotFoundException;
import com.smockin.admin.exception.ValidationException;
import com.smockin.mockserver.service.dto.HttpProxiedDTO;
import com.smockin.mockserver.service.HttpProxyService;
import com.smockin.utils.GeneralUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Created by mgallina.
 */
@RestController
public class HttpProxiedController {

    private final HttpProxyService httpProxyService;

    public HttpProxiedController(HttpProxyService httpProxyService) {
        this.httpProxyService = httpProxyService;
    }

    @PostMapping(path="/proxy/{extId}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> create(@PathVariable final String extId,
                                    @RequestBody final HttpProxiedDTO dto,
                                    @RequestHeader(value = GeneralUtils.OAUTH_HEADER_NAME, required = false) final String bearerToken)
                                                        throws RecordNotFoundException, ValidationException {

        httpProxyService.addResponse(extId, dto, GeneralUtils.extractOAuthToken(bearerToken));

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @PatchMapping(path="/proxy/{extId}/clear", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> clearSession(@PathVariable final String extId,
                                          @RequestHeader(value = GeneralUtils.OAUTH_HEADER_NAME, required = false) final String bearerToken)
                                                            throws RecordNotFoundException, ValidationException {

        httpProxyService.clearSession(extId, GeneralUtils.extractOAuthToken(bearerToken));

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

}
