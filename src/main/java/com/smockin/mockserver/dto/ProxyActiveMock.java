package com.smockin.mockserver.dto;

import com.smockin.admin.persistence.enums.RestMethodEnum;

public record ProxyActiveMock(String path, String userCtx, RestMethodEnum method) {

}
