package com.smockin.admin.controller;

import com.smockin.admin.dto.AuthDTO;
import com.smockin.admin.dto.response.SimpleMessageResponseDTO;
import com.smockin.admin.exception.AuthException;
import com.smockin.admin.exception.RecordNotFoundException;
import com.smockin.admin.exception.ValidationException;
import com.smockin.admin.service.AuthService;
import com.smockin.admin.service.SmockinUserService;
import com.smockin.utils.GeneralUtils;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created by mgallina.
 */
@RestController
public class AuthController {

    private final AuthService authService;
    private final SmockinUserService smockinUserService;

    public AuthController(AuthService authService, SmockinUserService smockinUserService) {
        this.authService = authService;
        this.smockinUserService = smockinUserService;
    }

    @PostMapping(path="/auth", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<SimpleMessageResponseDTO<String>> authenticate(@RequestBody final AuthDTO dto)
                                                                                throws ValidationException, AuthException {
        return ResponseEntity.ok(new SimpleMessageResponseDTO<>(authService.authenticate(dto)));
    }

    @PostMapping(path="/logout", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> logout(@RequestHeader(GeneralUtils.OAUTH_HEADER_NAME) final String bearerToken)
            throws RecordNotFoundException {

        smockinUserService.resetToken(GeneralUtils.extractOAuthToken(bearerToken));

        return ResponseEntity.noContent().build();
    }

}
