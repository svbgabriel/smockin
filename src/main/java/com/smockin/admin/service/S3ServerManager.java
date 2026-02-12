package com.smockin.admin.service;

import com.smockin.admin.exception.AuthException;
import com.smockin.admin.exception.RecordNotFoundException;
import com.smockin.admin.persistence.dao.S3MockDAO;
import com.smockin.admin.persistence.entity.S3Mock;
import com.smockin.admin.persistence.enums.ServerTypeEnum;
import com.smockin.admin.service.utils.UserTokenServiceUtils;
import com.smockin.mockserver.dto.MockServerState;
import com.smockin.mockserver.dto.MockedServerConfigDTO;
import com.smockin.mockserver.engine.MockedS3ServerEngine;
import com.smockin.mockserver.exception.MockServerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class S3ServerManager {

    private final Logger logger = LoggerFactory.getLogger(S3ServerManager.class);

    @Autowired
    private MockedS3ServerEngine mockedS3ServerEngine;

    @Autowired
    private S3MockDAO s3MockDAO;

    @Autowired
    private SmockinUserService smockinUserService;

    @Autowired
    private UserTokenServiceUtils userTokenServiceUtils;

    @Autowired
    private ServerConfigManager serverConfigManager;

    public MockedServerConfigDTO startS3(final String token) throws MockServerException, RecordNotFoundException, AuthException {
        smockinUserService.assertCurrentUserIsAdmin(userTokenServiceUtils.loadCurrentActiveUser(token));
        return startS3();
    }

    public MockedServerConfigDTO startS3() throws MockServerException {
        try {
            final MockedServerConfigDTO configDTO = serverConfigManager.loadServerConfig(ServerTypeEnum.S3);
            final List<S3Mock> activeMocks = s3MockDAO.findAllActiveBuckets();
            mockedS3ServerEngine.start(configDTO, activeMocks);
            return configDTO;
        } catch (IllegalArgumentException ex) {
            logger.error("Starting S3 Mocking Engine", ex);
            mockedS3ServerEngine.shutdown();
            throw ex;
        } catch (RecordNotFoundException ex) {
            logger.error("Starting S3 Mocking Engine, due to missing mock server config", ex);
            throw new MockServerException("Missing mock S3 server config");
        } catch (MockServerException ex) {
            logger.error("Starting S3 Mocking Engine", ex);
            throw ex;
        }
    }

    public MockedServerConfigDTO restartS3(final String token) throws MockServerException, RecordNotFoundException, AuthException {
        smockinUserService.assertCurrentUserIsAdmin(userTokenServiceUtils.loadCurrentActiveUser(token));
        if (getS3ServerState().isRunning()) {
            shutdownS3();
        }
        return startS3();
    }

    public MockServerState getS3ServerState() throws MockServerException {
        return mockedS3ServerEngine.getCurrentState();
    }

    public void shutdownS3(final String token) throws MockServerException, RecordNotFoundException, AuthException {
        smockinUserService.assertCurrentUserIsAdmin(userTokenServiceUtils.loadCurrentActiveUser(token));
        shutdownS3();
    }

    public void shutdownS3() throws MockServerException {
        try {
            mockedS3ServerEngine.shutdown();
        } catch (MockServerException ex) {
            logger.error("Stopping S3 Mocking Engine", ex);
            throw ex;
        }
    }

}
