package com.smockin.admin.controller;

import com.smockin.admin.exception.RecordNotFoundException;
import com.smockin.admin.exception.ValidationException;
import com.smockin.mockserver.exception.MockServerException;
import com.smockin.mockserver.service.WebSocketService;
import com.smockin.mockserver.service.dto.PushClientDTO;
import com.smockin.mockserver.service.dto.WebSocketDTO;
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
public class WebSocketController {

    private final WebSocketService webSocketService;

    public WebSocketController(WebSocketService webSocketService) {
        this.webSocketService = webSocketService;
    }

    @GetMapping(path="/ws/{id}/client", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<PushClientDTO>> getClients(@PathVariable final String id,
                                                                        @RequestHeader(value = GeneralUtils.OAUTH_HEADER_NAME, required = false) final String bearerToken)
                                                                            throws RecordNotFoundException, ValidationException {

        return new ResponseEntity<>(webSocketService.getClientConnections(id, GeneralUtils.extractOAuthToken(bearerToken)), HttpStatus.OK);
    }

    @PostMapping(path="/ws/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> sendMessage(@PathVariable final String id,
                                                          @RequestBody final WebSocketDTO dto)
                                                            throws MockServerException {

        webSocketService.sendMessage(id, dto);

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

}
