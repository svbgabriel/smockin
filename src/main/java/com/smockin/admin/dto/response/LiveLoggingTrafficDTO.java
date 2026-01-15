package com.smockin.admin.dto.response;

import com.smockin.admin.enums.LiveLoggingDirectionEnum;
import com.smockin.utils.GeneralUtils;
import lombok.Getter;

import java.util.Date;

@Getter
public class LiveLoggingTrafficDTO implements LiveLoggingPayloadDTO {

    private final String id;
    private final LiveLoggingDirectionEnum direction;
    private final Date date;
    private final boolean proxied;
    private final LiveLoggingContentDTO content;

    public LiveLoggingTrafficDTO(final String id,
                                 final LiveLoggingDirectionEnum direction,
                                 final boolean proxied,
                                 final LiveLoggingContentDTO content) {
        this.id = id;
        this.direction = direction;
        this.date = GeneralUtils.getCurrentDate();
        this.proxied = proxied;
        this.content = content;
    }

}

