package com.smockin.admin.dto.response;

import com.smockin.admin.dto.TunnelRequestDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper=false)
public class TunnelResponseDTO extends TunnelRequestDTO {

    private String uri;

    public TunnelResponseDTO(boolean enabled, String uri) {
        super(enabled);
        this.uri = uri;
    }
}
