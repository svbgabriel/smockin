package com.smockin.mockserver.engine;

import com.smockin.mockserver.dto.ProxyForwardConfigCacheDTO;
import com.smockin.mockserver.dto.ProxyForwardMappingDTO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

class ProxyMappingCacheTest {

    private ProxyMappingCache proxyMappingCache;

    @BeforeEach
    void setUp() {
        proxyMappingCache = new ProxyMappingCache();
    }

    @Test
    void initFiltersDisabledMappings() {

        // Setup
        final ProxyForwardConfigCacheDTO activeConfig = new ProxyForwardConfigCacheDTO();
        activeConfig.setUserCtxPath("user-1");
        activeConfig.setProxyForwardMappings(List.of(
                new ProxyForwardMappingDTO("/path", "http://example.com", false)));

        final ProxyForwardConfigCacheDTO disabledConfig = new ProxyForwardConfigCacheDTO();
        disabledConfig.setUserCtxPath("user-2");
        disabledConfig.setProxyForwardMappings(List.of(
                new ProxyForwardMappingDTO("/path", "http://example.com", true)));

        // Test
        proxyMappingCache.init(List.of(activeConfig, disabledConfig));

        // Assertions
        Assertions.assertTrue(proxyMappingCache.find("user-1").isPresent());
        Assertions.assertTrue(proxyMappingCache.find("user-2").isEmpty());
    }

    @Test
    void updateRemovesWhenNoActiveMappings() {

        // Setup
        final ProxyForwardConfigCacheDTO activeConfig = new ProxyForwardConfigCacheDTO();
        activeConfig.setUserCtxPath("user-1");
        activeConfig.setProxyForwardMappings(List.of(
                new ProxyForwardMappingDTO("/path", "http://example.com", false)));

        proxyMappingCache.init(List.of(activeConfig));

        final ProxyForwardConfigCacheDTO updatedConfig = new ProxyForwardConfigCacheDTO();
        updatedConfig.setUserCtxPath("user-1");
        updatedConfig.setProxyForwardMappings(List.of(
                new ProxyForwardMappingDTO("/path", "http://example.com", true)));

        // Test
        proxyMappingCache.update(updatedConfig);

        // Assertions
        Assertions.assertTrue(proxyMappingCache.find("user-1").isEmpty());
    }
}
