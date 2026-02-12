package com.smockin.admin.service;

import com.smockin.admin.enums.StoreTypeEnum;
import com.smockin.admin.exception.AuthException;
import com.smockin.admin.exception.RecordNotFoundException;
import com.smockin.admin.persistence.dao.MailMockDAO;
import com.smockin.admin.persistence.dao.MailMockMessageDAO;
import com.smockin.admin.persistence.enums.ServerTypeEnum;
import com.smockin.admin.service.utils.UserTokenServiceUtils;
import com.smockin.mockserver.dto.MockServerState;
import com.smockin.mockserver.dto.MockedServerConfigDTO;
import com.smockin.mockserver.engine.MockedMailServerEngine;
import com.smockin.mockserver.exception.MockServerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class MailServerManager {

    private final Logger logger = LoggerFactory.getLogger(MailServerManager.class);

    @Autowired
    private MockedMailServerEngine mockedMailServerEngine;

    @Autowired
    private MailMockDAO mailMockDAO;

    @Autowired
    private MailMockMessageDAO mailMockMessageDAO;

    @Autowired
    private SmockinUserService smockinUserService;

    @Autowired
    private UserTokenServiceUtils userTokenServiceUtils;

    @Autowired
    private ServerConfigManager serverConfigManager;

    public MockedServerConfigDTO startMail(final String token) throws MockServerException, RecordNotFoundException, AuthException {
        smockinUserService.assertCurrentUserIsAdmin(userTokenServiceUtils.loadCurrentActiveUser(token));
        return startMail();
    }

    public MockedServerConfigDTO startMail() throws MockServerException {
        try {
            final MockedServerConfigDTO configDTO = serverConfigManager.loadServerConfig(ServerTypeEnum.MAIL);
            mockedMailServerEngine.start(configDTO, mailMockDAO.findAllActive());
            return configDTO;
        } catch (IllegalArgumentException ex) {
            logger.error("Starting Mail Mocking Engine", ex);
            mockedMailServerEngine.shutdown();
            throw ex;
        } catch (RecordNotFoundException ex) {
            logger.error("Starting Mail Mocking Engine, due to missing mock server config", ex);
            throw new MockServerException("Missing mock Mail server config");
        } catch (MockServerException ex) {
            logger.error("Starting Mail Mocking Engine", ex);
            throw ex;
        }
    }

    public MockedServerConfigDTO restartMail(final String token) throws MockServerException, RecordNotFoundException, AuthException {
        smockinUserService.assertCurrentUserIsAdmin(userTokenServiceUtils.loadCurrentActiveUser(token));
        if (getMailServerState().isRunning()) {
            shutdownMail();
        }
        return startMail();
    }

    public MockServerState getMailServerState() throws MockServerException {
        return mockedMailServerEngine.getCurrentState();
    }

    public void shutdownMail(final String token) throws MockServerException, RecordNotFoundException, AuthException {
        smockinUserService.assertCurrentUserIsAdmin(userTokenServiceUtils.loadCurrentActiveUser(token));
        shutdownMail();
    }

    public void shutdownMail() throws MockServerException {
        try {
            mockedMailServerEngine.shutdown();
        } catch (MockServerException ex) {
            logger.error("Stopping Mail Mocking Engine", ex);
            throw ex;
        }
    }

    public void clearAllMailMessages(final StoreTypeEnum storeType,
                                     final String token) throws AuthException {
        smockinUserService.assertCurrentUserIsAdmin(userTokenServiceUtils.loadCurrentActiveUser(token));
        if (StoreTypeEnum.DB.equals(storeType)) {
            mailMockMessageDAO.deleteAll();
        } else if (StoreTypeEnum.CACHE.equals(storeType)) {
            mockedMailServerEngine.purgeAllMailMessagesForAllInboxes();
        }
    }

}
