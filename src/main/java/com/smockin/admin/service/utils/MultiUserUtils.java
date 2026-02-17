package com.smockin.admin.service.utils;

import com.smockin.admin.persistence.dao.SmockinUserDAO;
import com.smockin.utils.GeneralUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class MultiUserUtils {

    @Autowired
    private SmockinUserDAO smockinUserDAO;

    public String extractMultiUserCtxPathSegment(final String inboundPath) {
        return StringUtils.split(inboundPath, GeneralUtils.URL_PATH_SEPARATOR)[0];
    }

    public boolean isInboundPathMultiUserPath(final String userCtxPathSegment) {
        return smockinUserDAO.doesUserExistWithCtxPath(userCtxPathSegment);
    }

}
