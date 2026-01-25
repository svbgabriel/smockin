package com.smockin.utils;

import com.smockin.admin.dto.response.LiveLoggingContentDTO;
import com.smockin.admin.dto.response.LiveLoggingDTO;
import com.smockin.admin.dto.response.LiveLoggingInboundContentDTO;
import com.smockin.admin.dto.response.LiveLoggingOutboundContentDTO;
import com.smockin.admin.dto.response.LiveLoggingS3DTO;
import com.smockin.admin.dto.response.LiveLoggingTrafficDTO;
import com.smockin.admin.enums.LiveLoggingDirectionEnum;
import com.smockin.admin.enums.LiveLoggingMessageTypeEnum;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

class LiveLoggingUtilsTest {

    @Test
    void buildLiveLogInterceptedResponseDTO_defaultsBody_Test() {

        // Setup
        final Map<String, String> headers = new HashMap<>();
        headers.put("X-Test", "true");

        // Test
        final LiveLoggingDTO result = LiveLoggingUtils.buildLiveLogInterceptedResponseDTO(
                "req-1", "http://example.com", 200, headers, " ", true);

        // Assertions
        Assertions.assertEquals(LiveLoggingMessageTypeEnum.BLOCKED_RESPONSE, result.getType());
        Assertions.assertTrue(result.getPayload() instanceof LiveLoggingTrafficDTO);

        final LiveLoggingTrafficDTO payload = (LiveLoggingTrafficDTO) result.getPayload();
        Assertions.assertEquals("req-1", payload.getId());
        Assertions.assertEquals(LiveLoggingDirectionEnum.RESPONSE, payload.getDirection());
        Assertions.assertTrue(payload.isProxied());
        Assertions.assertNotNull(payload.getDate());

        final LiveLoggingContentDTO content = payload.getContent();
        Assertions.assertTrue(content instanceof LiveLoggingOutboundContentDTO);
        Assertions.assertEquals("http://example.com", content.getUrl());
        Assertions.assertEquals(headers, content.getHeaders());
        Assertions.assertEquals("n/a", content.getBody());
        Assertions.assertEquals(Integer.valueOf(200), ((LiveLoggingOutboundContentDTO) content).getStatus());
    }

    @Test
    void buildLiveLogInboundDTO_emptyParams_Defaults_Test() {

        // Setup
        final Map<String, String> headers = Collections.singletonMap("Accept", "application/json");

        // Test
        final LiveLoggingDTO result = LiveLoggingUtils.buildLiveLogInboundDTO(
                "req-2", "POST", "/pets", headers, "", false, Collections.emptyMap());

        // Assertions
        Assertions.assertEquals(LiveLoggingMessageTypeEnum.TRAFFIC, result.getType());
        Assertions.assertTrue(result.getPayload() instanceof LiveLoggingTrafficDTO);

        final LiveLoggingTrafficDTO payload = (LiveLoggingTrafficDTO) result.getPayload();
        Assertions.assertEquals("req-2", payload.getId());
        Assertions.assertEquals(LiveLoggingDirectionEnum.REQUEST, payload.getDirection());
        Assertions.assertFalse(payload.isProxied());

        final LiveLoggingContentDTO content = payload.getContent();
        Assertions.assertTrue(content instanceof LiveLoggingInboundContentDTO);
        Assertions.assertEquals("/pets", content.getUrl());
        Assertions.assertEquals(headers, content.getHeaders());
        Assertions.assertEquals("n/a", content.getBody());
        Assertions.assertEquals("POST", ((LiveLoggingInboundContentDTO) content).getMethod());
        Assertions.assertNull(((LiveLoggingInboundContentDTO) content).getRequestParams());
    }

    @Test
    void buildLiveLogOutboundDTO_bodyAndStatus_Test() {

        // Setup
        final Map<String, String> headers = Collections.singletonMap("Content-Type", "application/json");

        // Test
        final LiveLoggingDTO result = LiveLoggingUtils.buildLiveLogOutboundDTO(
                "req-3", "/pets/1", 201, headers, "{\"id\":1}", false);

        // Assertions
        Assertions.assertEquals(LiveLoggingMessageTypeEnum.TRAFFIC, result.getType());
        Assertions.assertTrue(result.getPayload() instanceof LiveLoggingTrafficDTO);

        final LiveLoggingContentDTO content = ((LiveLoggingTrafficDTO) result.getPayload()).getContent();
        Assertions.assertTrue(content instanceof LiveLoggingOutboundContentDTO);
        Assertions.assertEquals("/pets/1", content.getUrl());
        Assertions.assertEquals(headers, content.getHeaders());
        Assertions.assertEquals("{\"id\":1}", content.getBody());
        Assertions.assertEquals(Integer.valueOf(201), ((LiveLoggingOutboundContentDTO) content).getStatus());
    }

    @Test
    void buildS3LiveLogging_buildsPayload_Test() {

        // Test
        final LiveLoggingDTO result = LiveLoggingUtils.buildS3LiveLogging("bucket created", "owner-1");

        // Assertions
        Assertions.assertEquals(LiveLoggingMessageTypeEnum.S3, result.getType());
        Assertions.assertTrue(result.getPayload() instanceof LiveLoggingS3DTO);

        final LiveLoggingS3DTO payload = (LiveLoggingS3DTO) result.getPayload();
        Assertions.assertNull(payload.getId());
        Assertions.assertNotNull(payload.getDate());
        Assertions.assertEquals("bucket created", payload.getInformation());
        Assertions.assertEquals("owner-1", payload.getBucketOwnerId());
    }
}
