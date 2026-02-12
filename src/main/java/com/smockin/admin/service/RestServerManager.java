package com.smockin.admin.service;

import com.smockin.admin.enums.UserModeEnum;
import com.smockin.admin.exception.AuthException;
import com.smockin.admin.exception.RecordNotFoundException;
import com.smockin.admin.exception.ValidationException;
import com.smockin.admin.persistence.entity.SmockinUser;
import com.smockin.admin.persistence.enums.RestMethodEnum;
import com.smockin.admin.persistence.enums.ServerTypeEnum;
import com.smockin.admin.persistence.enums.SmockinUserRoleEnum;
import com.smockin.admin.service.utils.UserTokenServiceUtils;
import com.smockin.mockserver.dto.MockServerState;
import com.smockin.mockserver.dto.MockedServerConfigDTO;
import com.smockin.mockserver.dto.ProxyForwardConfigCacheDTO;
import com.smockin.mockserver.engine.MockedRestServerEngine;
import com.smockin.mockserver.engine.MockedRestServerEngineUtils;
import com.smockin.mockserver.exception.MockServerException;
import com.smockin.utils.GeneralUtils;
import org.apache.commons.lang3.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RestServerManager {

    private final Logger logger = LoggerFactory.getLogger(RestServerManager.class);

    @Autowired
    private MockedRestServerEngine mockedRestServerEngine;

    @Autowired
    private MockedRestServerEngineUtils mockedRestServerEngineUtils;

    @Autowired
    private SmockinUserService smockinUserService;

    @Autowired
    private UserTokenServiceUtils userTokenServiceUtils;

    @Autowired
    private ServerConfigManager serverConfigManager;

    @Autowired
    private ProxyMappingManager proxyMappingManager;

    public MockedServerConfigDTO startRest(final String token) throws MockServerException, RecordNotFoundException, AuthException {
        smockinUserService.assertCurrentUserIsAdmin(userTokenServiceUtils.loadCurrentActiveUser(token));
        return startRest();
    }

    public MockedServerConfigDTO startRest() throws MockServerException {
        try {
            final MockedServerConfigDTO configDTO = serverConfigManager.loadServerConfig(ServerTypeEnum.RESTFUL);
            final List<ProxyForwardConfigCacheDTO> allProxyForwardConfig = proxyMappingManager.loadAllUserProxyForwardMappings();
            mockedRestServerEngine.start(configDTO, allProxyForwardConfig);
            return configDTO;
        } catch (IllegalArgumentException ex) {
            mockedRestServerEngine.shutdown();
            throw ex;
        } catch (RecordNotFoundException ex) {
            logger.error("Starting REST Mocking Engine, due to missing mock server config", ex);
            throw new MockServerException("Missing mock REST server config");
        } catch (MockServerException ex) {
            logger.error("Starting REST Mocking Engine", ex);
            throw ex;
        }
    }

    public MockedServerConfigDTO restartRest(final String token) throws MockServerException, RecordNotFoundException, AuthException {
        smockinUserService.assertCurrentUserIsAdmin(userTokenServiceUtils.loadCurrentActiveUser(token));
        if (getRestServerState().isRunning()) {
            shutdownRest();
        }
        return startRest();
    }

    public MockServerState getRestServerState() throws MockServerException {
        return mockedRestServerEngine.getCurrentState();
    }

    public void shutdownRest(final String token) throws MockServerException, RecordNotFoundException, AuthException {
        smockinUserService.assertCurrentUserIsAdmin(userTokenServiceUtils.loadCurrentActiveUser(token));
        shutdownRest();
    }

    public void shutdownRest() throws MockServerException {
        try {
            mockedRestServerEngine.shutdown();
        } catch (MockServerException ex) {
            logger.error("Stopping REST Mocking Engine", ex);
            throw ex;
        }
    }

    public void addLiveLoggingPathToBlock(final RestMethodEnum method,
                                          final String path,
                                          final String token) throws ValidationException {
        final SmockinUser user = userTokenServiceUtils.loadCurrentActiveUser(token);
        mockedRestServerEngine.addPathToLiveBlocking(method, amendMultiUserCtxPath(path, user), user.getExtId());
    }

    public void removeLiveLoggingPathToBlock(final RestMethodEnum method,
                                             final String path,
                                             final String token) throws ValidationException {
        final SmockinUser user = userTokenServiceUtils.loadCurrentActiveUser(token);
        final String amendedPath = amendMultiUserCtxPath(path, user);
        mockedRestServerEngine.removePathFromLiveBlocking(method, amendedPath, user.getExtId());
        if (mockedRestServerEngine.countLiveBlockingPathsForUser(method, amendedPath, user.getExtId()) == 0) {
            mockedRestServerEngine.notifyBlockedLiveLoggingCalls(method, amendedPath);
        }
    }

    String amendMultiUserCtxPath(final String path, final SmockinUser user) throws ValidationException {
        if (UserModeEnum.ACTIVE.equals(smockinUserService.getUserMode())) {
            if (SmockinUserRoleEnum.SYS_ADMIN.equals(user.getRole())) {
                return path;
            }
            final String userCtxPathSegment = mockedRestServerEngineUtils.extractMultiUserCtxPathSegment(path);
            if (!Strings.CI.equals(user.getCtxPath(), userCtxPathSegment)
                    && mockedRestServerEngineUtils.isInboundPathMultiUserPath(userCtxPathSegment)) {
                throw new ValidationException("You cannot block another user's mock");
            }
            final String userCtxPath = GeneralUtils.URL_PATH_SEPARATOR + user.getCtxPath();
            return (Strings.CS.startsWith(path, userCtxPath))
                    ? path
                    : userCtxPath + path;
        }
        return path;
    }

}
