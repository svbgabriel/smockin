package com.smockin.mockserver.engine;

import org.junit.jupiter.api.BeforeEach;

class MockedS3ServerEngineTest {

    private MockedS3ServerEngine mockedS3ServerEngine;

    @BeforeEach
    void setUp() {

        mockedS3ServerEngine = new MockedS3ServerEngine();
    }


}
