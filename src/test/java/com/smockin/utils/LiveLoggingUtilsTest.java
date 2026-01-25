package com.smockin.utils;

import com.smockin.admin.dto.response.LiveLoggingContentDTO;
import com.smockin.admin.dto.response.LiveLoggingDTO;
import com.smockin.admin.dto.response.LiveLoggingInboundContentDTO;
import com.smockin.admin.dto.response.LiveLoggingOutboundContentDTO;
import com.smockin.admin.dto.response.LiveLoggingS3DTO;
import com.smockin.admin.dto.response.LiveLoggingTrafficDTO;
import com.smockin.admin.enums.LiveLoggingDirectionEnum;
import com.smockin.admin.enums.LiveLoggingMessageTypeEnum;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class LiveLoggingUtilsTest {

    @Test
    public void buildLiveLogInterceptedResponseDTO_defaultsBody_Test() {

        // Setup
        final Map<String, String> headers = new HashMap<>();
        headers.put("X-Test", "true");

        // Test
        final LiveLoggingDTO result = LiveLoggingUtils.buildLiveLogInterceptedResponseDTO(
                "req-1", "http://example.com", 200, headers, " ", true);

        // Assertions
        Assert.assertEquals(LiveLoggingMessageTypeEnum.BLOCKED_RESPONSE, result.getType());
        Assert.assertTrue(result.getPayload() instanceof LiveLoggingTrafficDTO);

        final LiveLoggingTrafficDTO payload = (LiveLoggingTrafficDTO) result.getPayload();
        Assert.assertEquals("req-1", payload.getId());
        Assert.assertEquals(LiveLoggingDirectionEnum.RESPONSE, payload.getDirection());
        Assert.assertTrue(payload.isProxied());
        Assert.assertNotNull(payload.getDate());

        final LiveLoggingContentDTO content = payload.getContent();
        Assert.assertTrue(content instanceof LiveLoggingOutboundContentDTO);
        Assert.assertEquals("http://example.com", content.getUrl());
        Assert.assertEquals(headers, content.getHeaders());
        Assert.assertEquals("n/a", content.getBody());
        Assert.assertEquals(Integer.valueOf(200), ((LiveLoggingOutboundContentDTO) content).getStatus());
    }

    @Test
    public void buildLiveLogInboundDTO_emptyParams_Defaults_Test() {

        // Setup
        final Map<String, String> headers = Collections.singletonMap("Accept", "application/json");

        // Test
        final LiveLoggingDTO result = LiveLoggingUtils.buildLiveLogInboundDTO(
                "req-2", "POST", "/pets", headers, "", false, Collections.emptyMap());

        // Assertions
        Assert.assertEquals(LiveLoggingMessageTypeEnum.TRAFFIC, result.getType());
        Assert.assertTrue(result.getPayload() instanceof LiveLoggingTrafficDTO);

        final LiveLoggingTrafficDTO payload = (LiveLoggingTrafficDTO) result.getPayload();
        Assert.assertEquals("req-2", payload.getId());
        Assert.assertEquals(LiveLoggingDirectionEnum.REQUEST, payload.getDirection());
        Assert.assertFalse(payload.isProxied());

        final LiveLoggingContentDTO content = payload.getContent();
        Assert.assertTrue(content instanceof LiveLoggingInboundContentDTO);
        Assert.assertEquals("/pets", content.getUrl());
        Assert.assertEquals(headers, content.getHeaders());
        Assert.assertEquals("n/a", content.getBody());
        Assert.assertEquals("POST", ((LiveLoggingInboundContentDTO) content).getMethod());
        Assert.assertNull(((LiveLoggingInboundContentDTO) content).getRequestParams());
    }

    @Test
    public void buildLiveLogOutboundDTO_bodyAndStatus_Test() {

        // Setup
        final Map<String, String> headers = Collections.singletonMap("Content-Type", "application/json");

        // Test
        final LiveLoggingDTO result = LiveLoggingUtils.buildLiveLogOutboundDTO(
                "req-3", "/pets/1", 201, headers, "{\"id\":1}", false);

        // Assertions
        Assert.assertEquals(LiveLoggingMessageTypeEnum.TRAFFIC, result.getType());
        Assert.assertTrue(result.getPayload() instanceof LiveLoggingTrafficDTO);

        final LiveLoggingContentDTO content = ((LiveLoggingTrafficDTO) result.getPayload()).getContent();
        Assert.assertTrue(content instanceof LiveLoggingOutboundContentDTO);
        Assert.assertEquals("/pets/1", content.getUrl());
        Assert.assertEquals(headers, content.getHeaders());
        Assert.assertEquals("{\"id\":1}", content.getBody());
        Assert.assertEquals(Integer.valueOf(201), ((LiveLoggingOutboundContentDTO) content).getStatus());
    }

    @Test
    public void buildS3LiveLogging_buildsPayload_Test() {

        // Test
        final LiveLoggingDTO result = LiveLoggingUtils.buildS3LiveLogging("bucket created", "owner-1");

        // Assertions
        Assert.assertEquals(LiveLoggingMessageTypeEnum.S3, result.getType());
        Assert.assertTrue(result.getPayload() instanceof LiveLoggingS3DTO);

        final LiveLoggingS3DTO payload = (LiveLoggingS3DTO) result.getPayload();
        Assert.assertNull(payload.getId());
        Assert.assertNotNull(payload.getDate());
        Assert.assertEquals("bucket created", payload.getInformation());
        Assert.assertEquals("owner-1", payload.getBucketOwnerId());
    }
}
