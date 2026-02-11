package com.smockin.admin.service.mapper;

import com.smockin.admin.dto.*;
import com.smockin.admin.dto.response.RestfulMockResponseDTO;
import com.smockin.admin.persistence.entity.*;
import com.smockin.admin.persistence.enums.RestMockTypeEnum;
import com.smockin.admin.service.ProjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class RestfulMockMapper {

    @Autowired
    private ProjectService projectService;

    public RestfulMockResponseDTO toRestfulMockResponseDTO(final RestfulMock rmd) {

        final boolean isStatefulParent = (RestMockTypeEnum.STATEFUL.equals(rmd.getMockType()) && rmd.getStatefulParent() == null);

        final RestfulMockResponseDTO dto = new RestfulMockResponseDTO(
                rmd.getExtId(),
                rmd.getPath(),
                rmd.getCreatedBy().getCtxPath(),
                rmd.getMethod(),
                rmd.getStatus(),
                rmd.getMockType(),
                isStatefulParent,
                rmd.getDateCreated(),
                rmd.getCreatedBy().getUsername(),
                rmd.getProxyTimeOutInMillis(),
                rmd.getWebSocketTimeoutInMillis(),
                rmd.getSseHeartBeatInMillis(),
                rmd.isProxyPushIdOnConnect(),
                rmd.isRandomiseDefinitions(),
                rmd.isProxyForwardWhenNoRuleMatch(),
                rmd.isRandomiseLatency(),
                rmd.getRandomiseLatencyRangeMinMillis(),
                rmd.getRandomiseLatencyRangeMaxMillis(),
                (rmd.getProject() != null) ? rmd.getProject().getExtId() : null,
                (rmd.getJavaScriptHandler() != null) ? rmd.getJavaScriptHandler().getSyntax() : null,
                (isStatefulParent && rmd.getRestfulMockStatefulMeta() != null) ? rmd.getRestfulMockStatefulMeta().getInitialResponseBody() : null,
                (isStatefulParent && rmd.getRestfulMockStatefulMeta() != null) ? rmd.getRestfulMockStatefulMeta().getIdFieldName() : null,
                (isStatefulParent && rmd.getRestfulMockStatefulMeta() != null) ? rmd.getRestfulMockStatefulMeta().getIdFieldLocation() : null);

        // Definitions
        for (RestfulMockDefinitionOrder order : rmd.getDefinitions()) {
            final RestfulMockDefinitionDTO restfulMockDefinitionDTO = new RestfulMockDefinitionDTO(
                    order.getExtId(),
                    order.getOrderNo(),
                    order.getHttpStatusCode(),
                    order.getResponseContentType(),
                    order.getResponseBody(),
                    order.getSleepInMillis(),
                    order.isSuspend(),
                    order.getFrequencyCount(),
                    order.getFrequencyPercentage());

            for (Map.Entry<String, String> responseHeader : order.getResponseHeaders().entrySet()) {
                restfulMockDefinitionDTO.getResponseHeaders().put(responseHeader.getKey(), responseHeader.getValue());
            }

            dto.getDefinitions().add(restfulMockDefinitionDTO);
        }

        // Rules
        for (RestfulMockDefinitionRule rule : rmd.getRules()) {

            final RuleDTO ruleDto = new RuleDTO(
                    rule.getExtId(),
                    rule.getOrderNo(),
                    rule.getHttpStatusCode(),
                    rule.getResponseContentType(),
                    rule.getResponseBody(),
                    rule.getSleepInMillis(),
                    rule.isSuspend());

            for (Map.Entry<String, String> responseHeader : rule.getResponseHeaders().entrySet()) {
                ruleDto.getResponseHeaders().put(responseHeader.getKey(), responseHeader.getValue());
            }

            for (RestfulMockDefinitionRuleGroup group : rule.getConditionGroups()) {

                final RuleGroupDTO groupDto = new RuleGroupDTO(group.getExtId(), group.getOrderNo());

                for (RestfulMockDefinitionRuleGroupCondition condition : group.getConditions()) {
                    groupDto.getConditions().add(new RuleConditionDTO(
                            condition.getExtId(),
                            condition.getField(),
                            condition.getDataType(),
                            condition.getComparator(),
                            condition.getMatchValue(),
                            condition.getRuleMatchingType(),
                            condition.getCaseSensitive()));
                }

                ruleDto.getGroups().add(groupDto);
            }

            dto.getRules().add(ruleDto);
        }

        return dto;
    }

    public RestfulMock toRestfulMock(final RestfulMockDTO dto, final SmockinUser smockinUser) {

        return new RestfulMock(
                dto.getPath(),
                dto.getMethod(),
                dto.getStatus(),
                dto.getMockType(),
                dto.getProxyTimeoutInMillis(),
                dto.getWebSocketTimeoutInMillis(),
                dto.getSseHeartBeatInMillis(),
                dto.isProxyPushIdOnConnect(),
                dto.isRandomiseDefinitions(),
                dto.isProxyForwardWhenNoRuleMatch(),
                smockinUser,
                dto.isRandomiseLatency(),
                dto.getRandomiseLatencyRangeMinMillis(),
                dto.getRandomiseLatencyRangeMaxMillis(),
                (dto.getProjectId() != null) ? projectService.loadByExtId(dto.getProjectId()) : null);
    }

    public void populateEndpointDefinitionsAndRules(final RestfulMockDTO dtoSource, final RestfulMock mockDest) {

        // Endpoint Sequenced Definition
        for (RestfulMockDefinitionDTO restMockOrderDto : dtoSource.getDefinitions()) {

            final RestfulMockDefinitionOrder restfulMockDefinitionOrder =
                    new RestfulMockDefinitionOrder(mockDest, restMockOrderDto.getHttpStatusCode(), restMockOrderDto.getResponseContentType(), restMockOrderDto.getResponseBody(), restMockOrderDto.getOrderNo(), restMockOrderDto.getSleepInMillis(), restMockOrderDto.isSuspend(), restMockOrderDto.getFrequencyCount(), restMockOrderDto.getFrequencyPercentage());

            if (restMockOrderDto.getResponseHeaders() != null) {
                for (Map.Entry<String, String> responseHeader : restMockOrderDto.getResponseHeaders().entrySet()) {
                    restfulMockDefinitionOrder.getResponseHeaders().put(responseHeader.getKey(), responseHeader.getValue());
                }
            }

            mockDest.getDefinitions().add(restfulMockDefinitionOrder);
        }

        // Endpoint Rules
        for (RuleDTO ruleDto : dtoSource.getRules()) {

            final RestfulMockDefinitionRule rule = new RestfulMockDefinitionRule(mockDest, ruleDto.getOrderNo(), ruleDto.getHttpStatusCode(), ruleDto.getResponseContentType(), ruleDto.getResponseBody(), ruleDto.getSleepInMillis(), ruleDto.isSuspend());

            for (Map.Entry<String, String> responseHeader : ruleDto.getResponseHeaders().entrySet()) {
                rule.getResponseHeaders().put(responseHeader.getKey(), responseHeader.getValue());
            }

            buildRuleGroups(ruleDto, rule);

            mockDest.getRules().add(rule);
        }

    }

    public void buildRuleGroups(final RuleDTO dto, final RestfulMockDefinitionRule rule) {

        for (RuleGroupDTO groupDTO : dto.getGroups()) {

            final RestfulMockDefinitionRuleGroup group = new RestfulMockDefinitionRuleGroup(rule, groupDTO.getOrderNo());

            for (RuleConditionDTO conditionDTO : groupDTO.getConditions()) {
                group.getConditions().add(new RestfulMockDefinitionRuleGroupCondition(group, conditionDTO.getField(), conditionDTO.getDataType(), conditionDTO.getComparator(), conditionDTO.getValue(), conditionDTO.getRuleMatchingType(), conditionDTO.isCaseSensitive()));
            }

            rule.getConditionGroups().add(group);
        }

    }

}
