package com.smockin.utils;

import com.smockin.admin.persistence.entity.RestfulMock;
import com.smockin.admin.persistence.enums.SmockinUserRoleEnum;

public class MockUtils {

    private MockUtils() {
    }

    public static String buildUserPath(final RestfulMock mock) {

        if (!SmockinUserRoleEnum.SYS_ADMIN.equals(mock.getCreatedBy().getRole())) {

            return GeneralUtils.URL_PATH_SEPARATOR + mock.getCreatedBy().getCtxPath() + mock.getPath();

        }

        return mock.getPath();

    }
}
