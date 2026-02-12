package com.smockin.admin.controller;

import com.smockin.admin.dto.HttpClientCallDTO;
import com.smockin.admin.dto.response.HttpClientResponseDTO;
import com.smockin.admin.exception.ValidationException;
import com.smockin.admin.service.HttpClientService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created by mgallina.
 */
@RestController
public class HttpClientController {

    private final HttpClientService httpClientService;

    public HttpClientController(HttpClientService httpClientService) {
        this.httpClientService = httpClientService;
    }

    @PostMapping(path="/httpclientcall", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<HttpClientResponseDTO> httpClientCall(@RequestBody final HttpClientCallDTO httpClientCallDTO) throws ValidationException {
        return new ResponseEntity<>(httpClientService.handleCallToMock(httpClientCallDTO), HttpStatus.OK);
    }

}
