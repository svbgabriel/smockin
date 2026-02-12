package com.smockin.admin.controller;

import com.smockin.admin.exception.RecordNotFoundException;
import com.smockin.admin.exception.ValidationException;
import com.smockin.mockserver.service.ServerSideEventService;
import com.smockin.mockserver.service.dto.PushClientDTO;
import com.smockin.mockserver.service.dto.SseMessageDTO;
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
public class SseProxiedController {

    private final ServerSideEventService serverSideEventService;

    public SseProxiedController(ServerSideEventService serverSideEventService) {
        this.serverSideEventService = serverSideEventService;
    }

    @GetMapping(path = "/sse/{id}/client", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<PushClientDTO>> getClients(@PathVariable final String id,
                                                          @RequestHeader(value = GeneralUtils.OAUTH_HEADER_NAME, required = false) final String bearerToken)
            throws RecordNotFoundException, ValidationException {

        return new ResponseEntity<>(serverSideEventService.getClientConnections(id, GeneralUtils.extractOAuthToken(bearerToken)), HttpStatus.OK);
    }

    @PostMapping(path = "/sse/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> send(@PathVariable final String id,
                                     @RequestBody final SseMessageDTO dto) {

        serverSideEventService.addMessage(id, dto);

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

}
