package com.smockin.admin.controller;

import com.smockin.admin.dto.PasswordDTO;
import com.smockin.admin.dto.PasswordResetDTO;
import com.smockin.admin.dto.SmockinNewUserDTO;
import com.smockin.admin.dto.SmockinUserDTO;
import com.smockin.admin.dto.response.SimpleMessageResponseDTO;
import com.smockin.admin.dto.response.SmockinUserResponseDTO;
import com.smockin.admin.enums.UserModeEnum;
import com.smockin.admin.exception.AuthException;
import com.smockin.admin.exception.RecordNotFoundException;
import com.smockin.admin.exception.ValidationException;
import com.smockin.admin.service.SmockinUserService;
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
public class SmockinUserController {

    private final SmockinUserService smockinUserService;

    public SmockinUserController(SmockinUserService smockinUserService) {
        this.smockinUserService = smockinUserService;
    }

    @GetMapping(path = "/user", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<SmockinUserResponseDTO>> getUsers(@RequestHeader(GeneralUtils.OAUTH_HEADER_NAME) final String bearerToken)
            throws RecordNotFoundException, AuthException {

        return ResponseEntity.ok(smockinUserService.loadAllUsers(GeneralUtils.extractOAuthToken(bearerToken)));
    }

    @PostMapping(path = "/user", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> createUser(@RequestBody final SmockinNewUserDTO dto,
                                           @RequestHeader(GeneralUtils.OAUTH_HEADER_NAME) final String bearerToken)
            throws ValidationException, RecordNotFoundException, AuthException {

        smockinUserService.createUser(dto, GeneralUtils.extractOAuthToken(bearerToken));

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PutMapping(path = "/user/{extId}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> updateUser(@PathVariable final String extId,
                                           @RequestBody final SmockinUserDTO dto,
                                           @RequestHeader(GeneralUtils.OAUTH_HEADER_NAME) final String bearerToken)
            throws ValidationException, RecordNotFoundException, AuthException {

        smockinUserService.updateUser(extId, dto, GeneralUtils.extractOAuthToken(bearerToken));

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping(path = "/user/{extId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> deleteUser(@PathVariable final String extId,
                                           @RequestHeader(GeneralUtils.OAUTH_HEADER_NAME) final String bearerToken)
            throws ValidationException, RecordNotFoundException, AuthException {

        smockinUserService.deleteUser(extId, GeneralUtils.extractOAuthToken(bearerToken));

        return ResponseEntity.noContent().build();
    }

    @PatchMapping(path = "/user/password", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> updateUserPassword(@RequestBody final PasswordDTO dto,
                                                   @RequestHeader(GeneralUtils.OAUTH_HEADER_NAME) final String bearerToken)
            throws ValidationException, RecordNotFoundException {

        smockinUserService.updateUserPassword(dto, GeneralUtils.extractOAuthToken(bearerToken));

        return ResponseEntity.noContent().build();
    }

    @GetMapping(path = "/user/{extId}/password/reset", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<SimpleMessageResponseDTO<String>> createPasswordResetToken(@PathVariable final String extId,
                                                                                     @RequestHeader(GeneralUtils.OAUTH_HEADER_NAME) final String bearerToken)
            throws RecordNotFoundException, AuthException {

        return ResponseEntity.ok(new SimpleMessageResponseDTO<>(smockinUserService.issuePasswordResetToken(extId, GeneralUtils.extractOAuthToken(bearerToken))));
    }

    @GetMapping(path = "/password/reset/token/{token}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> validatePasswordResetToken(@PathVariable final String token)
            throws RecordNotFoundException {

        smockinUserService.validatePasswordResetToken(token);

        return ResponseEntity.noContent().build();
    }

    @PostMapping(path = "/password/reset/token/{token}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> applyPasswordResetToken(@PathVariable final String token,
                                                        @RequestBody final PasswordResetDTO dto)
            throws RecordNotFoundException, ValidationException {

        smockinUserService.applyPasswordResetToken(token, dto.getNewPassword());

        return ResponseEntity.noContent().build();
    }

    @GetMapping(path = "/user/mode", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<SimpleMessageResponseDTO<UserModeEnum>> getUserMode() {
        return ResponseEntity.ok(new SimpleMessageResponseDTO<>(smockinUserService.getUserMode()));
    }

}
