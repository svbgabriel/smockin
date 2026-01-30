import { RuleComparator, RuleMatchingType } from './models';

export const ACTIVE_STATUS = 'ACTIVE';
export const INACTIVE_STATUS = 'INACTIVE';

export const HTTP_METHODS = ['GET', 'POST', 'PUT', 'DELETE', 'PATCH'];

export const CONTENT_MIME_TYPES = [
  'application/json',
  'text/html',
  'text/plain',
  'text/css',
  'application/xml'
];

export const MOCK_TYPES = [
  { name: 'mockType.httpSequenced', value: 'SEQ' },
  { name: 'mockType.httpRules', value: 'RULE' },
  { name: 'mockType.httpExternalFeed', value: 'PROXY_HTTP' },
  { name: 'mockType.customJs', value: 'CUSTOM_JS' },
  { name: 'mockType.sseProxied', value: 'PROXY_SSE' },
  { name: 'mockType.wsProxied', value: 'PROXY_WS' },
  { name: 'mockType.wsRules', value: 'RULE_WS' },
  { name: 'mockType.statefulRest', value: 'STATEFUL' }
];

export const RULE_MATCHING_TYPES: RuleMatchingType[] = [
  {
    name: 'rule.matchType.pathVariable',
    value: 'PATH_VARIABLE',
    fieldPlaceholderText: 'rule.matchType.pathVariable.placeholder'
  },
  {
    name: 'rule.matchType.pathVariableWildcard',
    value: 'PATH_VARIABLE_WILD',
    fieldPlaceholderText: 'rule.matchType.pathVariableWildcard.placeholder'
  },
  {
    name: 'rule.matchType.requestHeader',
    value: 'REQUEST_HEADER',
    fieldPlaceholderText: 'rule.matchType.requestHeader.placeholder'
  },
  {
    name: 'rule.matchType.requestParam',
    value: 'REQUEST_PARAM',
    fieldPlaceholderText: 'rule.matchType.requestParam.placeholder'
  },
  { name: 'rule.matchType.requestBody', value: 'REQUEST_BODY', fieldPlaceholderText: '' },
  {
    name: 'rule.matchType.requestBodyJson',
    value: 'REQUEST_BODY_JSON_ANY',
    fieldPlaceholderText: 'rule.matchType.requestBodyJson.placeholder'
  }
];

export const WEBSOCKET_RULE_MATCHING_TYPES: RuleMatchingType[] = [
  { name: 'rule.matchType.requestBody', value: 'REQUEST_BODY', fieldPlaceholderText: '' },
  {
    name: 'rule.matchType.requestBodyJson',
    value: 'REQUEST_BODY_JSON_ANY',
    fieldPlaceholderText: 'rule.matchType.requestBodyJson.placeholder'
  }
];

export const RULE_COMPARATORS: RuleComparator[] = [
  {
    dropDownName: 'rule.comparator.equalsText',
    tableName: 'rule.comparator.equalsLabel',
    value: 'EQUALS',
    dataType: 'TEXT'
  },
  {
    dropDownName: 'rule.comparator.equalsNumber',
    tableName: 'rule.comparator.equalsSymbol',
    value: 'EQUALS',
    dataType: 'NUMERIC'
  },
  {
    dropDownName: 'rule.comparator.containsText',
    tableName: 'rule.comparator.containsLabel',
    value: 'CONTAINS',
    dataType: 'TEXT'
  },
  {
    dropDownName: 'rule.comparator.isMissing',
    tableName: 'rule.comparator.isMissingLabel',
    value: 'IS_MISSING',
    dataType: 'TEXT'
  }
];

export const SYNC_MODES = {
  none: 'NO_SYNC',
  oneWay: 'ONE_WAY',
  biDirectional: 'BI_DIRECTIONAL'
};

export const SYNC_MODE_LABELS = {
  none: 'NO SYNC',
  oneWay: 'ONE WAY',
  biDirectional: 'BI-DIRECTIONAL'
};
