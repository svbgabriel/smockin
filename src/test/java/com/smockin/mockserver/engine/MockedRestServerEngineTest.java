package com.smockin.mockserver.engine;

import com.smockin.admin.exception.ValidationException;
import com.smockin.admin.persistence.enums.RestMethodEnum;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MockedRestServerEngineTest {

    private MockedRestServerEngine mockedRestServerEngine;

    @BeforeEach
    void setUp() {
        mockedRestServerEngine = new MockedRestServerEngine();
    }

    @Test
    void addAndRemoveLiveBlockingPathTest() throws ValidationException {

        // Test
        mockedRestServerEngine.addPathToLiveBlocking(RestMethodEnum.GET, "/path", "user-1");

        // Assertions
        Assertions.assertEquals(1,
                mockedRestServerEngine.countLiveBlockingPathsForUser(RestMethodEnum.GET, "/path", "user-1"));

        // Test
        mockedRestServerEngine.removePathFromLiveBlocking(RestMethodEnum.GET, "/path", "user-1");

        // Assertions
        Assertions.assertEquals(0,
                mockedRestServerEngine.countLiveBlockingPathsForUser(RestMethodEnum.GET, "/path", "user-1"));
    }

    @Test
    void clearAllPathsFromLiveBlockingForUserTest() throws ValidationException {

        // Setup
        mockedRestServerEngine.addPathToLiveBlocking(RestMethodEnum.GET, "/path", "user-1");
        mockedRestServerEngine.addPathToLiveBlocking(RestMethodEnum.GET, "/path", "user-2");

        // Test
        mockedRestServerEngine.clearAllPathsFromLiveBlockingForUser("user-1");

        // Assertions
        Assertions.assertEquals(0,
                mockedRestServerEngine.countLiveBlockingPathsForUser(RestMethodEnum.GET, "/path", "user-1"));
        Assertions.assertEquals(1,
                mockedRestServerEngine.countLiveBlockingPathsForUser(RestMethodEnum.GET, "/path", "user-2"));
    }
}
