package com.smockin.mockserver.service;

import com.smockin.admin.exception.RecordNotFoundException;
import com.smockin.admin.exception.ValidationException;
import com.smockin.admin.persistence.dao.RestfulMockDAO;
import com.smockin.admin.persistence.entity.RestfulMock;
import com.smockin.admin.persistence.entity.SmockinUser;
import com.smockin.admin.persistence.enums.RecordStatusEnum;
import com.smockin.admin.persistence.enums.RestMethodEnum;
import com.smockin.admin.persistence.enums.RestMockTypeEnum;
import com.smockin.admin.persistence.enums.SmockinUserRoleEnum;
import com.smockin.admin.service.utils.UserTokenServiceUtils;
import com.smockin.mockserver.engine.MockedRestServerEngineUtils;
import com.smockin.mockserver.service.bean.ProxiedKey;
import com.smockin.mockserver.service.dto.HttpProxiedDTO;
import com.smockin.mockserver.service.dto.RestfulResponseDTO;
import com.smockin.utils.GeneralUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.MediaType;

import java.io.File;
import java.util.concurrent.*;

/**
 * Created by mgallina on 11/08/17.
 */
@RunWith(MockitoJUnitRunner.class)
public class HttpProxyServiceQueueTest {

    private RestfulMock mockReqHelloGet, mockReqHelloPost, mockReqHelloDelete, mockReqFooGet;
    private ProxiedKey helloKeyGet, helloKeyPost, helloKeyDelete, fooKeyGet;
    private HttpProxiedDTO helloGetDTO, helloPostDTO, helloDeleteDTO, fooGetDTO;
    private SmockinUser user;

    @Mock
    private RestfulMockDAO restfulMockDAO;

    @Mock
    private UserTokenServiceUtils userTokenServiceUtils;

    @Mock
    private MockedRestServerEngineUtils mockedRestServerEngineUtils;

    @Spy
    @InjectMocks
    private HttpProxyService proxyService = new HttpProxyServiceImpl();

    @Before
    public void setUp() throws RecordNotFoundException, ValidationException {

        user = new SmockinUser();
        user.setRole(SmockinUserRoleEnum.REGULAR);
        user.setCtxPath("foo");
        user.setSessionToken(GeneralUtils.generateUUID());

        helloKeyGet = new ProxiedKey("/helloworld", RestMethodEnum.GET);
        helloKeyPost = new ProxiedKey("/helloworld", RestMethodEnum.POST);
        helloKeyDelete = new ProxiedKey("/helloworld", RestMethodEnum.DELETE);
        fooKeyGet = new ProxiedKey("/foo", RestMethodEnum.GET);

        mockReqHelloGet = new RestfulMock(helloKeyGet.path(), helloKeyGet.method(), RecordStatusEnum.ACTIVE, RestMockTypeEnum.PROXY_HTTP, 500, 0, 0, false, false, false, user, false, 0, 0, null);
        mockReqHelloGet.setExtId(GeneralUtils.generateUUID());
        mockReqHelloPost = new RestfulMock(helloKeyPost.path(), helloKeyPost.method(), RecordStatusEnum.ACTIVE, RestMockTypeEnum.PROXY_HTTP, 500, 0, 0, false, false, false, user, false, 0, 0, null);
        mockReqHelloPost.setExtId(GeneralUtils.generateUUID());
        mockReqHelloDelete = new RestfulMock(helloKeyDelete.path(), helloKeyDelete.method(), RecordStatusEnum.ACTIVE, RestMockTypeEnum.PROXY_HTTP, 500, 0, 0, false, false, false, user, false, 0, 0, null);
        mockReqHelloDelete.setExtId(GeneralUtils.generateUUID());
        mockReqFooGet = new RestfulMock(fooKeyGet.path(), fooKeyGet.method(), RecordStatusEnum.ACTIVE, RestMockTypeEnum.PROXY_HTTP, 500, 0, 0, false, false, false, user, false, 0, 0, null);
        mockReqFooGet.setExtId(GeneralUtils.generateUUID());

        helloGetDTO = new HttpProxiedDTO(helloKeyGet.method(), 200, MediaType.APPLICATION_JSON_VALUE, "{ \"msg\" : \"helloworld 1\" }");
        helloPostDTO = new HttpProxiedDTO(helloKeyPost.method(), 200, MediaType.APPLICATION_JSON_VALUE, "{ \"msg\" : \"helloworld 2\" }");
        helloDeleteDTO = new HttpProxiedDTO(helloKeyDelete.method(), 200, MediaType.APPLICATION_JSON_VALUE, "{ \"msg\" : \"helloworld 3\" }");
        fooGetDTO = new HttpProxiedDTO(fooKeyGet.method(), 200, MediaType.APPLICATION_JSON_VALUE, "{ \"msg\" : \"foo 1\" }");

        Mockito.when(restfulMockDAO.findByExtId(mockReqHelloGet.getExtId())).thenReturn(mockReqHelloGet);
        Mockito.when(restfulMockDAO.findByExtId(mockReqHelloPost.getExtId())).thenReturn(mockReqHelloPost);
        Mockito.when(restfulMockDAO.findByExtId(mockReqHelloDelete.getExtId())).thenReturn(mockReqHelloDelete);
        Mockito.when(restfulMockDAO.findByExtId(mockReqFooGet.getExtId())).thenReturn(mockReqFooGet);

        Mockito.doNothing().when(userTokenServiceUtils).validateRecordOwner(Mockito.any(SmockinUser.class), Mockito.anyString());

        Mockito.when(mockedRestServerEngineUtils.buildUserPath(mockReqHelloGet)).thenReturn(File.separator + user.getCtxPath() + mockReqHelloGet.getPath());
        Mockito.when(mockedRestServerEngineUtils.buildUserPath(mockReqHelloPost)).thenReturn(File.separator + user.getCtxPath() + mockReqHelloPost.getPath());
        Mockito.when(mockedRestServerEngineUtils.buildUserPath(mockReqHelloDelete)).thenReturn(File.separator + user.getCtxPath() + mockReqHelloDelete.getPath());
        Mockito.when(mockedRestServerEngineUtils.buildUserPath(mockReqFooGet)).thenReturn(File.separator + user.getCtxPath() + mockReqFooGet.getPath());

        proxyService.addResponse(mockReqHelloGet.getExtId(), helloGetDTO, user.getSessionToken());
        proxyService.addResponse(mockReqHelloPost.getExtId(), helloPostDTO, user.getSessionToken());
        proxyService.addResponse(mockReqHelloDelete.getExtId(), helloDeleteDTO, user.getSessionToken());
        proxyService.addResponse(mockReqFooGet.getExtId(), fooGetDTO, user.getSessionToken());

    }

    @Test
    public void waitForResponse_ConsumeAll_Test() throws InterruptedException, ExecutionException, TimeoutException {

        final RestfulResponseDTO dto1 = proxyService.waitForResponse(File.separator + user.getCtxPath() + helloKeyGet.path(), mockReqHelloGet);
        Assert.assertNotNull(dto1);
        Assert.assertEquals(helloGetDTO.getBody(), dto1.getResponseBody());

        final RestfulResponseDTO dto2 = proxyService.waitForResponse(File.separator + user.getCtxPath() + helloKeyPost.path(), mockReqHelloPost);
        Assert.assertNotNull(dto2);
        Assert.assertEquals(helloPostDTO.getBody(), dto2.getResponseBody());

        final RestfulResponseDTO dto3 = proxyService.waitForResponse(File.separator + user.getCtxPath() + helloKeyDelete.path(), mockReqHelloDelete);
        Assert.assertNotNull(dto3);
        Assert.assertEquals(helloDeleteDTO.getBody(), dto3.getResponseBody());

        final RestfulResponseDTO dto4 = proxyService.waitForResponse(File.separator + user.getCtxPath() + fooKeyGet.path(), mockReqFooGet);
        Assert.assertNotNull(dto4);
        Assert.assertEquals(fooGetDTO.getBody(), dto4.getResponseBody());

    }

    @Test
    public void waitForResponse_ConsumeAndWaitTimeout_Test() throws InterruptedException, ExecutionException, TimeoutException {

        final RestfulResponseDTO dto1 = proxyService.waitForResponse(File.separator + user.getCtxPath() + helloKeyGet.path(), mockReqHelloGet);
        Assert.assertNotNull(dto1);
        Assert.assertEquals(helloGetDTO.getBody(), dto1.getResponseBody());

        Assert.assertNull(proxyService.waitForResponse(File.separator + user.getCtxPath() + helloKeyGet.path(), mockReqHelloGet));
    }

    @Test
    public void clearSession_Test() throws InterruptedException, ExecutionException, TimeoutException {

        // Test
        proxyService.clearAllSessions();

        // Assertions
        Assert.assertNull(proxyService.waitForResponse(helloKeyGet.path(), mockReqHelloGet));
    }

    @Test
    public void clearSession_ByPath_Hello_Test() throws RecordNotFoundException, ValidationException {

        // Test
        proxyService.clearSession(mockReqHelloPost.getExtId(), user.getSessionToken());

        // Assertions
        Assert.assertNull(proxyService.waitForResponse(File.separator + user.getCtxPath() + helloKeyGet.path(), mockReqHelloGet));
        Assert.assertNull(proxyService.waitForResponse(File.separator + user.getCtxPath() + helloKeyPost.path(), mockReqHelloPost));
        Assert.assertNull(proxyService.waitForResponse(File.separator + user.getCtxPath() + helloKeyDelete.path(), mockReqHelloDelete));

        final RestfulResponseDTO dto4 = proxyService.waitForResponse(File.separator + user.getCtxPath() + fooKeyGet.path(), mockReqFooGet);
        Assert.assertNotNull(dto4);
        Assert.assertEquals(fooGetDTO.getBody(), dto4.getResponseBody());
    }

    @Test
    public void clearSession_ByPath_Foo_Test() throws RecordNotFoundException, ValidationException {

        // Test
        proxyService.clearSession(mockReqFooGet.getExtId(), user.getSessionToken());

        // Assertions
        Assert.assertNull(proxyService.waitForResponse(File.separator + user.getCtxPath() + fooKeyGet.path(), mockReqFooGet));

        final RestfulResponseDTO dto1 = proxyService.waitForResponse(File.separator + user.getCtxPath() + helloKeyGet.path(), mockReqHelloGet);
        Assert.assertNotNull(dto1);
        Assert.assertEquals(helloGetDTO.getBody(), dto1.getResponseBody());

        final RestfulResponseDTO dto2 = proxyService.waitForResponse(File.separator + user.getCtxPath() + helloKeyPost.path(), mockReqHelloPost);
        Assert.assertNotNull(dto2);
        Assert.assertEquals(helloPostDTO.getBody(), dto2.getResponseBody());

        final RestfulResponseDTO dto3 = proxyService.waitForResponse(File.separator + user.getCtxPath() + helloKeyDelete.path(), mockReqHelloDelete);
        Assert.assertNotNull(dto3);
        Assert.assertEquals(helloDeleteDTO.getBody(), dto3.getResponseBody());
    }

}
