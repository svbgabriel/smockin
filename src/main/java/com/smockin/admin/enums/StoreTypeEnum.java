package com.smockin.admin.enums;

import org.apache.commons.lang3.Strings;

import java.util.Arrays;

public enum StoreTypeEnum {
    DB, CACHE;

    public static StoreTypeEnum toEnum(final String value) {

        return Arrays.stream(values())
                .filter(e ->
                        Strings.CI.equals(e.name(), value))
                .findFirst()
                .orElse(null);
    }
}
