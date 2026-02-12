package com.smockin.mockserver.service;

import lombok.Getter;

@Getter
public class StatefulValidationException extends RuntimeException {

    public static final String PATH_STRUCTURE_MISALIGN = "Invalid path '%s' does align with structure of existing JSON";
    public static final String FROM_STRUCTURE_MISALIGN = "Invalid from '%s' does align with structure of existing JSON";
    public static final String PATH_OUT_OF_RANGE_LIST_INDEX = "Invalid path '%s', list index %s is out of range";
    public static final String FROM_OUT_OF_RANGE_LIST_INDEX = "Invalid from '%s', list index %s is out of range";
    public static final String INVALID_PATCH_INSTRUCTION = "Invalid PATCH instruction in request body, %s";

    private final Integer status;

    public StatefulValidationException(final String msg) {
        super(msg);
        this.status = null;
    }

    public StatefulValidationException(final Integer status) {
        super();
        this.status = status;
    }

}
