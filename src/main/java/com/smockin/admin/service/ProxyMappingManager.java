package com.smockin.admin.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.smockin.admin.exception.*;
import com.smockin.admin.persistence.dao.ProxyForwardUserConfigDAO;
import com.smockin.admin.persistence.dao.ServerConfigDAO;
import com.smockin.admin.persistence.entity.ProxyForwardMapping;
import com.smockin.admin.persistence.entity.ProxyForwardUserConfig;
import com.smockin.admin.persistence.entity.ServerConfig;
import com.smockin.admin.persistence.entity.SmockinUser;
import com.smockin.admin.persistence.enums.ProxyModeTypeEnum;
import com.smockin.admin.persistence.enums.ServerTypeEnum;
import com.smockin.admin.service.utils.UserTokenServiceUtils;
import com.smockin.mockserver.dto.ProxyForwardConfigCacheDTO;
import com.smockin.mockserver.dto.ProxyForwardConfigDTO;
import com.smockin.mockserver.dto.ProxyForwardConfigResponseDTO;
import com.smockin.mockserver.dto.ProxyForwardMappingDTO;
import com.smockin.mockserver.engine.MockedRestServerEngine;
import com.smockin.mockserver.engine.ProxyMappingCache;
import com.smockin.utils.GeneralUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Optional;

@Component
public class ProxyMappingManager {

    @Autowired
    private ProxyForwardUserConfigDAO proxyForwardUserConfigDAO;

    @Autowired
    private ServerConfigDAO serverConfigDAO;

    @Autowired
    private SmockinUserService smockinUserService;

    @Autowired
    private UserTokenServiceUtils userTokenServiceUtils;

    @Autowired
    private ProxyMappingCache proxyMappingCache;

    @Autowired
    private MockedRestServerEngine mockedRestServerEngine;

    public boolean isProxyModeEnabled() {
        final ServerConfig serverConfig = serverConfigDAO.findByServerType(ServerTypeEnum.RESTFUL);
        if (serverConfig == null) {
            throw new RecordNotFoundException();
        }
        return serverConfig.isProxyMode();
    }

    public void updateProxyMode(final boolean enableProxyMode, final String token) throws AuthException {
        smockinUserService.assertCurrentUserIsAdmin(userTokenServiceUtils.loadCurrentActiveUser(token));
        final ServerConfig serverConfig = serverConfigDAO.findByServerType(ServerTypeEnum.RESTFUL);
        if (serverConfig == null) {
            throw new RecordNotFoundException();
        }
        if (serverConfig.isProxyMode() == enableProxyMode) {
            return;
        }
        serverConfig.setProxyMode(enableProxyMode);
        serverConfigDAO.save(serverConfig);
        mockedRestServerEngine.updateProxyMode(enableProxyMode);
        if (enableProxyMode) {
            proxyMappingCache.init(loadAllUserProxyForwardMappings());
        }
    }

    public List<ProxyForwardConfigCacheDTO> loadAllUserProxyForwardMappings() {
        return proxyForwardUserConfigDAO.findAll().stream()
                .map(this::toProxyForwardConfigCacheDTO)
                .toList();
    }

    public ProxyForwardConfigCacheDTO toProxyForwardConfigCacheDTO(final ProxyForwardUserConfig pm) {
        final ProxyForwardConfigCacheDTO dto = new ProxyForwardConfigCacheDTO(
                pm.getCreatedBy().getExtId(),
                pm.getCreatedBy().getCtxPath());
        dto.setProxyModeType(pm.getProxyModeType());
        dto.setDoNotForwardWhen404Mock(pm.isDoNotForwardWhen404Mock());
        dto.setProxyForwardMappings(
                pm.getProxyForwardMappings()
                        .stream()
                        .map(m ->
                                new ProxyForwardMappingDTO(
                                        m.getPath(),
                                        m.getProxyForwardUrl(),
                                        m.isDisabled()))
                        .toList());
        return dto;
    }

    public ProxyForwardConfigResponseDTO loadProxyForwardMappingsForUser(final String token) {
        final SmockinUser smockinUser = userTokenServiceUtils.loadCurrentActiveUser(token);
        ProxyForwardUserConfig proxyForwardUserConfig
                = proxyForwardUserConfigDAO.findByUser(smockinUser.getId());
        if (proxyForwardUserConfig == null) {
            proxyForwardUserConfig = buildNewProxyForwardUserConfig(smockinUser);
            proxyForwardUserConfig.setProxyModeType(ProxyModeTypeEnum.ACTIVE);
            proxyForwardUserConfig.setDoNotForwardWhen404Mock(false);
            saveUserProxyMappings(proxyForwardUserConfig, List.of());
        }
        return new ProxyForwardConfigResponseDTO(
                isProxyModeEnabled(),
                proxyForwardUserConfig.getProxyModeType(),
                proxyForwardUserConfig.isDoNotForwardWhen404Mock(),
                proxyForwardUserConfig.getProxyForwardMappings()
                        .stream()
                        .map(m ->
                                new ProxyForwardMappingDTO(
                                        m.getPath(),
                                        m.getProxyForwardUrl(),
                                        m.isDisabled()))
                        .toList());
    }

    public void saveProxyForwardMappingsForUser(
            final ProxyForwardConfigDTO proxyForwardConfigDTO,
            final String token)
            throws ValidationException, RecordNotFoundException {
        if (!proxyForwardConfigDTO.getProxyForwardMappings().isEmpty()) {
            if (proxyForwardConfigDTO.getProxyModeType() == null) {
                throw new ValidationException("Proxy Mode Type is required");
            }
            validateProxyMappings(proxyForwardConfigDTO.getProxyForwardMappings());
        }
        final SmockinUser smockinUser = userTokenServiceUtils.loadCurrentActiveUser(token);
        ProxyForwardUserConfig proxyForwardUserConfig
                = proxyForwardUserConfigDAO.findByUser(smockinUser.getId());
        if (proxyForwardUserConfig == null) {
            proxyForwardUserConfig = buildNewProxyForwardUserConfig(smockinUser);
        }
        proxyForwardUserConfig.setProxyModeType(proxyForwardConfigDTO.getProxyModeType());
        proxyForwardUserConfig.setDoNotForwardWhen404Mock(proxyForwardConfigDTO.isDoNotForwardWhen404Mock());
        saveUserProxyMappings(proxyForwardUserConfig, proxyForwardConfigDTO.getProxyForwardMappings());
        final ProxyForwardConfigCacheDTO cacheDTO = new ProxyForwardConfigCacheDTO(smockinUser.getExtId(), smockinUser.getCtxPath());
        cacheDTO.setProxyModeType(proxyForwardConfigDTO.getProxyModeType());
        cacheDTO.setDoNotForwardWhen404Mock(proxyForwardConfigDTO.isDoNotForwardWhen404Mock());
        cacheDTO.setProxyForwardMappings(proxyForwardConfigDTO.getProxyForwardMappings());
        if (isProxyModeEnabled()) {
            proxyMappingCache.update(cacheDTO);
        }
    }

    public ProxyForwardUserConfig buildNewProxyForwardUserConfig(final SmockinUser smockinUser) {
        final ServerConfig serverConfig = serverConfigDAO.findByServerType(ServerTypeEnum.RESTFUL);
        if (serverConfig == null) {
            throw new RecordNotFoundException();
        }
        final ProxyForwardUserConfig proxyForwardUserConfig = new ProxyForwardUserConfig();
        proxyForwardUserConfig.setCreatedBy(smockinUser);
        proxyForwardUserConfig.setServerConfig(serverConfig);
        return proxyForwardUserConfig;
    }

    public Optional<String> exportProxyMappings(final String token) {
        final SmockinUser smockinUser = userTokenServiceUtils.loadCurrentActiveUser(token);
        final ProxyForwardUserConfig proxyForwardUserConfig = proxyForwardUserConfigDAO.findByUser(smockinUser.getId());
        final List<ProxyForwardMappingDTO> dtos = proxyForwardUserConfig.getProxyForwardMappings()
                .stream()
                .map(m ->
                        new ProxyForwardMappingDTO(m.getPath(), m.getProxyForwardUrl(), m.isDisabled()))
                .toList();
        if (dtos.isEmpty()) {
            return Optional.empty();
        }
        final String exportContent = GeneralUtils.serialiseJson(dtos);
        final byte[] exportBytes = exportContent.getBytes();
        return Optional.of(GeneralUtils.base64Encode(exportBytes));
    }

    public String importProxyMappingsFile(final MultipartFile file, final boolean keepExisting, final String token)
            throws MockImportException, ValidationException {
        final SmockinUser smockinUser = userTokenServiceUtils.loadCurrentActiveUser(token);
        final ProxyForwardUserConfig proxyForwardUserConfig = proxyForwardUserConfigDAO.findByUser(smockinUser.getId());
        try {
            final ByteArrayInputStream stream = new ByteArrayInputStream(file.getBytes());
            final String content = IOUtils.toString(stream, Charset.defaultCharset().displayName());
            List<ProxyForwardMappingDTO> proxyForwardMappingDTOs = GeneralUtils.deserializeJson(content, new TypeReference<List<ProxyForwardMappingDTO>>() {
            });
            if (proxyForwardMappingDTOs == null) {
                throw new ValidationException("Error reading import file: invalid json structure");
            }
            validateProxyMappings(proxyForwardMappingDTOs);
            if (!keepExisting) {
                proxyForwardUserConfig.getProxyForwardMappings().clear();
                proxyForwardUserConfigDAO.saveAndFlush(proxyForwardUserConfig);
            } else {
                final List<String> paths = proxyForwardUserConfig.getProxyForwardMappings()
                        .stream()
                        .map(p -> p.getPath().toLowerCase())
                        .toList();
                proxyForwardMappingDTOs = proxyForwardMappingDTOs
                        .stream()
                        .filter(p -> !paths.contains(p.getPath().toLowerCase()))
                        .toList();
            }
            saveUserProxyMappings(proxyForwardUserConfig, proxyForwardMappingDTOs);
        } catch (IOException ex) {
            throw new MockExportException("Error importing proxy mappings file");
        }
        return null;
    }

    public void saveUserProxyMappings(final ProxyForwardUserConfig proxyForwardUserConfig,
                                      final List<ProxyForwardMappingDTO> proxyForwardMappings) {
        proxyForwardUserConfig.getProxyForwardMappings().clear();
        proxyForwardUserConfigDAO.saveAndFlush(proxyForwardUserConfig);
        if (!proxyForwardMappings.isEmpty()) {
            proxyForwardUserConfig.getProxyForwardMappings().addAll(
                    proxyForwardMappings
                            .stream()
                            .map(dto -> toProxyForwardMapping(dto, proxyForwardUserConfig))
                            .toList());
        }
        proxyForwardUserConfigDAO.save(proxyForwardUserConfig);
    }

    public ProxyForwardMapping toProxyForwardMapping(final ProxyForwardMappingDTO proxyForwardMappingDTO,
                                                     final ProxyForwardUserConfig proxyForwardUserConfig) {
        final ProxyForwardMapping proxyForwardMapping = new ProxyForwardMapping();
        proxyForwardMapping.setProxyForwardUserConfig(proxyForwardUserConfig);
        proxyForwardMapping.setPath(
                (!Strings.CS.startsWith(proxyForwardMappingDTO.getPath(), GeneralUtils.URL_PATH_SEPARATOR)
                        && !Strings.CS.equals(proxyForwardMappingDTO.getPath(), GeneralUtils.PATH_WILDCARD)) ? GeneralUtils.URL_PATH_SEPARATOR : proxyForwardMappingDTO.getPath());
        proxyForwardMapping.setProxyForwardUrl(proxyForwardMappingDTO.getProxyForwardUrl());
        proxyForwardMapping.setDisabled(proxyForwardMappingDTO.isDisabled());
        return proxyForwardMapping;
    }

    public void validateProxyMappings(final List<ProxyForwardMappingDTO> proxyForwardMappings)
            throws ValidationException {
        for (ProxyForwardMappingDTO dto : proxyForwardMappings) {
            if (StringUtils.isBlank(dto.getPath())) {
                throw new ValidationException("A 'Path' value is missing");
            }
            if (StringUtils.isBlank(dto.getProxyForwardUrl())) {
                throw new ValidationException("A 'Proxy Forward Url' value is missing");
            }
            if (!dto.getProxyForwardUrl().startsWith(HttpClientService.HTTPS_PROTOCOL)
                    && !dto.getProxyForwardUrl().startsWith(HttpClientService.HTTP_PROTOCOL)) {
                throw new ValidationException("The 'Proxy Forward Url' value '" + dto.getProxyForwardUrl() + "' is invalid");
            }
        }
    }

}
