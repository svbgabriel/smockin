package com.smockin.admin.dto.response;

import lombok.Getter;

/**
 * Created by mgallina.
 */
@Getter
public class SimpleMessageResponseDTO<M> {

    private final M message;

    public SimpleMessageResponseDTO(final M message) {
        this.message = message;
    }

}
