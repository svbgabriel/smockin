package com.smockin.mockserver.service;

import com.smockin.admin.persistence.dao.RestfulMockDAO;
import com.smockin.admin.service.utils.UserTokenServiceUtils;
import com.smockin.admin.websocket.LiveLoggingHandler;
import com.smockin.mockserver.engine.MockedRestServerEngineUtils;
import com.smockin.utils.GeneralUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentHashMap;

@ExtendWith(MockitoExtension.class)
class ServerSideEventServiceTest {

    @Mock
    private RestfulMockDAO restfulMockDAO;

    @Mock
    private MockedRestServerEngineUtils mockedRestServerEngineUtils;

    @Mock
    private UserTokenServiceUtils userTokenServiceUtils;

    @Mock
    private LiveLoggingHandler liveLoggingHandler;

    @Spy
    @InjectMocks
    private ServerSideEventServiceImpl serverSideEventService = new ServerSideEventServiceImpl();

    @Test
    void register_addsClientAndAppliesHeaders() throws IOException, NoSuchFieldException, IllegalAccessException {

        // Setup
        final HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        final HttpServletResponse response = Mockito.mock(HttpServletResponse.class);

        Mockito.when(request.getAttribute(GeneralUtils.LOG_REQ_ID)).thenReturn("trace-1");
        Mockito.doNothing().when(serverSideEventService).initHeartBeat(
                Mockito.anyString(),
                Mockito.anyLong(),
                Mockito.anyBoolean(),
                Mockito.anyString(),
                Mockito.anyString(),
                Mockito.any(HttpServletResponse.class));

        // Test
        serverSideEventService.register("/sse/path", 2500L, false, request, response);

        // Assertions
        Mockito.verify(response).setHeader(HttpHeaders.CONTENT_TYPE, ServerSideEventService.SSE_EVENT_STREAM_HEADER);
        Mockito.verify(response).setHeader(HttpHeaders.CACHE_CONTROL, "no-cache");
        Mockito.verify(response).setHeader(HttpHeaders.CONNECTION, "keep-alive");

        final Field clientsField = ServerSideEventServiceImpl.class.getDeclaredField("clients");
        clientsField.setAccessible(true);
        final ConcurrentHashMap<?, ?> clients = (ConcurrentHashMap<?, ?>) clientsField.get(serverSideEventService);
        Assertions.assertEquals(1, clients.size());
    }
}
