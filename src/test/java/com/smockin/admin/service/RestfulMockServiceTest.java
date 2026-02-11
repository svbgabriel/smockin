package com.smockin.admin.service;

import com.smockin.admin.dto.RestfulMockDTO;
import com.smockin.admin.exception.RecordNotFoundException;
import com.smockin.admin.exception.ValidationException;
import com.smockin.admin.persistence.dao.RestfulMockDAO;
import com.smockin.admin.persistence.dao.RestfulMockDefinitionRuleDAO;
import com.smockin.admin.persistence.entity.RestfulMock;
import com.smockin.admin.persistence.entity.SmockinUser;
import com.smockin.admin.service.mapper.RestfulMockMapper;
import com.smockin.admin.service.utils.RestfulMockServiceUtils;
import com.smockin.admin.service.utils.UserTokenServiceUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RestfulMockServiceTest {

    @Mock
    private RestfulMockDAO restfulMockDAO;

    @Mock
    private RestfulMockDefinitionRuleDAO restfulMockDefinitionRuleDAO;

    @Mock
    private RestfulMockServiceUtils restfulMockServiceUtils;

    @Mock
    private UserTokenServiceUtils userTokenServiceUtils;

    @Mock
    private RestfulMockMapper restfulMockMapper;

    @InjectMocks
    private RestfulMockServiceImpl restfulMockService;

    @Test
    void createEndpoint_returnsExternalId() throws RecordNotFoundException, ValidationException {

        // Setup
        final RestfulMockDTO dto = new RestfulMockDTO();
        dto.setPath("/api/widgets");

        final SmockinUser user = new SmockinUser();
        user.setId(1L);

        final RestfulMock mock = new RestfulMock();
        mock.setExtId("mock-1");

        Mockito.when(userTokenServiceUtils.loadCurrentActiveUser("token")).thenReturn(user);
        Mockito.when(restfulMockServiceUtils.buildRestfulMock(dto, user)).thenReturn(mock);
        Mockito.when(restfulMockServiceUtils.handleCreateStatefulMockType(dto, mock, user)).thenReturn(mock);
        Mockito.when(restfulMockDAO.save(mock)).thenReturn(mock);

        // Test
        final String result = restfulMockService.createEndpoint(dto, "token");

        // Assertions
        Assertions.assertEquals("mock-1", result);
        Mockito.verify(restfulMockServiceUtils).handleEndpointOrdering();
    }
}
