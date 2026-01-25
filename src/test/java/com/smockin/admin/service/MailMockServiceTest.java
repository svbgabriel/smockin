package com.smockin.admin.service;

import com.smockin.admin.dto.response.MailMockResponseLiteDTO;
import com.smockin.admin.exception.RecordNotFoundException;
import com.smockin.admin.persistence.dao.MailMockDAO;
import com.smockin.admin.persistence.dao.MailMockMessageDAO;
import com.smockin.admin.persistence.entity.MailMock;
import com.smockin.admin.persistence.entity.SmockinUser;
import com.smockin.admin.persistence.enums.RecordStatusEnum;
import com.smockin.admin.service.utils.UserTokenServiceUtils;
import com.smockin.mockserver.engine.MockedMailServerEngine;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;
import java.util.List;

@ExtendWith(MockitoExtension.class)
class MailMockServiceTest {

    @Mock
    private UserTokenServiceUtils userTokenServiceUtils;

    @Mock
    private MailMockDAO mailMockDAO;

    @Mock
    private MailMockMessageDAO mailMockMessageDAO;

    @Mock
    private MockedServerEngineService mockedServerEngineService;

    @Mock
    private MockedMailServerEngine mockedMailServerEngine;

    @InjectMocks
    private MailMockServiceImpl mailMockService;

    @Test
    void loadAll_returnsLiteDtos() throws RecordNotFoundException {

        // Setup
        final String token = "token";
        final SmockinUser user = new SmockinUser();
        user.setId(1L);

        final MailMock mailMock = new MailMock();
        mailMock.setId(10L);
        mailMock.setExtId("mail-1");
        mailMock.setDateCreated(new Date());
        mailMock.setAddress("test@smockin.local");
        mailMock.setStatus(RecordStatusEnum.ACTIVE);
        mailMock.setSaveReceivedMail(true);

        Mockito.when(userTokenServiceUtils.loadCurrentActiveUser(token)).thenReturn(user);
        Mockito.when(mailMockDAO.findAllByUser(1L)).thenReturn(List.of(mailMock));
        Mockito.when(mailMockMessageDAO.countAllMessageByMailMockId(10L)).thenReturn(3);

        // Test
        final List<MailMockResponseLiteDTO> result = mailMockService.loadAll(token);

        // Assertions
        Assertions.assertEquals(1, result.size());
        final MailMockResponseLiteDTO dto = result.getFirst();
        Assertions.assertEquals("mail-1", dto.getExternalId());
        Assertions.assertEquals(3, dto.getMessageCount());
        Assertions.assertEquals("test@smockin.local", dto.getAddress());
        Assertions.assertEquals(RecordStatusEnum.ACTIVE, dto.getStatus());
        Assertions.assertTrue(dto.isSaveReceivedMail());
    }
}
