package com.smockin.admin.service;

import com.smockin.admin.enums.StoreTypeEnum;
import com.smockin.admin.exception.AuthException;
import com.smockin.admin.exception.MockImportException;
import com.smockin.admin.exception.RecordNotFoundException;
import com.smockin.admin.exception.ValidationException;
import com.smockin.admin.persistence.enums.RestMethodEnum;
import com.smockin.admin.persistence.enums.ServerTypeEnum;
import com.smockin.mockserver.dto.MockServerState;
import com.smockin.mockserver.dto.MockedServerConfigDTO;
import com.smockin.mockserver.dto.ProxyForwardConfigDTO;
import com.smockin.mockserver.dto.ProxyForwardConfigResponseDTO;
import com.smockin.mockserver.exception.MockServerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

/**
 * Created by mgallina.
 */
@Service
@Transactional
public class MockedServerEngineServiceImpl implements MockedServerEngineService {

    @Autowired
    private RestServerManager restServerManager;

    @Autowired
    private S3ServerManager s3ServerManager;

    @Autowired
    private MailServerManager mailServerManager;

    @Autowired
    private ServerConfigManager serverConfigManager;

    @Autowired
    private ProxyMappingManager proxyMappingManager;


    //
    // Rest
    @Override
    public MockedServerConfigDTO startRest(final String token) throws MockServerException, RecordNotFoundException, AuthException {
        return restServerManager.startRest(token);
    }

    @Override
    public MockedServerConfigDTO restartRest(final String token) throws MockServerException, RecordNotFoundException, AuthException {
        return restServerManager.restartRest(token);
    }

    @Override
    public MockServerState getRestServerState() throws MockServerException {
        return restServerManager.getRestServerState();
    }

    @Override
    public void shutdownRest(final String token) throws MockServerException, RecordNotFoundException, AuthException {
        restServerManager.shutdownRest(token);
    }


    //
    // S3
    @Override
    public MockedServerConfigDTO startS3(final String token) throws MockServerException, RecordNotFoundException, AuthException {
        return s3ServerManager.startS3(token);
    }

    @Override
    public MockedServerConfigDTO restartS3(final String token) throws MockServerException, RecordNotFoundException, AuthException {
        return s3ServerManager.restartS3(token);
    }

    @Override
    public MockServerState getS3ServerState() throws MockServerException {
        return s3ServerManager.getS3ServerState();
    }

    @Override
    public void shutdownS3(final String token) throws MockServerException, RecordNotFoundException, AuthException {
        s3ServerManager.shutdownS3(token);
    }


    //
    // Mail
    @Override
    public MockedServerConfigDTO startMail(final String token) throws MockServerException, RecordNotFoundException, AuthException {
        return mailServerManager.startMail(token);
    }

    @Override
    public MockedServerConfigDTO restartMail(final String token) throws MockServerException, RecordNotFoundException, AuthException {
        return mailServerManager.restartMail(token);
    }

    @Override
    public MockServerState getMailServerState() throws MockServerException {
        return mailServerManager.getMailServerState();
    }

    @Override
    public void shutdownMail(final String token) throws MockServerException, RecordNotFoundException, AuthException {
        mailServerManager.shutdownMail(token);
    }


    //
    // Config
    @Override
    public MockedServerConfigDTO loadServerConfig(final ServerTypeEnum serverType) throws RecordNotFoundException {
        return serverConfigManager.loadServerConfig(serverType);
    }

    @Override
    public void saveServerConfig(final ServerTypeEnum serverType, final MockedServerConfigDTO config, final String token)
            throws RecordNotFoundException, AuthException, ValidationException {
        serverConfigManager.saveServerConfig(serverType, config, token);
    }

    @Override
    public void handleServerAutoStart() {
        serverConfigManager.handleServerAutoStart();
    }

    @Override
    public void updateProxyMode(final boolean enableProxyMode, final String token) throws AuthException {
        proxyMappingManager.updateProxyMode(enableProxyMode, token);
    }

    @Override
    public ProxyForwardConfigResponseDTO loadProxyForwardMappingsForUser(final String token) {
        return proxyMappingManager.loadProxyForwardMappingsForUser(token);
    }

    @Override
    public void saveProxyForwardMappingsForUser(
            final ProxyForwardConfigDTO proxyForwardConfigDTO,
            final String token)
            throws ValidationException, RecordNotFoundException {
        proxyMappingManager.saveProxyForwardMappingsForUser(proxyForwardConfigDTO, token);
    }

    @Override
    public void addLiveLoggingPathToBlock(final RestMethodEnum method,
                                          final String path,
                                          final String token) throws ValidationException {
        restServerManager.addLiveLoggingPathToBlock(method, path, token);
    }

    @Override
    public void removeLiveLoggingPathToBlock(final RestMethodEnum method,
                                             final String path,
                                             final String token) throws ValidationException {
        restServerManager.removeLiveLoggingPathToBlock(method, path, token);
    }

    @Override
    public Optional<String> exportProxyMappings(final String token) {
        return proxyMappingManager.exportProxyMappings(token);
    }

    @Override
    public String importProxyMappingsFile(final MultipartFile file, final boolean keepExisting, final String token)
            throws MockImportException, ValidationException {
        return proxyMappingManager.importProxyMappingsFile(file, keepExisting, token);
    }

    @Override
    public void clearAllMailMessages(final StoreTypeEnum storeType,
                                     final String token) throws AuthException {
        mailServerManager.clearAllMailMessages(storeType, token);
    }

}
