package com.smockin.admin.service;

import com.smockin.admin.exception.AuthException;
import com.smockin.admin.exception.RecordNotFoundException;
import com.smockin.admin.exception.ValidationException;
import com.smockin.admin.persistence.dao.ServerConfigDAO;
import com.smockin.admin.persistence.entity.ServerConfig;
import com.smockin.admin.persistence.enums.ServerTypeEnum;
import com.smockin.admin.service.utils.UserTokenServiceUtils;
import com.smockin.mockserver.dto.MockedServerConfigDTO;
import com.smockin.mockserver.exception.MockServerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ServerConfigManager {

    private final Logger logger = LoggerFactory.getLogger(ServerConfigManager.class);

    @Autowired
    private ServerConfigDAO serverConfigDAO;

    @Autowired
    private SmockinUserService smockinUserService;

    @Autowired
    private UserTokenServiceUtils userTokenServiceUtils;

    @Autowired
    private RestServerManager restServerManager;

    @Autowired
    private S3ServerManager s3ServerManager;

    @Autowired
    private MailServerManager mailServerManager;

    public MockedServerConfigDTO loadServerConfig(final ServerTypeEnum serverType) throws RecordNotFoundException {
        final ServerConfig serverConfig = serverConfigDAO.findByServerType(serverType);
        if (serverConfig == null) {
            throw new RecordNotFoundException();
        }
        return new MockedServerConfigDTO(
                serverConfig.getServerType(),
                serverConfig.getPort(),
                serverConfig.getMaxThreads(),
                serverConfig.getMinThreads(),
                serverConfig.getTimeOutMillis(),
                serverConfig.isAutoStart(),
                serverConfig.isProxyMode(),
                serverConfig.getNativeProperties()
        );
    }

    public void saveServerConfig(final ServerTypeEnum serverType, final MockedServerConfigDTO config, final String token)
            throws RecordNotFoundException, AuthException, ValidationException {
        smockinUserService.assertCurrentUserIsAdmin(userTokenServiceUtils.loadCurrentActiveUser(token));
        validateServerConfig(config);
        ServerConfig serverConfig = serverConfigDAO.findByServerType(serverType);
        if (serverConfig == null) {
            serverConfig = new ServerConfig(serverType);
        }
        serverConfig.setPort(config.getPort());
        serverConfig.setMaxThreads(config.getMaxThreads());
        serverConfig.setMinThreads(config.getMinThreads());
        serverConfig.setTimeOutMillis(config.getTimeOutMillis());
        serverConfig.setAutoStart(config.isAutoStart());
        serverConfig.getNativeProperties().clear();
        serverConfig.getNativeProperties().putAll(config.getNativeProperties());
        serverConfigDAO.saveAndFlush(serverConfig);
    }

    public void handleServerAutoStart() {
        serverConfigDAO.findAll()
                .stream()
                .forEach(sc -> {
                    if (sc.isAutoStart()) {
                        try {
                            autoStartManager(sc.getServerType());
                        } catch (MockServerException ex) {
                            logger.error("Error auto starting server type : {}", sc.getServerType(), ex);
                        }
                    }
                });
    }

    public void autoStartManager(final ServerTypeEnum serverType) throws MockServerException {
        if (serverType == null) {
            return;
        }
        switch (serverType) {
            case RESTFUL:
                restServerManager.startRest();
                break;
            case S3:
                s3ServerManager.startS3();
                break;
            case MAIL:
                mailServerManager.startMail();
                break;
            default:
                logger.warn("Found auto start instruction for discontinued server type: {}", serverType);
        }
    }

    public void validateServerConfig(final MockedServerConfigDTO dto) throws ValidationException {
        if (dto == null) {
            throw new ValidationException("config is required");
        }
        if (dto.getPort() == null) {
            throw new ValidationException("'port' config value is required");
        }
        if (dto.getMaxThreads() == null) {
            throw new ValidationException("'maxThreads' config value is required");
        }
        if (dto.getMinThreads() == null) {
            throw new ValidationException("'minThreads' config value is required");
        }
        if (dto.getTimeOutMillis() == null) {
            throw new ValidationException("'timeOutMillis' config value is required");
        }
    }

}
