package com.smockin.admin.controller;

import com.smockin.admin.dto.MailMockDTO;
import com.smockin.admin.dto.response.MailMockResponseDTO;
import com.smockin.admin.dto.response.MailMockResponseLiteDTO;
import com.smockin.admin.dto.response.SimpleMessageResponseDTO;
import com.smockin.admin.exception.RecordNotFoundException;
import com.smockin.admin.exception.ValidationException;
import com.smockin.admin.service.MailMockService;
import com.smockin.utils.GeneralUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Optional;

@RestController
public class MailMockController {

    private final MailMockService mailMockService;

    public MailMockController(MailMockService mailMockService) {
        this.mailMockService = mailMockService;
    }


    @GetMapping(path="/mailmock", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<MailMockResponseLiteDTO>> getAll(@RequestHeader(value = GeneralUtils.OAUTH_HEADER_NAME, required = false) final String bearerToken)
            throws RecordNotFoundException {

        return ResponseEntity.ok(mailMockService.loadAll(GeneralUtils.extractOAuthToken(bearerToken)));
    }

    @GetMapping(path="/mailmock/{extId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<MailMockResponseDTO> get(@PathVariable final String extId,
                                            @RequestParam(value = "sender", required = false) final String sender,
                                            @RequestParam(value = "subject", required = false) final String subject,
                                            @RequestParam(value = "dateReceived", required = false) final String dateReceived,
                                            @RequestParam(value = "pageStart") final int pageStart,
                                            @RequestParam(value = "search", required = false) final String search,
                                            @RequestHeader(value = GeneralUtils.OAUTH_HEADER_NAME, required = false) final String bearerToken)
            throws RecordNotFoundException {

        return ResponseEntity.ok(mailMockService.loadByIdWithFilteredMessages(
                extId,
                Optional.ofNullable(sender),
                Optional.ofNullable(subject),
                Optional.ofNullable(dateReceived),
                pageStart,
                search,
                GeneralUtils.extractOAuthToken(bearerToken)));
    }

    @PostMapping(path="/mailmock", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<SimpleMessageResponseDTO<String>> create(@RequestBody final MailMockDTO dto,
                                                            @RequestHeader(value = GeneralUtils.OAUTH_HEADER_NAME, required = false) final String bearerToken)
            throws RecordNotFoundException, ValidationException {

        return new ResponseEntity<>(new SimpleMessageResponseDTO<>(mailMockService.create(dto, GeneralUtils.extractOAuthToken(bearerToken))), HttpStatus.CREATED);
    }

    @PutMapping(path="/mailmock/{extId}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> update(@PathVariable final String extId,
                                @RequestBody final MailMockDTO dto,
                                @RequestParam(value = "retainCachedMail", required = false) final Boolean retainCachedMail,
                                @RequestHeader(value = GeneralUtils.OAUTH_HEADER_NAME, required = false) final String bearerToken)
            throws RecordNotFoundException, ValidationException {

        mailMockService.update(extId, dto, retainCachedMail, GeneralUtils.extractOAuthToken(bearerToken));

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping(path="/mailmock/{extId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> delete(@PathVariable final String extId,
                             @RequestHeader(value = GeneralUtils.OAUTH_HEADER_NAME, required = false) final String bearerToken)
            throws RecordNotFoundException {

        mailMockService.delete(extId, GeneralUtils.extractOAuthToken(bearerToken));

        return ResponseEntity.noContent().build();
    }

}
