package com.smockin.admin.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class EncryptionServiceTest {

    private final EncryptionServiceImpl encryptionService = new EncryptionServiceImpl();

    @Test
    void encryptAndVerifyTest() {

        // Setup
        final String plain = "Passw0rd";

        // Test
        final String encrypted = encryptionService.encrypt(plain);

        // Assertions
        Assertions.assertTrue(encryptionService.verify(plain, encrypted));
    }
}
