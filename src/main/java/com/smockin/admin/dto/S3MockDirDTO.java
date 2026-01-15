package com.smockin.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Created by mgallina.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class S3MockDirDTO {

    private String name;
    private String bucketExtId;
    private String parentDirExtId;

}
