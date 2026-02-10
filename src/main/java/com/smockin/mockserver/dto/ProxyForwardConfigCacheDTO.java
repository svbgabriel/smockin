package com.smockin.mockserver.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Created by mgallina.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper=false)
public class ProxyForwardConfigCacheDTO extends ProxyForwardConfigDTO {

    private String createdByUserExtId;
    private String userCtxPath;

}
