package com.smockin.mockserver.service;

import com.smockin.admin.persistence.entity.RestfulMockDefinitionRule;
import com.smockin.mockserver.service.dto.RestfulResponseDTO;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

/**
 * Created by gallina.
 */
public interface RuleEngine {

    RestfulResponseDTO process(final HttpServletRequest req, final List<RestfulMockDefinitionRule> rules);

}
