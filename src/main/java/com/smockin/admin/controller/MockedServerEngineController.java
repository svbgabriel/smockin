package com.smockin.admin.controller;

import com.smockin.admin.dto.LiveLoggingBlockingEndpointDTO;
import com.smockin.admin.enums.StoreTypeEnum;
import com.smockin.admin.exception.AuthException;
import com.smockin.admin.exception.MockImportException;
import com.smockin.admin.exception.RecordNotFoundException;
import com.smockin.admin.exception.ValidationException;
import com.smockin.admin.persistence.enums.RestMethodEnum;
import com.smockin.admin.persistence.enums.ServerTypeEnum;
import com.smockin.admin.service.MockedServerEngineService;
import com.smockin.mockserver.dto.MockServerState;
import com.smockin.mockserver.dto.MockedServerConfigDTO;
import com.smockin.mockserver.dto.ProxyForwardConfigDTO;
import com.smockin.mockserver.dto.ProxyForwardConfigResponseDTO;
import com.smockin.mockserver.exception.MockServerException;
import com.smockin.utils.GeneralUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

/**
 * Created by mgallina.
 */
@RestController
public class MockedServerEngineController {

    private final MockedServerEngineService mockedServerEngineService;

    public MockedServerEngineController(MockedServerEngineService mockedServerEngineService) {
        this.mockedServerEngineService = mockedServerEngineService;
    }

    //
    // REST Server
    @PostMapping(path = "/mockedserver/rest/start", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<MockedServerConfigDTO> startRest(@RequestHeader(value = GeneralUtils.OAUTH_HEADER_NAME, required = false) final String bearerToken)
            throws MockServerException, RecordNotFoundException, AuthException {
        return new ResponseEntity<>(mockedServerEngineService.startRest(GeneralUtils.extractOAuthToken(bearerToken)), HttpStatus.OK);
    }

    @PostMapping(path = "/mockedserver/rest/stop", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> stopRest(@RequestHeader(value = GeneralUtils.OAUTH_HEADER_NAME, required = false) final String bearerToken)
            throws MockServerException, RecordNotFoundException, AuthException {
        mockedServerEngineService.shutdownRest(GeneralUtils.extractOAuthToken(bearerToken));
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @PostMapping(path = "/mockedserver/rest/restart", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<MockedServerConfigDTO> restartRest(@RequestHeader(value = GeneralUtils.OAUTH_HEADER_NAME, required = false) final String bearerToken)
            throws MockServerException, RecordNotFoundException, AuthException {
        return new ResponseEntity<>(mockedServerEngineService.restartRest(GeneralUtils.extractOAuthToken(bearerToken)), HttpStatus.OK);
    }

    @GetMapping(path = "/mockedserver/rest/status", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<MockServerState> restStatus() throws MockServerException {
        return new ResponseEntity<>(mockedServerEngineService.getRestServerState(), HttpStatus.OK);
    }


    //
    // S3 Server
    @PostMapping(path = "/mockedserver/s3/start", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<MockedServerConfigDTO> startS3(@RequestHeader(value = GeneralUtils.OAUTH_HEADER_NAME, required = false) final String bearerToken)
            throws MockServerException, RecordNotFoundException, AuthException {
        return new ResponseEntity<>(mockedServerEngineService.startS3(GeneralUtils.extractOAuthToken(bearerToken)), HttpStatus.OK);
    }

    @PostMapping(path = "/mockedserver/s3/stop", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> stopS3(@RequestHeader(value = GeneralUtils.OAUTH_HEADER_NAME, required = false) final String bearerToken)
            throws MockServerException, RecordNotFoundException, AuthException {
        mockedServerEngineService.shutdownS3(GeneralUtils.extractOAuthToken(bearerToken));
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @PostMapping(path = "/mockedserver/s3/restart", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<MockedServerConfigDTO> restartS3(@RequestHeader(value = GeneralUtils.OAUTH_HEADER_NAME, required = false) final String bearerToken)
            throws MockServerException, RecordNotFoundException, AuthException {
        return new ResponseEntity<>(mockedServerEngineService.restartS3(GeneralUtils.extractOAuthToken(bearerToken)), HttpStatus.OK);
    }

    @GetMapping(path = "/mockedserver/s3/status", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<MockServerState> s3Status() throws MockServerException {
        return new ResponseEntity<>(mockedServerEngineService.getS3ServerState(), HttpStatus.OK);
    }

    //
    // Mail Server
    @PostMapping(path = "/mockedserver/mail/start", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<MockedServerConfigDTO> startMail(@RequestHeader(value = GeneralUtils.OAUTH_HEADER_NAME, required = false) final String bearerToken)
            throws MockServerException, RecordNotFoundException, AuthException {
        return new ResponseEntity<>(mockedServerEngineService.startMail(GeneralUtils.extractOAuthToken(bearerToken)), HttpStatus.OK);
    }

    @PostMapping(path = "/mockedserver/mail/stop", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> stopMail(@RequestHeader(value = GeneralUtils.OAUTH_HEADER_NAME, required = false) final String bearerToken)
            throws MockServerException, RecordNotFoundException, AuthException {
        mockedServerEngineService.shutdownMail(GeneralUtils.extractOAuthToken(bearerToken));
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @PostMapping(path = "/mockedserver/mail/restart", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<MockedServerConfigDTO> restartMail(@RequestHeader(value = GeneralUtils.OAUTH_HEADER_NAME, required = false) final String bearerToken)
            throws MockServerException, RecordNotFoundException, AuthException {
        return new ResponseEntity<>(mockedServerEngineService.restartMail(GeneralUtils.extractOAuthToken(bearerToken)), HttpStatus.OK);
    }

    @GetMapping(path = "/mockedserver/mail/status", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<MockServerState> mailStatus() throws MockServerException {
        return new ResponseEntity<>(mockedServerEngineService.getMailServerState(), HttpStatus.OK);
    }

    @DeleteMapping(path = "/mockedserver/mail/clear/{storeType}")
    public ResponseEntity<MockServerState> clearAllMail(@PathVariable final String storeType,
                                                        @RequestHeader(value = GeneralUtils.OAUTH_HEADER_NAME, required = false) final String bearerToken)
            throws AuthException {

        mockedServerEngineService.clearAllMailMessages(StoreTypeEnum.toEnum(storeType), GeneralUtils.extractOAuthToken(bearerToken));

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }


    //
    // Server Config
    @GetMapping(path = "/mockedserver/config/{serverType}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<MockedServerConfigDTO> getServerConfig(@PathVariable final String serverType)
            throws ValidationException, RecordNotFoundException {
        final ServerTypeEnum type = convertServerType(serverType);
        return new ResponseEntity<>(mockedServerEngineService.loadServerConfig(type), HttpStatus.OK);
    }

    @PutMapping(path = "/mockedserver/config/{serverType}",
            produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> putServerConfig(@PathVariable final String serverType,
                                                @RequestBody final MockedServerConfigDTO dto,
                                                @RequestHeader(value = GeneralUtils.OAUTH_HEADER_NAME, required = false) final String bearerToken)
            throws RecordNotFoundException, AuthException, ValidationException {
        mockedServerEngineService.saveServerConfig(convertServerType(serverType), dto, GeneralUtils.extractOAuthToken(bearerToken));
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    ServerTypeEnum convertServerType(final String serverType) throws ValidationException {

        try {
            return ServerTypeEnum.valueOf(serverType);
        } catch (Exception ex) {
            throw new ValidationException("Invalid serverType value: " + serverType);
        }

    }


    //
    // Toggle Proxy Forward
    @PutMapping(
            path = "/mockedserver/config/{serverType}/proxy/mode",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> putServerProxyMode(
            @PathVariable final String serverType,
            @RequestParam("enableProxyMode") final boolean enableProxyMode,
            @RequestHeader(value = GeneralUtils.OAUTH_HEADER_NAME, required = false) final String bearerToken)
            throws AuthException {

        mockedServerEngineService.updateProxyMode(enableProxyMode, GeneralUtils.extractOAuthToken(bearerToken));

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }


    //
    // Proxy Forward Export / Import
    @GetMapping(path = "/mockedserver/config/{serverType}/proxy/mappings/export", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> getServerConfigProxyMappingsExport(@PathVariable final String serverType,
                                                                     @RequestHeader(value = GeneralUtils.OAUTH_HEADER_NAME, required = false) final String bearerToken)
            throws AuthException {

        final Optional<String> exportOpt = mockedServerEngineService.exportProxyMappings(GeneralUtils.extractOAuthToken(bearerToken));

        if (!exportOpt.isPresent()) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok()
                .body(exportOpt.get());

    }

    @PostMapping(path = "/mockedserver/config/{serverType}/proxy/mappings/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> postServerConfigProxyMappingsImport(@PathVariable final String serverType,
                                                                      @RequestHeader(value = GeneralUtils.KEEP_EXISTING_HEADER_NAME) final boolean keepExisting,
                                                                      @RequestHeader(value = GeneralUtils.OAUTH_HEADER_NAME, required = false) final String bearerToken,
                                                                      @RequestParam("file") final MultipartFile file)
            throws MockImportException, ValidationException {

        mockedServerEngineService.importProxyMappingsFile(file, keepExisting, GeneralUtils.extractOAuthToken(bearerToken));

        return ResponseEntity.noContent().build();
    }


    //
    // Proxy Forward User Mappings
    @GetMapping(
            path = "/mockedserver/config/{serverType}/user/proxy",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ProxyForwardConfigResponseDTO> getServerConfigUserProxyMappings(
            @PathVariable final String serverType,
            @RequestHeader(value = GeneralUtils.OAUTH_HEADER_NAME, required = false) final String bearerToken)
            throws RecordNotFoundException {

        return new ResponseEntity<>(mockedServerEngineService.loadProxyForwardMappingsForUser(GeneralUtils.extractOAuthToken(bearerToken)), HttpStatus.OK);
    }

    @PostMapping(
            path = "/mockedserver/config/{serverType}/user/proxy",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> postServerConfigUserProxyMappings(
            @PathVariable final String serverType,
            @RequestBody final ProxyForwardConfigDTO proxyForwardConfigDTO,
            @RequestHeader(value = GeneralUtils.OAUTH_HEADER_NAME, required = false) final String bearerToken)
            throws AuthException, RecordNotFoundException, ValidationException {

        mockedServerEngineService.saveProxyForwardMappingsForUser(
                proxyForwardConfigDTO,
                GeneralUtils.extractOAuthToken(bearerToken));

        return new ResponseEntity<>(HttpStatus.OK);
    }


    //
    // Live Logging
    @PostMapping(
            path = "/mockedserver/config/{serverType}/live-logging-block/endpoint",
            produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> addLiveLoggingPathToBlock(@PathVariable final String serverType,
                                                          @RequestBody final LiveLoggingBlockingEndpointDTO liveLoggingBlockingEndpoint,
                                                          @RequestHeader(value = GeneralUtils.OAUTH_HEADER_NAME, required = false) final String bearerToken)
            throws AuthException, ValidationException {

        mockedServerEngineService.addLiveLoggingPathToBlock(
                liveLoggingBlockingEndpoint.getMethod(),
                liveLoggingBlockingEndpoint.getPath(),
                GeneralUtils.extractOAuthToken(bearerToken));

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @DeleteMapping(
            path = "/mockedserver/config/{serverType}/live-logging-block/endpoint",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> removeLiveLoggingPathToBlock(@PathVariable final String serverType,
                                                             @RequestParam("method") final String method,
                                                             @RequestParam("path") final String path,
                                                             @RequestHeader(value = GeneralUtils.OAUTH_HEADER_NAME, required = false) final String bearerToken)
            throws AuthException, ValidationException {

        mockedServerEngineService.removeLiveLoggingPathToBlock(
                RestMethodEnum.findByName(method),
                path,
                GeneralUtils.extractOAuthToken(bearerToken));

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

}
