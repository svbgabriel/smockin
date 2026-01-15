package com.smockin.admin.controller;

import com.smockin.admin.dto.response.PagingResponseDTO;
import com.smockin.admin.dto.response.SimpleMessageResponseDTO;
import com.smockin.admin.exception.RecordNotFoundException;
import com.smockin.admin.exception.ValidationException;
import com.smockin.admin.service.MailMockMessageService;
import com.smockin.admin.service.MailMockService;
import com.smockin.mockserver.dto.MailServerMessageInboxAttachmentDTO;
import com.smockin.mockserver.dto.MailServerMessageInboxAttachmentLiteDTO;
import com.smockin.mockserver.dto.MailServerMessageInboxDTO;
import com.smockin.utils.GeneralUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
public class MailMockMessageController {

    private final MailMockService mailMockService;
    private final MailMockMessageService mailMockMessageService;

    public MailMockMessageController(MailMockService mailMockService, MailMockMessageService mailMockMessageService) {
        this.mailMockService = mailMockService;
        this.mailMockMessageService = mailMockMessageService;
    }


    @GetMapping(
            path = "/mailmock/{mailExtId}/inbox",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PagingResponseDTO<MailServerMessageInboxDTO>> getInboxMessages(
            @PathVariable final String mailExtId,
            @RequestParam(value = "pageStart") final int pageStart,
            @RequestParam(value = "search", required = false) final String search,
            @RequestHeader(value = GeneralUtils.OAUTH_HEADER_NAME, required = false) final String bearerToken)
            throws RecordNotFoundException, ValidationException {

        return ResponseEntity.ok(mailMockService.loadMessagesFromMailServerInbox(
                mailExtId,
                pageStart,
                search,
                GeneralUtils.extractOAuthToken(bearerToken)));
    }

    @PostMapping(
            path = "/mailmock/{mailExtId}/inbox",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<SimpleMessageResponseDTO<String>> createInboxMessage(
            @PathVariable final String mailExtId,
            @RequestBody final MailServerMessageInboxDTO dto,
            @RequestHeader(value = GeneralUtils.OAUTH_HEADER_NAME, required = false) final String bearerToken)
            throws RecordNotFoundException, ValidationException {

        final String extId = mailMockMessageService.saveMailMessage(mailExtId,
                dto.getFrom(),
                dto.getSubject(),
                dto.getBody(),
                dto.getDateReceived(),
                Optional.of(GeneralUtils.extractOAuthToken(bearerToken)));

        return new ResponseEntity<>(new SimpleMessageResponseDTO<>(extId), HttpStatus.CREATED);
    }

    @DeleteMapping(
            path = "/mailmock/{mailExtId}/inbox/{messageId}",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> deleteInboxMessage(
            @PathVariable final String mailExtId,
            @PathVariable final String messageId,
            @RequestHeader(value = GeneralUtils.OAUTH_HEADER_NAME, required = false) final String bearerToken)
            throws RecordNotFoundException, ValidationException {

        mailMockMessageService.deleteMailMessage(mailExtId, messageId, GeneralUtils.extractOAuthToken(bearerToken));

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping(
            path = "/mailmock/{mailExtId}/inbox",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> deleteAllInboxMessages(
            @PathVariable final String mailExtId,
            @RequestHeader(value = GeneralUtils.OAUTH_HEADER_NAME, required = false) final String bearerToken)
            throws RecordNotFoundException, ValidationException {

        mailMockMessageService.deleteAllMailMessages(mailExtId, GeneralUtils.extractOAuthToken(bearerToken));

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping(
            path = "/mailmock/{mailExtId}/server/inbox",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> deleteAllInboxMessagesOnServer(
            @PathVariable final String mailExtId,
            @RequestHeader(value = GeneralUtils.OAUTH_HEADER_NAME, required = false) final String bearerToken)
            throws RecordNotFoundException, ValidationException {

        mailMockMessageService.deleteAllMailMessagesOnServer(mailExtId, GeneralUtils.extractOAuthToken(bearerToken));

        return ResponseEntity.noContent().build();
    }

    @GetMapping(
            path = "/mailmock/{mailExtId}/message/{messageId}/attachments",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<MailServerMessageInboxAttachmentLiteDTO>> getAllAttachments(
            @PathVariable final String mailExtId,
            @PathVariable final String messageId,
            @RequestHeader(value = GeneralUtils.OAUTH_HEADER_NAME, required = false) final String bearerToken)
            throws RecordNotFoundException, ValidationException {

        return ResponseEntity.ok(mailMockMessageService
                .findAllMessageAttachments(mailExtId, messageId, GeneralUtils.extractOAuthToken(bearerToken)));
    }

    @GetMapping(
            path = "/mailmock/{mailExtId}/message/{messageId}/attachment/{attachmentIdOrName}",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<MailServerMessageInboxAttachmentDTO> getAttachmentByIdOrName(
            @PathVariable final String mailExtId,
            @PathVariable final String messageId,
            @PathVariable final String attachmentIdOrName,
            @RequestHeader(value = GeneralUtils.OAUTH_HEADER_NAME, required = false) final String bearerToken)
            throws RecordNotFoundException, ValidationException {

        return ResponseEntity.ok(mailMockMessageService
                .findMessageAttachment(
                        mailExtId,
                        messageId,
                        attachmentIdOrName,
                        GeneralUtils.extractOAuthToken(bearerToken)));
    }

}
