package com.smockin.mockserver.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * Created by mgallina.
 */
@Setter
@Getter
public class MockServerState {

    private boolean running;
    private int port;

    public MockServerState() {
    }

    public MockServerState(boolean running, int port) {
        this.running = running;
        this.port = port;
    }

}
