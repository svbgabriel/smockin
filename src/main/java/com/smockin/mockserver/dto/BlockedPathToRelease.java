package com.smockin.mockserver.dto;

import com.smockin.admin.persistence.enums.RestMethodEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@AllArgsConstructor
@EqualsAndHashCode
@ToString
public class BlockedPathToRelease {

    private RestMethodEnum method;
    private String pathPattern;

}
