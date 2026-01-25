package com.smockin.mockserver.service;

import com.smockin.mockserver.engine.MockedS3ServerEngineUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class S3ClientTest {

    @Test
    void handleSeparatorSuffix_appendsWhenMissing() {

        // Setup
        final S3Client client = new S3Client("localhost", 9000);

        // Test
        final String result = client.handleSeparatorSuffix("folder");

        // Assertions
        Assertions.assertEquals(MockedS3ServerEngineUtils.SEPARATOR_CHAR, result);
    }

    @Test
    void handleSeparatorSuffix_noOpWhenPresent() {

        // Setup
        final S3Client client = new S3Client("localhost", 9000);
        final String folderPath = "folder" + MockedS3ServerEngineUtils.SEPARATOR_CHAR;

        // Test
        final String result = client.handleSeparatorSuffix(folderPath);

        // Assertions
        Assertions.assertEquals("", result);
    }
}
