package com.smockin.mockserver.engine;

import com.smockin.mockserver.dto.MailMessageSearchDTO;
import com.smockin.mockserver.dto.MailServerMessageInboxDTO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.List;
import java.util.Optional;

class MailInboxCacheTest {

    private MailInboxCache mailInboxCache;

    @BeforeEach
    void setUp() {
        mailInboxCache = new MailInboxCache();
    }

    @Test
    void addAndFindMessageTest() {

        // Setup
        final MailServerMessageInboxDTO inboxDTO =
                new MailServerMessageInboxDTO("msg-1", "from@x", "Hello", "Body", new Date(), 0);
        final CachedMailServerMessage cached =
                new CachedMailServerMessage(inboxDTO, List.of());

        // Test
        mailInboxCache.add("mail-1", cached);
        final Optional<CachedMailServerMessage> result = mailInboxCache.findMessageById("mail-1", "msg-1");

        // Assertions
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals("Hello", result.get().getMailServerMessageInboxDTO().getSubject());
    }

    @Test
    void findAllMessagesWithSearchTest() {

        // Setup
        final MailServerMessageInboxDTO inboxDTO =
                new MailServerMessageInboxDTO("msg-1", "from@x", "Hello World", "Body", new Date(), 0);
        mailInboxCache.add("mail-1", new CachedMailServerMessage(inboxDTO, List.of()));

        final MailMessageSearchDTO searchDTO = new MailMessageSearchDTO();
        searchDTO.setSubject("Hello");

        // Test
        final List<CachedMailServerMessage> results =
                mailInboxCache.findAllMessages("mail-1", Optional.of(searchDTO), Optional.of(0));

        // Assertions
        Assertions.assertEquals(1, results.size());
        Assertions.assertEquals(1, mailInboxCache.countAllMessages("mail-1", Optional.of(searchDTO)));
    }

    @Test
    void deleteMessageTest() {

        // Setup
        final MailServerMessageInboxDTO inboxDTO =
                new MailServerMessageInboxDTO("msg-1", "from@x", "Hello", "Body", new Date(), 0);
        mailInboxCache.add("mail-1", new CachedMailServerMessage(inboxDTO, List.of()));

        // Test
        mailInboxCache.delete("mail-1", "msg-1");

        // Assertions
        Assertions.assertEquals(0, mailInboxCache.countAllMessages("mail-1", Optional.empty()));
    }
}
