package com.smockin.mockserver.engine;

import com.icegreen.greenmail.store.FolderException;
import com.icegreen.greenmail.user.GreenMailUser;
import com.icegreen.greenmail.user.UserException;
import com.smockin.admin.persistence.dao.MailMockDAO;
import com.smockin.admin.persistence.entity.MailMock;
import com.smockin.admin.persistence.entity.SmockinUser;
import com.smockin.admin.service.MailMockMessageService;
import com.smockin.admin.service.SmockinUserService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class MockedMailServerEngineTest {

    @Mock
    private MailMockMessageService mailMockMessageService;

    @Mock
    private MailInboxCache mailInboxCache;

    @Mock
    private SmockinUserService smockinUserService;

    @Mock
    private MailMockDAO mailMockDAO;

    @Spy
    @InjectMocks
    private MockedMailServerEngine mockedMailServerEngine = new MockedMailServerEngine();

    @Test
    void autoGenerateMailInbox_createsNewInbox() throws UserException, FolderException {

        // Setup
        final SmockinUser adminUser = new SmockinUser();
        adminUser.setUsername("admin");

        Mockito.when(mailMockDAO.findByAddress("user@smockin.local")).thenReturn(null);
        Mockito.when(smockinUserService.loadDefaultUser()).thenReturn(Optional.of(adminUser));
        Mockito.when(mailMockDAO.save(Mockito.any(MailMock.class))).thenAnswer(invocation -> invocation.getArgument(0));

        final GreenMailUser greenMailUser = Mockito.mock(GreenMailUser.class);
        Mockito.doReturn(greenMailUser).when(mockedMailServerEngine).addMailUser(Mockito.any(MailMock.class));

        // Test
        final GreenMailUser result = mockedMailServerEngine.autoGenerateMailInbox("user@smockin.local");

        // Assertions
        Assertions.assertNotNull(result);
        Mockito.verify(mailMockDAO).save(Mockito.any(MailMock.class));
    }
}
