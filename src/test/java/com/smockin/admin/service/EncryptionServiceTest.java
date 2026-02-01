package com.smockin.admin.service;

import org.jasypt.util.password.StrongPasswordEncryptor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class EncryptionServiceTest {

    StrongPasswordEncryptor strongPasswordEncryptor = new StrongPasswordEncryptor();

    private final EncryptionServiceImpl encryptionService = new EncryptionServiceImpl(strongPasswordEncryptor);

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
