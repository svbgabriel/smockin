package com.smockin.utils;

import com.smockin.admin.dto.HttpClientCallDTO;
import com.smockin.admin.persistence.enums.RestMethodEnum;
import org.apache.http.NameValuePair;
import org.apache.http.client.fluent.Request;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

class HttpClientUtilsTest {

    @Test
    void handleRequestData_formUrlEncodedBody_Test() {

        // Setup
        final Request request = Mockito.mock(Request.class);
        Mockito.doReturn(request).when(request).bodyForm(Mockito.anyList());

        final Map<String, String> headers = new HashMap<>();
        headers.put(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE);

        final HttpClientCallDTO reqDto = new HttpClientCallDTO("http://localhost", RestMethodEnum.POST);
        reqDto.setBody("name=bob&age=21&invalid");

        // Test
        HttpClientUtils.handleRequestData(request, headers, reqDto);

        // Assertions
        final ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
        Mockito.verify(request).bodyForm(captor.capture());
        Mockito.verify(request, Mockito.never()).bodyByteArray(Mockito.any());

        final List<NameValuePair> params = captor.getValue();
        Assertions.assertEquals(2, params.size());
        Assertions.assertEquals("name", params.get(0).getName());
        Assertions.assertEquals("bob", params.get(0).getValue());
        Assertions.assertEquals("age", params.get(1).getName());
        Assertions.assertEquals("21", params.get(1).getValue());
    }

    @Test
    void handleRequestData_nonFormBody_UsesByteArray_Test() {

        // Setup
        final Request request = Mockito.mock(Request.class);
        Mockito.doReturn(request).when(request).bodyByteArray(Mockito.any());

        final Map<String, String> headers = new HashMap<>();
        final HttpClientCallDTO reqDto = new HttpClientCallDTO("http://localhost", RestMethodEnum.POST);
        reqDto.setBody("plain-text");

        // Test
        HttpClientUtils.handleRequestData(request, headers, reqDto);

        // Assertions
        final ArgumentCaptor<byte[]> captor = ArgumentCaptor.forClass(byte[].class);
        Mockito.verify(request).bodyByteArray(captor.capture());
        Mockito.verify(request, Mockito.never()).bodyForm(Mockito.anyList());
        Assertions.assertEquals("plain-text", new String(captor.getValue()));
    }

    @Test
    void handleRequestData_nullBody_UsesNullBytes_Test() {

        // Setup
        final Request request = Mockito.mock(Request.class);
        Mockito.doReturn(request).when(request).bodyByteArray(Mockito.any());

        final Map<String, String> headers = new HashMap<>();
        headers.put(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE);

        final HttpClientCallDTO reqDto = new HttpClientCallDTO("http://localhost", RestMethodEnum.POST);
        reqDto.setBody(null);

        // Test
        HttpClientUtils.handleRequestData(request, headers, reqDto);

        // Assertions
        Mockito.verify(request).bodyByteArray(null);
        Mockito.verify(request, Mockito.never()).bodyForm(Mockito.anyList());
    }
}
