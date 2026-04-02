package com.smockin.admin.service;

import com.smockin.admin.exception.ValidationException;
import com.smockin.admin.persistence.dao.MailMockDAO;
import com.smockin.admin.persistence.dao.MailMockMessageDAO;
import com.smockin.admin.persistence.entity.MailMock;
import com.smockin.admin.persistence.entity.MailMockMessage;
import com.smockin.admin.service.utils.UserTokenServiceUtils;
import com.smockin.mockserver.engine.MockedMailServerEngine;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import java.util.Date;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class MailMockMessageServiceTest {

    @Mock
    private MailMockDAO mailMockDAO;

    @Mock
    private MailMockMessageDAO mailMockMessageDAO;

    @Mock
    private UserTokenServiceUtils userTokenServiceUtils;

    @Mock
    private MockedServerEngineService mockedServerEngineService;

    @Mock
    private MockedMailServerEngine mockedMailServerEngine;

    private MailMockMessageServiceImpl mailMockMessageService;

    @BeforeEach
    public void setUp() {
        final ObjectProvider<MockedServerEngineService> mockedServerEngineServiceProvider = Mockito.mock(ObjectProvider.class);
        final ObjectProvider<MockedMailServerEngine> mockedMailServerEngineProvider = Mockito.mock(ObjectProvider.class);

        Mockito.lenient().when(mockedServerEngineServiceProvider.getObject()).thenReturn(mockedServerEngineService);
        Mockito.lenient().when(mockedMailServerEngineProvider.getObject()).thenReturn(mockedMailServerEngine);

        mailMockMessageService = new MailMockMessageServiceImpl(mailMockDAO, mailMockMessageDAO, userTokenServiceUtils, mockedServerEngineServiceProvider, mockedMailServerEngineProvider);
    }

    @Test
    void saveMailMessage_returnsExternalId() throws ValidationException {

        // Setup
        final MailMock mailMock = new MailMock();
        mailMock.setExtId("mail-1");

        final MailMockMessage savedMessage = new MailMockMessage();
        savedMessage.setExtId("msg-1");

        Mockito.when(mailMockDAO.findByExtId("mail-1")).thenReturn(mailMock);
        Mockito.when(mailMockMessageDAO.save(Mockito.any(MailMockMessage.class))).thenReturn(savedMessage);

        // Test
        final String result = mailMockMessageService.saveMailMessage(
                "mail-1",
                "sender@example.com",
                "subject",
                "body",
                new Date(),
                Optional.empty());

        // Assertions
        Assertions.assertEquals("msg-1", result);

        final ArgumentCaptor<MailMockMessage> messageCaptor = ArgumentCaptor.forClass(MailMockMessage.class);
        Mockito.verify(mailMockMessageDAO).save(messageCaptor.capture());
        Assertions.assertEquals(mailMock, messageCaptor.getValue().getMailMock());
    }
}
