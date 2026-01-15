package com.smockin.admin.controller;

import com.smockin.admin.dto.TunnelRequestDTO;
import com.smockin.admin.dto.response.TunnelResponseDTO;
import com.smockin.admin.exception.AuthException;
import com.smockin.admin.exception.ValidationException;
import com.smockin.admin.service.TunnelService;
import com.smockin.utils.GeneralUtils;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class TunnelController {

    private final TunnelService tunnelService;

    public TunnelController(TunnelService tunnelService) {
        this.tunnelService = tunnelService;
    }

    @GetMapping(path = "/tunnel", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TunnelResponseDTO> get(@RequestHeader(value = GeneralUtils.OAUTH_HEADER_NAME, required = false) final String bearerToken) {

        return ResponseEntity.ok(tunnelService.load(GeneralUtils.extractOAuthToken(bearerToken)));
    }

    @PutMapping(path = "/tunnel",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TunnelResponseDTO> update(@RequestBody final TunnelRequestDTO dto,
                                                    @RequestHeader(value = GeneralUtils.OAUTH_HEADER_NAME, required = false) final String bearerToken)
            throws AuthException, ValidationException {

        return ResponseEntity.ok(tunnelService.update(dto, GeneralUtils.extractOAuthToken(bearerToken)));
    }

}
