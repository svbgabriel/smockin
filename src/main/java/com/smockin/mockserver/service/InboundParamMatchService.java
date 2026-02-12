package com.smockin.mockserver.service;

import com.smockin.mockserver.exception.InboundParamMatchException;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Created by mgallina.
 */

public interface InboundParamMatchService {

    String enrichWithInboundParamMatches(final HttpServletRequest req,
                                         final String mockPath,
                                         final String responseBody,
                                         final String userCtxPath,
                                         final long mockOwnerUserId) throws InboundParamMatchException;

}
