package com.smockin.admin.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class UserKeyValueDataDTO {

    private String extId;
    private String key;
    private String value;

    public UserKeyValueDataDTO() {

    }

    public UserKeyValueDataDTO(final String extId, final String key, final String value) {
        this.extId = extId;
        this.key = key;
        this.value = value;
    }

}
