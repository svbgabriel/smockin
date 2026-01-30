import { Component, ElementRef, OnInit, ViewChild } from '@angular/core';
import { DatePipe, NgFor, NgIf, SlicePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { forkJoin } from 'rxjs';
import { HttpClient, HttpHeaders } from '@angular/common/http';

import { ApiService } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';
import { AlertsComponent } from '../../shared/alerts/alerts.component';
import {
  Alert,
  RestMockDetail,
  RestMockDefinition,
  RestMockGroup,
  RestMockResponse,
  RuleCondition,
  RuleGroup,
  RuleResponse,
  ServerStatusResponse,
  TunnelResponse,
  RuleComparator,
  RuleMatchingType
} from '../../core/models';
import { downloadBase64File } from '../../core/file-utils';
import {
  ACTIVE_STATUS,
  CONTENT_MIME_TYPES,
  HTTP_METHODS,
  INACTIVE_STATUS,
  MOCK_TYPES,
  RULE_COMPARATORS,
  RULE_MATCHING_TYPES,
  WEBSOCKET_RULE_MATCHING_TYPES
} from '../../core/constants';
import { formatJs, formatJson, highlightJs, validateJson, validateXml } from '../../core/formatters';
import { ModalComponent } from '../../shared/modal/modal.component';
import { ServerConfigPanelComponent } from '../../shared/panels/server-config-panel.component';
import { ProxyMappingsPanelComponent } from '../../shared/panels/proxy-mappings-panel.component';
import { I18nPipe } from '../../core/i18n.pipe';
import { I18nService } from '../../core/i18n.service';

interface ResponseHeaderRow {
  name: string | null;
  value: string | null;
}

interface ProxyEndpointForm {
  contentType: string | null;
  httpStatusCode: number | null;
  responseBody: string | null;
}

interface EndpointForm {
  path: string | null;
  method: string | null;
  status: string;
  mockType: string;
  contentType: string | null;
  httpStatusCode: number | null;
  responseBody: string | null;
  proxyTimeout: number;
  webSocketTimeout: number;
  sseHeartbeat: number;
  wsPushIdOnConnect: boolean;
  ssePushIdOnConnect: boolean;
  randomiseDefinitions: boolean;
  randomiseLatency: boolean;
  randomiseLatencyRangeMinMillis: number;
  randomiseLatencyRangeMaxMillis: number;
  definitions: RestMockDefinition[];
  rules: RuleResponse[];
  customJsSyntax: string | null;
  statefulIdFieldName: string | null;
  statefulIdFieldLocation: string | null;
  createdBy?: string;
}

@Component({
  selector: 'app-tcp-endpoint',
  standalone: true,
  imports: [
    AlertsComponent,
    DatePipe,
    FormsModule,
    I18nPipe,
    ModalComponent,
    NgFor,
    NgIf,
    SlicePipe,
    ProxyMappingsPanelComponent,
    ServerConfigPanelComponent
  ],
  templateUrl: './tcp-endpoint.component.html',
  styleUrls: ['./tcp-endpoint.component.css']
})
export class TcpEndpointComponent implements OnInit {
  @ViewChild('customJsHighlightEl') customJsHighlightRef?: ElementRef<HTMLPreElement>;

  alerts: Alert[] = [];
  mockServerStatus = 'Stopped';
  activeTunnelUrl: string | null = null;
  tunnelEnabled = false;
  tunnelBusy = false;
  restServices: RestMockGroup[] = [];
  allRestServices: RestMockGroup[] = [];
  mockSelection = new Set<string>();
  searchFilter = '';
  isBusy = false;

  showImport = false;
  importType: 'smockin' | 'raml' = 'smockin';
  keepExisting = false;
  importFile: File | null = null;

  showServerConfig = false;
  showProxyMappings = false;

  viewMode: 'list' | 'editor' = 'list';
  isNew = false;
  currentExtId: string | null = null;
  endpoint: EndpointForm = this.defaultEndpoint();
  responseHeaderList: ResponseHeaderRow[] = [];
  proxyEndpoint: ProxyEndpointForm = { contentType: null, httpStatusCode: null, responseBody: null };
  activeWsClients: Array<{ id: string; dateJoined: string }> = [];
  activeSseClients: Array<{ id: string; dateJoined: string }> = [];
  pathPlaceholderKey = '';
  defaultCtxPathPrefix: string | null = null;

  ruleEditorOpen = false;
  ruleDraft: RuleResponse = this.defaultRule();
  ruleHeaderList: ResponseHeaderRow[] = [];
  ruleEditingIndex = -1;

  groupEditorOpen = false;
  groupDraft: RuleGroup = { extId: null, orderNo: 0, conditions: [] };
  groupEditingIndex = -1;
  conditionDraft: RuleCondition = this.defaultCondition();
  conditionArgs: RuleCondition[] = [];

  seqEditorOpen = false;
  seqDraft: RestMockDefinition = this.defaultSeq();
  seqHeaderList: ResponseHeaderRow[] = [];
  seqEditingIndex = -1;

  wsMessageOpen = false;
  sseMessageOpen = false;
  messageDraft = { path: '', sessionId: '', body: '' };
  customJsHighlight = '';

  readonly statusRunning = 'Running';
  readonly statusStopped = 'Stopped';
  readonly statusRestarting = 'Restarting';
  readonly activeStatus = ACTIVE_STATUS;
  readonly inactiveStatus = INACTIVE_STATUS;
  readonly httpMethods = HTTP_METHODS;
  readonly contentTypes = CONTENT_MIME_TYPES;
  readonly mockTypes = MOCK_TYPES;
  readonly responseBodyLimit = 100;

  private readonly minTimeoutMillis = 10000;
  private readonly maxProxyTimeoutMillis = 1800000;
  private readonly maxWebSocketTimeoutMillis = 3600000;

  private readonly httpPathPlaceholderKey = 'httpEndpoint.placeholders.httpPath';
  private readonly wsPathPlaceholderKey = 'httpEndpoint.placeholders.wsPath';
  private readonly ssePathPlaceholderKey = 'httpEndpoint.placeholders.ssePath';

  constructor(
    private readonly api: ApiService,
    private readonly auth: AuthService,
    private readonly router: Router,
    private readonly route: ActivatedRoute,
    private readonly http: HttpClient,
    private readonly i18n: I18nService
  ) {
    this.pathPlaceholderKey = this.httpPathPlaceholderKey;
  }

  ngOnInit(): void {
    this.loadTableData();
    this.loadServerStatus();
    this.loadTunnel();

    this.route.queryParamMap.subscribe((params) => {
      const extId = params.get('eid');
      const isNew = params.get('new');

      if (extId) {
        this.openEditor(extId);
        return;
      }

      if (isNew) {
        this.openEditor();
        return;
      }

      this.viewMode = 'list';
      this.currentExtId = null;
    });
  }

  get isReadOnly(): boolean {
    return this.auth.isLoggedIn() && !this.auth.isAdmin();
  }

  get isOwnerReadOnly(): boolean {
    if (!this.endpoint.createdBy || !this.auth.isLoggedIn()) {
      return false;
    }
    return this.endpoint.createdBy !== this.auth.getUserName();
  }

  get isEditorReadOnly(): boolean {
    return this.isReadOnly || this.isOwnerReadOnly;
  }

  get matchingTypes(): RuleMatchingType[] {
    return this.endpoint.mockType === 'RULE_WS' ? WEBSOCKET_RULE_MATCHING_TYPES : RULE_MATCHING_TYPES;
  }

  get proxyQueuePath(): string {
    return this.currentExtId ? `/proxy/${this.currentExtId}` : '';
  }

  translateMockType(key: string): string {
    const match = this.mockTypes.find((item) => item.value === key);
    return match ? this.i18n.t(match.name) : key;
  }

  toggleSelection(item: RestMockResponse): void {
    if (this.mockSelection.has(item.extId)) {
      this.mockSelection.delete(item.extId);
      return;
    }
    this.mockSelection.add(item.extId);
  }

  isSelected(extId: string): boolean {
    return this.mockSelection.has(extId);
  }

  selectAll(): void {
    this.mockSelection.clear();
    this.restServices.forEach((group) => group.data.forEach((item) => this.mockSelection.add(item.extId)));
  }

  clearSelection(): void {
    this.mockSelection.clear();
  }

  expandAll(): void {
    this.restServices.forEach((group) => (group.isOpen = true));
  }

  collapseAll(): void {
    this.restServices.forEach((group) => (group.isOpen = false));
  }

  filterMocks(): void {
    const filter = this.searchFilter.trim();
    if (!filter) {
      this.restServices = this.allRestServices.map((group) => ({ ...group, data: [...group.data] }));
      return;
    }

    const filtered: RestMockGroup[] = [];
    this.allRestServices.forEach((group) => {
      group.data.forEach((item) => {
        if (item.path.includes(filter)) {
          this.batchData(filtered, item, group.basePath);
        }
      });
    });
    this.restServices = filtered;
    this.clearSelection();
  }

  startServer(): void {
    if (this.isReadOnly) {
      return;
    }
    this.isBusy = true;
    this.api.post<{ port: number; nativeProperties?: Record<string, string> }>('/mockedserver/rest/start', {}).subscribe({
      next: (data) => {
        this.isBusy = false;
        this.mockServerStatus = this.statusRunning;
        let message = this.i18n.t('httpEndpoint.alerts.serverStarted', { port: data.port });
        if (data.nativeProperties?.['PROXY_SERVER_ENABLED'] === 'TRUE') {
          message = this.i18n.t('httpEndpoint.alerts.serverStartedWithProxy', { port: data.port, proxyPort: 8010 });
        }
        this.showAlert(message, 'success');
        this.loadTableData();
      },
      error: () => {
        this.isBusy = false;
        this.showAlert(this.i18n.t('common.errors.generic'));
      }
    });
  }

  stopServer(): void {
    if (this.isReadOnly) {
      return;
    }
    this.isBusy = true;
    this.api.post<void>('/mockedserver/rest/stop', {}).subscribe({
      next: () => {
        this.isBusy = false;
        this.mockServerStatus = this.statusStopped;
        this.showAlert(this.i18n.t('httpEndpoint.alerts.serverStopped'), 'success');
        this.loadTableData();
      },
      error: () => {
        this.isBusy = false;
        this.showAlert(this.i18n.t('common.errors.generic'));
      }
    });
  }

  exportSelection(): void {
    if (this.mockSelection.size === 0) {
      this.showAlert(this.i18n.t('httpEndpoint.alerts.exportNoneSelected'));
      return;
    }
    if (!confirm(this.i18n.t('httpEndpoint.confirm.exportSelected'))) {
      return;
    }

    const ids = Array.from(this.mockSelection);
    this.api.post<string>('/mock/export/RESTFUL', ids).subscribe({
      next: (data) => {
        downloadBase64File(data, `smockin_export_${ids.length}_mocks.zip`, 'application/zip');
        this.clearSelection();
      },
      error: () => this.showAlert(this.i18n.t('common.errors.generic'))
    });
  }

  deleteSelection(): void {
    if (this.mockSelection.size === 0) {
      this.showAlert(this.i18n.t('httpEndpoint.alerts.deleteNoneSelected'));
      return;
    }
    if (!confirm(this.i18n.t('httpEndpoint.confirm.deleteSelected'))) {
      return;
    }

    this.isBusy = true;
    const deletes = Array.from(this.mockSelection).map((id) => this.api.delete<void>(`/restmock/${id}`));
    forkJoin(deletes).subscribe({
      next: () => {
        this.isBusy = false;
        this.showAlert(this.i18n.t('httpEndpoint.alerts.deleteSuccess'), 'success');
        this.clearSelection();
        this.loadTableData();
      },
      error: () => {
        this.isBusy = false;
        this.showAlert(this.i18n.t('httpEndpoint.alerts.deletePartial'));
        this.loadTableData();
      }
    });
  }

  copyToClipboard(text: string | null): void {
    if (!text) {
      return;
    }
    navigator.clipboard.writeText(text);
    this.showAlert(this.i18n.t('common.alerts.copied'), 'success');
  }

  openNewEndpoint(): void {
    this.router.navigate([], { queryParams: { new: 'true' }, queryParamsHandling: 'merge' });
  }

  openEditor(extId?: string): void {
    this.viewMode = 'editor';
    this.resetEditor();

    if (!extId) {
      this.isNew = true;
      this.currentExtId = null;
      this.endpoint = this.defaultEndpoint();
      this.defaultCtxPathPrefix = this.auth.isLoggedIn() && !this.auth.isSysAdmin()
        ? `/${this.auth.getUserName()}`
        : null;
      return;
    }

    this.isNew = false;
    this.currentExtId = extId;
    this.loadEndpoint(extId);
  }

  closeEditor(): void {
    this.router.navigate([], { queryParams: { eid: null, new: null }, queryParamsHandling: 'merge' });
    this.viewMode = 'list';
    this.loadTableData();
  }

  onMockTypeChange(): void {
    switch (this.endpoint.mockType) {
      case 'PROXY_WS':
        this.pathPlaceholderKey = this.wsPathPlaceholderKey;
        this.endpoint.method = 'GET';
        break;
      case 'PROXY_SSE':
        this.pathPlaceholderKey = this.ssePathPlaceholderKey;
        this.endpoint.method = 'GET';
        break;
      case 'RULE_WS':
        this.pathPlaceholderKey = this.wsPathPlaceholderKey;
        this.endpoint.method = 'GET';
        break;
      case 'CUSTOM_JS':
        this.pathPlaceholderKey = this.httpPathPlaceholderKey;
        if (!this.endpoint.customJsSyntax) {
          this.endpoint.customJsSyntax = this.defaultJsTemplate();
        }
        this.updateCustomJsHighlight(this.endpoint.customJsSyntax);
        break;
      case 'STATEFUL':
        this.pathPlaceholderKey = this.httpPathPlaceholderKey;
        this.endpoint.method = 'ALL METHODS';
        this.endpoint.contentType = 'application/json';
        this.endpoint.responseBody = '[]';
        break;
      default:
        this.pathPlaceholderKey = this.httpPathPlaceholderKey;
        break;
    }
  }

  addResponseHeaderRow(): void {
    this.responseHeaderList.push({ name: null, value: null });
  }

  removeResponseHeaderRow(index: number): void {
    this.responseHeaderList.splice(index, 1);
  }

  moveSeqUp(index: number): void {
    if (index === 0 || this.endpoint.definitions[index].suspend) {
      return;
    }
    const item = this.endpoint.definitions[index];
    this.endpoint.definitions.splice(index, 1);
    this.endpoint.definitions.splice(index - 1, 0, item);
    this.updateSeqOrderNumbers();
  }

  moveSeqDown(index: number): void {
    if (index + 1 >= this.endpoint.definitions.length || this.endpoint.definitions[index].suspend) {
      return;
    }
    const item = this.endpoint.definitions[index];
    this.endpoint.definitions.splice(index, 1);
    this.endpoint.definitions.splice(index + 1, 0, item);
    this.updateSeqOrderNumbers();
  }

  toggleSeqSuspend(index: number): void {
    this.endpoint.definitions[index].suspend = !this.endpoint.definitions[index].suspend;
    if (this.countActiveDefinitions(this.endpoint.definitions) < 2) {
      this.endpoint.randomiseDefinitions = false;
    }
  }

  removeSeq(index: number): void {
    if (this.endpoint.definitions[index].suspend) {
      return;
    }
    if (!confirm('Remove this sequenced response? (You will need to save for this to take effect)')) {
      return;
    }
    this.endpoint.definitions.splice(index, 1);
    this.updateSeqOrderNumbers();
    if (this.countActiveDefinitions(this.endpoint.definitions) < 2) {
      this.endpoint.randomiseDefinitions = false;
    }
  }

  moveRuleUp(index: number): void {
    if (index === 0 || this.endpoint.rules[index].suspend) {
      return;
    }
    const item = this.endpoint.rules[index];
    this.endpoint.rules.splice(index, 1);
    this.endpoint.rules.splice(index - 1, 0, item);
    this.updateRuleOrderNumbers();
  }

  moveRuleDown(index: number): void {
    if (index + 1 >= this.endpoint.rules.length || this.endpoint.rules[index].suspend) {
      return;
    }
    const item = this.endpoint.rules[index];
    this.endpoint.rules.splice(index, 1);
    this.endpoint.rules.splice(index + 1, 0, item);
    this.updateRuleOrderNumbers();
  }

  toggleRuleSuspend(index: number): void {
    this.endpoint.rules[index].suspend = !this.endpoint.rules[index].suspend;
  }

  removeRule(index: number): void {
    if (this.endpoint.rules[index].suspend) {
      return;
    }
    if (!confirm(this.i18n.t('httpEndpoint.rule.confirm.remove'))) {
      return;
    }
    this.endpoint.rules.splice(index, 1);
    this.updateRuleOrderNumbers();
  }

  openRuleEditor(rule?: RuleResponse, index?: number): void {
    this.ruleEditingIndex = index ?? -1;
    this.ruleDraft = rule ? this.cloneRule(rule) : this.defaultRule();
    this.ruleHeaderList = this.mapHeadersToList(this.ruleDraft.responseHeaders);
    this.ruleEditorOpen = true;
  }

  closeRuleEditor(): void {
    this.ruleEditorOpen = false;
  }

  saveRule(): void {
    if (this.isBlank(this.ruleDraft.responseContentType)) {
      this.showAlert(this.i18n.t('httpEndpoint.rule.errors.contentTypeRequired'));
      return;
    }
    if (!this.isNumeric(this.ruleDraft.httpStatusCode)) {
      this.showAlert(this.i18n.t('httpEndpoint.rule.errors.statusRequired'));
      return;
    }
    if (this.ruleDraft.groups.length === 0) {
      this.showAlert(this.i18n.t('httpEndpoint.rule.errors.noConditions'));
      return;
    }

    const headers = this.buildHeaderMap(this.ruleHeaderList);
    if (!headers) {
      return;
    }
    this.ruleDraft.responseHeaders = headers;

    if (this.ruleEditingIndex >= 0) {
      this.endpoint.rules.splice(this.ruleEditingIndex, 1, this.ruleDraft);
    } else {
      this.ruleDraft.orderNo = this.endpoint.rules.length + 1;
      this.endpoint.rules.push(this.ruleDraft);
    }
    this.ruleEditorOpen = false;
  }

  formatRuleJson(): void {
    if (!this.ruleDraft.responseBody) {
      return;
    }
    const result = formatJson(this.ruleDraft.responseBody);
    if (!result.ok) {
      this.showAlert(result.error || this.i18n.t('common.errors.invalidJson'));
      return;
    }
    this.ruleDraft.responseBody = result.value;
  }

  openGroupEditor(group?: RuleGroup, index?: number): void {
    this.groupEditingIndex = index ?? -1;
    this.groupDraft = group ? this.cloneGroup(group) : { extId: null, orderNo: 0, conditions: [] };
    this.conditionArgs = [...this.groupDraft.conditions];
    this.conditionDraft = this.defaultCondition();
    this.groupEditorOpen = true;
  }

  closeGroupEditor(): void {
    this.groupEditorOpen = false;
  }

  addConditionArg(): void {
    if (!this.conditionDraft.matchType) {
      this.showAlert(this.i18n.t('httpEndpoint.group.errors.matchOnRequired'));
      return;
    }

    const matchTypeValue = this.conditionDraft.matchType.value;
    if (
      (matchTypeValue === 'PATH_VARIABLE' ||
        matchTypeValue === 'REQUEST_HEADER' ||
        matchTypeValue === 'REQUEST_PARAM') &&
      this.isBlank(this.conditionDraft.fieldName)
    ) {
      this.showAlert(
        this.i18n.t('httpEndpoint.group.errors.keyNameRequired', {
          name: this.i18n.t(this.conditionDraft.matchType.name)
        })
      );
      return;
    }

    if (matchTypeValue === 'PATH_VARIABLE_WILD' && !this.isNumeric(this.conditionDraft.fieldName)) {
      this.showAlert(this.i18n.t('httpEndpoint.group.errors.pathWildcardPosition'));
      return;
    }

    if (!this.conditionDraft.comparator) {
      this.showAlert(this.i18n.t('httpEndpoint.group.errors.comparatorRequired'));
      return;
    }

    if (this.conditionDraft.comparator.value !== 'IS_MISSING' && this.isBlank(this.conditionDraft.matchValue)) {
      this.showAlert(this.i18n.t('httpEndpoint.group.errors.matchValueRequired'));
      return;
    }

    if (this.conditionDraft.comparator.dataType === 'NUMERIC' && !this.isNumeric(this.conditionDraft.matchValue)) {
      this.showAlert(this.i18n.t('httpEndpoint.group.errors.matchValueNumeric'));
      return;
    }

    this.conditionArgs.push({ ...this.conditionDraft });
    this.conditionDraft = this.defaultCondition();
  }

  saveGroup(): void {
    if (this.conditionArgs.length === 0) {
      this.showAlert(this.i18n.t('httpEndpoint.group.errors.noArguments'));
      return;
    }

    const group: RuleGroup = {
      extId: this.groupDraft.extId ?? null,
      orderNo: this.groupEditingIndex >= 0 ? this.groupDraft.orderNo : this.ruleDraft.groups.length + 1,
      conditions: [...this.conditionArgs]
    };

    if (this.groupEditingIndex >= 0) {
      this.ruleDraft.groups.splice(this.groupEditingIndex, 1, group);
    } else {
      this.ruleDraft.groups.push(group);
    }

    this.groupEditorOpen = false;
  }

  moveConditionUp(index: number): void {
    if (index === 0) {
      return;
    }
    const item = this.ruleDraft.groups[index];
    this.ruleDraft.groups.splice(index, 1);
    this.ruleDraft.groups.splice(index - 1, 0, item);
  }

  moveConditionDown(index: number): void {
    if (index + 1 >= this.ruleDraft.groups.length) {
      return;
    }
    const item = this.ruleDraft.groups[index];
    this.ruleDraft.groups.splice(index, 1);
    this.ruleDraft.groups.splice(index + 1, 0, item);
  }

  removeCondition(index: number): void {
    this.ruleDraft.groups.splice(index, 1);
  }

  moveConditionArgUp(index: number): void {
    if (index === 0) {
      return;
    }
    const item = this.conditionArgs[index];
    this.conditionArgs.splice(index, 1);
    this.conditionArgs.splice(index - 1, 0, item);
  }

  moveConditionArgDown(index: number): void {
    if (index + 1 >= this.conditionArgs.length) {
      return;
    }
    const item = this.conditionArgs[index];
    this.conditionArgs.splice(index, 1);
    this.conditionArgs.splice(index + 1, 0, item);
  }

  removeConditionArg(index: number): void {
    this.conditionArgs.splice(index, 1);
  }

  onMatchTypeChange(matchType: RuleMatchingType): void {
    this.conditionDraft.matchType = matchType;
    this.conditionDraft.comparator = this.getComparators(matchType)[0] ?? this.conditionDraft.comparator;
    this.conditionDraft.dataType = this.conditionDraft.comparator?.dataType ?? null;
    if (matchType.value === 'REQUEST_BODY') {
      this.conditionDraft.fieldName = null;
    }
  }

  onComparatorChange(comparator: RuleComparator): void {
    this.conditionDraft.comparator = comparator;
    this.conditionDraft.dataType = comparator.dataType;
    if (comparator.value === 'IS_MISSING') {
      this.conditionDraft.matchValue = null;
    }
  }

  getComparators(matchType: RuleMatchingType): RuleComparator[] {
    if (matchType.value === 'REQUEST_BODY') {
      return [
        {
          dropDownName: 'rule.comparator.equalsText',
          tableName: 'rule.comparator.equalsLabel',
          value: 'EQUALS',
          dataType: 'TEXT'
        },
        {
          dropDownName: 'rule.comparator.containsText',
          tableName: 'rule.comparator.containsLabel',
          value: 'CONTAINS',
          dataType: 'TEXT'
        }
      ];
    }
    if (matchType.value === 'PATH_VARIABLE') {
      return [
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
        }
      ];
    }
    return RULE_COMPARATORS;
  }

  openSeqEditor(seq?: RestMockDefinition, index?: number): void {
    this.seqEditingIndex = index ?? -1;
    this.seqDraft = seq ? { ...seq } : this.defaultSeq();
    this.seqHeaderList = this.mapHeadersToList(this.seqDraft.responseHeaders);
    this.seqEditorOpen = true;
  }

  closeSeqEditor(): void {
    this.seqEditorOpen = false;
  }

  saveSeq(): void {
    if (this.isBlank(this.seqDraft.responseContentType)) {
      this.showAlert(this.i18n.t('httpEndpoint.sequence.errors.contentTypeRequired'));
      return;
    }
    if (!this.isNumeric(this.seqDraft.httpStatusCode)) {
      this.showAlert(this.i18n.t('httpEndpoint.sequence.errors.statusRequired'));
      return;
    }
    if (!this.isNumeric(this.seqDraft.frequencyCount) || this.seqDraft.frequencyCount < 1) {
      this.showAlert(this.i18n.t('httpEndpoint.sequence.errors.occurrenceRequired'));
      return;
    }

    const headers = this.buildHeaderMap(this.seqHeaderList);
    if (!headers) {
      return;
    }
    this.seqDraft.responseHeaders = headers;

    if (this.seqEditingIndex >= 0) {
      this.endpoint.definitions.splice(this.seqEditingIndex, 1, this.seqDraft);
    } else {
      this.seqDraft.orderNo = this.endpoint.definitions.length + 1;
      this.endpoint.definitions.push(this.seqDraft);
    }

    this.seqEditorOpen = false;
  }

  formatSeqJson(): void {
    if (!this.seqDraft.responseBody) {
      return;
    }
    const result = formatJson(this.seqDraft.responseBody);
    if (!result.ok) {
      this.showAlert(result.error || this.i18n.t('common.errors.invalidJson'));
      return;
    }
    this.seqDraft.responseBody = result.value;
  }

  formatEndpointJson(): void {
    if (!this.endpoint.responseBody) {
      return;
    }
    const result = formatJson(this.endpoint.responseBody);
    if (!result.ok) {
      this.showAlert(result.error || this.i18n.t('common.errors.invalidJson'));
      return;
    }
    this.endpoint.responseBody = result.value;
  }

  formatProxyJson(): void {
    if (!this.proxyEndpoint.responseBody) {
      return;
    }
    const result = formatJson(this.proxyEndpoint.responseBody);
    if (!result.ok) {
      this.showAlert(result.error || this.i18n.t('common.errors.invalidJson'));
      return;
    }
    this.proxyEndpoint.responseBody = result.value;
  }

  formatCustomJs(): void {
    if (!this.endpoint.customJsSyntax) {
      return;
    }
    const result = formatJs(this.endpoint.customJsSyntax);
    if (!result.ok) {
      this.showAlert(this.i18n.t('common.errors.generic'));
      return;
    }
    this.endpoint.customJsSyntax = result.value;
    this.updateCustomJsHighlight(this.endpoint.customJsSyntax);
  }

  updateCustomJsHighlight(value: string | null): void {
    const highlighted = highlightJs(value ?? '');
    this.customJsHighlight = highlighted || '&nbsp;';
  }

  syncCustomJsScroll(event: Event): void {
    const target = event.target as HTMLTextAreaElement | null;
    const highlight = this.customJsHighlightRef?.nativeElement;
    if (!target || !highlight) {
      return;
    }
    highlight.scrollTop = target.scrollTop;
    highlight.scrollLeft = target.scrollLeft;
  }

  validateXmlResponse(text: string): void {
    const error = validateXml(text);
    if (error) {
      this.showAlert(this.i18n.t('common.errors.invalidXml', { error }));
    }
  }

  postProxyResponse(): void {
    if (!this.currentExtId) {
      return;
    }
    if (this.isBlank(this.proxyEndpoint.contentType)) {
      this.showAlert(this.i18n.t('httpEndpoint.proxy.errors.contentTypeRequired'));
      return;
    }
    if (!this.isNumeric(this.proxyEndpoint.httpStatusCode)) {
      this.showAlert(this.i18n.t('httpEndpoint.proxy.errors.statusRequired'));
      return;
    }

    const req = {
      path: this.endpoint.path,
      method: this.endpoint.method,
      responseContentType: this.proxyEndpoint.contentType,
      httpStatusCode: this.proxyEndpoint.httpStatusCode,
      body: this.proxyEndpoint.responseBody
    };

    this.api.post<void>(`/proxy/${this.currentExtId}`, req).subscribe({
      next: () => this.showAlert(this.i18n.t('httpEndpoint.proxy.alerts.posted'), 'success'),
      error: () => this.showAlert(this.i18n.t('common.errors.generic'))
    });
  }

  clearProxyQueue(): void {
    if (!this.currentExtId) {
      return;
    }
    if (!confirm(this.i18n.t('httpEndpoint.proxy.confirm.clearQueue'))) {
      return;
    }
    this.api.patch<void>(`/proxy/${this.currentExtId}/clear`, { path: null }).subscribe({
      next: () => {},
      error: () => this.showAlert(this.i18n.t('common.errors.generic'))
    });
  }

  clearProxyFields(): void {
    this.proxyEndpoint = { contentType: null, httpStatusCode: null, responseBody: null };
  }

  refreshWsClients(): void {
    if (!this.currentExtId) {
      return;
    }
    this.api.get<Array<{ id: string; dateJoined: string }>>(`/ws/${this.currentExtId}/client`).subscribe({
      next: (data) => {
        this.activeWsClients = data || [];
      },
      error: () => this.showAlert(this.i18n.t('common.errors.generic'))
    });
  }

  refreshSseClients(): void {
    if (!this.currentExtId) {
      return;
    }
    this.api.get<Array<{ id: string; dateJoined: string }>>(`/sse/${this.currentExtId}/client`).subscribe({
      next: (data) => {
        this.activeSseClients = data || [];
      },
      error: () => this.showAlert(this.i18n.t('common.errors.generic'))
    });
  }

  openWsMessage(sessionId?: string): void {
    this.messageDraft = {
      path: this.getUserCtxPushPath(),
      sessionId: sessionId ?? '',
      body: ''
    };
    this.wsMessageOpen = true;
  }

  openSseMessage(sessionId?: string): void {
    this.messageDraft = {
      path: this.getUserCtxPushPath(),
      sessionId: sessionId ?? '',
      body: ''
    };
    this.sseMessageOpen = true;
  }

  sendWsMessage(): void {
    if (this.isBlank(this.messageDraft.body)) {
      this.showAlert(this.i18n.t('httpEndpoint.messages.errors.bodyRequired'));
      return;
    }
    const req = { path: this.messageDraft.path, body: this.messageDraft.body };
    const url = this.messageDraft.sessionId ? `/ws/${this.messageDraft.sessionId}` : '/ws';

    this.api.post<void>(url, req).subscribe({
      next: () => {
        this.wsMessageOpen = false;
        this.showAlert(this.i18n.t('httpEndpoint.messages.wsSent'), 'success');
      },
      error: () => this.showAlert(this.i18n.t('common.errors.generic'))
    });
  }

  sendSseMessage(): void {
    if (this.isBlank(this.messageDraft.body)) {
      this.showAlert(this.i18n.t('httpEndpoint.messages.errors.bodyRequired'));
      return;
    }
    const req = { path: this.messageDraft.path, body: this.messageDraft.body };
    const url = this.messageDraft.sessionId ? `/sse/${this.messageDraft.sessionId}` : '/sse';

    this.api.post<void>(url, req).subscribe({
      next: () => {
        this.sseMessageOpen = false;
        this.showAlert(this.i18n.t('httpEndpoint.messages.sseSent'), 'success');
      },
      error: () => this.showAlert(this.i18n.t('common.errors.generic'))
    });
  }

  clearDataState(): void {
    if (!this.currentExtId) {
      return;
    }
    if (!confirm(this.i18n.t('httpEndpoint.stateful.confirm.clearData'))) {
      return;
    }

    this.api.post<void>(`/stateful/${this.currentExtId}/clear`, {}).subscribe({
      next: () => this.showAlert(this.i18n.t('httpEndpoint.stateful.alerts.dataCleared'), 'success'),
      error: () => this.showAlert(this.i18n.t('common.errors.generic'))
    });
  }

  saveEndpoint(): void {
    if (this.isBlank(this.endpoint.path)) {
      this.showAlert(this.i18n.t('httpEndpoint.errors.pathRequired'));
      return;
    }

    const mockType = this.endpoint.mockType;
    if (mockType === 'RULE' || mockType === 'RULE_WS') {
      if (!this.validateRule()) {
        return;
      }
    } else if (mockType === 'SEQ') {
      if (!this.validateSeq()) {
        return;
      }
    } else if (mockType === 'PROXY_HTTP') {
      if (!this.validateProxy()) {
        return;
      }
    } else if (mockType === 'PROXY_WS') {
      if (!this.validateWebSocket()) {
        return;
      }
    } else if (mockType === 'PROXY_SSE') {
      if (!this.validateSse()) {
        return;
      }
    } else if (mockType === 'CUSTOM_JS') {
      if (!this.validateCustomJS()) {
        return;
      }
    } else if (mockType === 'STATEFUL') {
      if (!this.validateStateful()) {
        return;
      }
    }

    if (this.endpoint.randomiseLatency) {
      if (this.endpoint.randomiseLatencyRangeMinMillis < 0) {
        this.showAlert(this.i18n.t('httpEndpoint.latency.errors.minNegative'));
        return;
      }
      if (this.endpoint.randomiseLatencyRangeMaxMillis < 100) {
        this.showAlert(this.i18n.t('httpEndpoint.latency.errors.maxTooShort'));
        return;
      }
      if (this.endpoint.randomiseLatencyRangeMinMillis > this.endpoint.randomiseLatencyRangeMaxMillis) {
        this.showAlert(this.i18n.t('httpEndpoint.latency.errors.minGreaterThanMax'));
        return;
      }
    } else {
      this.endpoint.randomiseLatencyRangeMinMillis = 0;
      this.endpoint.randomiseLatencyRangeMaxMillis = 0;
    }

    const req: any = {
      path: this.endpoint.path,
      method: this.endpoint.method,
      status: this.endpoint.status,
      mockType: this.endpoint.mockType,
      proxyTimeoutInMillis: this.endpoint.proxyTimeout,
      webSocketTimeoutInMillis: this.endpoint.webSocketTimeout,
      sseHeartBeatInMillis: this.endpoint.sseHeartbeat,
      proxyPushIdOnConnect: false,
      randomiseDefinitions: this.endpoint.randomiseDefinitions,
      randomiseLatency: this.endpoint.randomiseLatency,
      randomiseLatencyRangeMinMillis: this.endpoint.randomiseLatencyRangeMinMillis,
      randomiseLatencyRangeMaxMillis: this.endpoint.randomiseLatencyRangeMaxMillis,
      definitions: [],
      rules: [],
      customJsSyntax: null,
      statefulDefaultResponseBody: null
    };

    if (mockType === 'SEQ') {
      req.definitions = [...this.endpoint.definitions];
      if (this.countActiveDefinitions(req.definitions) < 2) {
        req.randomiseDefinitions = false;
      }
    } else if (mockType === 'RULE' || mockType === 'RULE_WS') {
      const defaultDef: RestMockDefinition = {
        extId: null,
        orderNo: 1,
        responseContentType: this.endpoint.contentType || 'application/json',
        httpStatusCode: Number(this.endpoint.httpStatusCode || 200),
        responseBody: this.endpoint.responseBody,
        sleepInMillis: 0,
        suspend: false,
        frequencyCount: 1,
        frequencyPercentage: 0,
        responseHeaders: this.buildHeaderMap(this.responseHeaderList) || {}
      };
      req.definitions = [defaultDef];
      req.rules = this.endpoint.rules.map((rule) => this.toRuleDto(rule));
    } else if (mockType === 'PROXY_WS') {
      req.proxyPushIdOnConnect = this.endpoint.wsPushIdOnConnect;
    } else if (mockType === 'PROXY_SSE') {
      req.proxyPushIdOnConnect = this.endpoint.ssePushIdOnConnect;
    } else if (mockType === 'CUSTOM_JS') {
      req.customJsSyntax = this.endpoint.customJsSyntax;
    } else if (mockType === 'STATEFUL') {
      req.method = 'GET';
      req.statefulDefaultResponseBody = this.endpoint.responseBody || '[]';
      req.statefulIdFieldName = this.endpoint.statefulIdFieldName || 'id';
      req.statefulIdFieldLocation = req.statefulIdFieldName;
    }

    this.isBusy = true;
    const request = this.currentExtId
      ? this.api.put<void>(`/restmock/${this.currentExtId}`, req)
      : this.api.post<any>('/restmock', req);

    request.subscribe({
      next: () => {
        this.isBusy = false;
        this.showAlert(this.i18n.t('common.alerts.saved'), 'success');
        this.loadTableData();
      },
      error: (err) => {
        this.isBusy = false;
        if (err?.status === 400) {
          this.showAlert(err.error?.message || this.i18n.t('common.errors.invalidData'));
          return;
        }
        if (err?.status === 409) {
          this.showAlert(this.i18n.t('httpEndpoint.errors.pathInUse', { path: this.endpoint.path ?? '' }));
          return;
        }
        this.showAlert(this.i18n.t('common.errors.generic'));
      }
    });
  }

  deleteEndpoint(): void {
    if (!this.currentExtId) {
      return;
    }
    if (!confirm(this.i18n.t('httpEndpoint.confirm.deleteEndpoint'))) {
      return;
    }
    this.api.delete<void>(`/restmock/${this.currentExtId}`).subscribe({
      next: () => {
        this.showAlert(this.i18n.t('httpEndpoint.alerts.deleted'), 'success');
        this.closeEditor();
      },
      error: () => this.showAlert(this.i18n.t('common.errors.generic'))
    });
  }

  toggleImport(): void {
    this.showImport = !this.showImport;
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.importFile = input.files?.[0] ?? null;
  }

  importMocks(): void {
    if (!this.importFile) {
      this.showAlert(this.i18n.t('httpEndpoint.import.selectFile'));
      return;
    }

    const formData = new FormData();
    formData.append('file', this.importFile);
    const headers = new HttpHeaders({ KeepExisting: String(this.keepExisting) });

    this.isBusy = true;
    const url = this.importType === 'raml' ? '/api/RAML/import' : '/mock/import';
    this.http
      .post(url, formData, { observe: 'response', headers })
      .subscribe({
        next: (resp) => {
          this.isBusy = false;
          if (this.importType === 'raml' && resp.status !== 201) {
            this.showAlert(this.i18n.t('httpEndpoint.import.invalidFile'));
            return;
          }
          if (this.importType === 'smockin' && resp.status !== 200) {
            this.showAlert(this.i18n.t('httpEndpoint.import.failed'));
            return;
          }
          this.showAlert(this.i18n.t('httpEndpoint.import.success'), 'success');
          this.importFile = null;
          this.showImport = false;
          this.loadTableData();
        },
        error: (err) => {
          this.isBusy = false;
          this.showAlert(err?.error?.message || this.i18n.t('httpEndpoint.import.failed'));
        }
      });
  }

  onServerConfigClose(event?: { restartReq?: boolean; reload?: boolean } | void): void {
    this.showServerConfig = false;
    if (event && 'reload' in event && event.reload) {
      this.loadTableData();
    }
    if (event && 'restartReq' in event && event.restartReq) {
      this.loadServerStatus();
    }
  }

  onProxyMappingsClose(): void {
    this.showProxyMappings = false;
  }

  private loadEndpoint(extId: string): void {
    this.api.get<RestMockDetail>(`/restmock/${extId}`).subscribe({
      next: (data) => {
        this.currentExtId = data.extId;
        this.endpoint = this.mapEndpoint(data);
        this.defaultCtxPathPrefix = data.userCtxPath ? `/${data.userCtxPath}` : null;
        this.pathPlaceholderKey = this.httpPathPlaceholderKey;
        this.responseHeaderList = [];

        if (data.mockType === 'SEQ' || data.mockType === 'RULE' || data.mockType === 'RULE_WS') {
          const def = data.definitions?.[0];
          if (def) {
            this.endpoint.contentType = def.responseContentType;
            this.endpoint.httpStatusCode = def.httpStatusCode;
            this.endpoint.responseBody = def.responseBody;
            this.responseHeaderList = this.mapHeadersToList(def.responseHeaders);
          }
        }

        if (data.mockType === 'CUSTOM_JS') {
          this.endpoint.customJsSyntax = data.customJsSyntax;
        }

        if (data.mockType === 'STATEFUL') {
          this.endpoint.method = 'ALL METHODS';
          this.endpoint.contentType = 'application/json';
          this.endpoint.responseBody = data.statefulDefaultResponseBody ?? '[]';
        }

        this.onMockTypeChange();
      },
      error: () => this.showAlert(this.i18n.t('httpEndpoint.errors.loadEndpoint'))
    });
  }

  private mapEndpoint(data: RestMockDetail): EndpointForm {
    return {
      path: data.path,
      method: data.method,
      status: data.status,
      mockType: data.mockType,
      contentType: null,
      httpStatusCode: null,
      responseBody: null,
      proxyTimeout: data.proxyTimeoutInMillis,
      webSocketTimeout: data.webSocketTimeoutInMillis,
      sseHeartbeat: data.sseHeartBeatInMillis,
      wsPushIdOnConnect: data.proxyPushIdOnConnect,
      ssePushIdOnConnect: data.proxyPushIdOnConnect,
      randomiseDefinitions: data.randomiseDefinitions,
      randomiseLatency: data.randomiseLatency,
      randomiseLatencyRangeMinMillis: data.randomiseLatencyRangeMinMillis,
      randomiseLatencyRangeMaxMillis: data.randomiseLatencyRangeMaxMillis,
      definitions: data.definitions ?? [],
      rules: (data.rules ?? []).map((rule) => this.hydrateRule(rule)),
      customJsSyntax: data.customJsSyntax,
      statefulIdFieldName: data.statefulIdFieldName ?? 'id',
      statefulIdFieldLocation: data.statefulIdFieldLocation ?? null,
      createdBy: data.createdBy
    };
  }

  private hydrateRule(rule: RuleResponse): RuleResponse {
    return {
      ...rule,
      groups: rule.groups.map((group) => ({
        ...group,
        conditions: group.conditions.map((cond: any) => ({
          matchType: this.findMatchingType(cond.ruleMatchingType),
          fieldName: cond.field,
          comparator: this.findComparator(cond.comparator, cond.dataType),
          dataType: cond.dataType,
          caseSensitive: cond.caseSensitive ?? false,
          matchValue: cond.value
        }))
      }))
    };
  }

  private toRuleDto(rule: RuleResponse): any {
    return {
      ...rule,
      groups: rule.groups.map((group) => ({
        ...group,
        conditions: group.conditions.map((cond) => ({
          extId: null,
          ruleMatchingType: cond.matchType.value,
          field: cond.fieldName,
          comparator: cond.comparator.value,
          dataType: cond.dataType,
          caseSensitive: cond.caseSensitive,
          value: cond.matchValue
        }))
      })) as unknown as RuleResponse
    };
  }

  private findMatchingType(value: string): RuleMatchingType {
    return (
      RULE_MATCHING_TYPES.find((item) => item.value === value) ||
      WEBSOCKET_RULE_MATCHING_TYPES.find((item) => item.value === value) ||
      RULE_MATCHING_TYPES[0]
    );
  }

  private findComparator(value: string, dataType: string): RuleComparator {
    return RULE_COMPARATORS.find((item) => item.value === value && item.dataType === dataType) || RULE_COMPARATORS[0];
  }

  private getMatchingTypes(): RuleMatchingType[] {
    if (this.endpoint.mockType === 'RULE_WS') {
      return WEBSOCKET_RULE_MATCHING_TYPES;
    }
    return RULE_MATCHING_TYPES;
  }

  private updateSeqOrderNumbers(): void {
    this.endpoint.definitions.forEach((item, index) => {
      item.orderNo = index + 1;
    });
  }

  private updateRuleOrderNumbers(): void {
    this.endpoint.rules.forEach((item, index) => {
      item.orderNo = index + 1;
    });
  }

  private validateRule(): boolean {
    if (this.isBlank(this.endpoint.method)) {
      this.showAlert(this.i18n.t('httpEndpoint.validation.methodRequired'));
      return false;
    }
    if (this.isBlank(this.endpoint.contentType)) {
      this.showAlert(this.i18n.t('httpEndpoint.validation.defaultContentTypeRequired'));
      return false;
    }
    if (!this.isNumeric(this.endpoint.httpStatusCode)) {
      this.showAlert(this.i18n.t('httpEndpoint.validation.defaultStatusRequired'));
      return false;
    }

    const headers = this.buildHeaderMap(this.responseHeaderList, true);
    if (!headers) {
      return false;
    }
    return true;
  }

  private validateSeq(): boolean {
    if (this.isBlank(this.endpoint.method)) {
      this.showAlert(this.i18n.t('httpEndpoint.validation.methodRequired'));
      return false;
    }
    if (this.countActiveDefinitions(this.endpoint.definitions) === 0) {
      this.showAlert(this.i18n.t('httpEndpoint.validation.sequenceActiveRequired'));
      return false;
    }
    return true;
  }

  private validateProxy(): boolean {
    if (this.isBlank(this.endpoint.method)) {
      this.showAlert(this.i18n.t('httpEndpoint.validation.methodRequired'));
      return false;
    }
    if (!this.isNumeric(this.endpoint.proxyTimeout)) {
      this.showAlert(this.i18n.t('httpEndpoint.validation.proxyTimeoutRequired'));
      return false;
    }
    const timeout = Number(this.endpoint.proxyTimeout);
    if (timeout > 0 && timeout < this.minTimeoutMillis) {
      this.showAlert(
        this.i18n.t('httpEndpoint.validation.proxyTimeoutMin', {
          min: this.minTimeoutMillis,
          seconds: this.minTimeoutMillis / 1000
        })
      );
      return false;
    }
    if (timeout > this.maxProxyTimeoutMillis) {
      this.showAlert(
        this.i18n.t('httpEndpoint.validation.proxyTimeoutMax', {
          max: this.maxProxyTimeoutMillis,
          seconds: this.maxProxyTimeoutMillis / 1000
        })
      );
      return false;
    }
    return true;
  }

  private validateWebSocket(): boolean {
    if (!this.isNumeric(this.endpoint.webSocketTimeout)) {
      this.showAlert(this.i18n.t('httpEndpoint.validation.wsTimeoutRequired'));
      return false;
    }
    const timeout = Number(this.endpoint.webSocketTimeout);
    if (timeout > 0 && timeout < this.minTimeoutMillis) {
      this.showAlert(
        this.i18n.t('httpEndpoint.validation.wsTimeoutMin', {
          min: this.minTimeoutMillis,
          seconds: this.minTimeoutMillis / 1000
        })
      );
      return false;
    }
    if (timeout > this.maxWebSocketTimeoutMillis) {
      this.showAlert(
        this.i18n.t('httpEndpoint.validation.wsTimeoutMax', {
          max: this.maxWebSocketTimeoutMillis,
          seconds: this.maxWebSocketTimeoutMillis / 1000
        })
      );
      return false;
    }
    return true;
  }

  private validateSse(): boolean {
    if (!this.isNumeric(this.endpoint.sseHeartbeat) || this.endpoint.sseHeartbeat < 1000) {
      this.showAlert(this.i18n.t('httpEndpoint.validation.sseHeartbeatRequired'));
      return false;
    }
    return true;
  }

  private validateCustomJS(): boolean {
    if (this.isBlank(this.endpoint.method)) {
      this.showAlert(this.i18n.t('httpEndpoint.validation.methodRequired'));
      return false;
    }
    if (this.isBlank(this.endpoint.customJsSyntax)) {
      this.showAlert(this.i18n.t('httpEndpoint.validation.jsLogicRequired'));
      return false;
    }
    return true;
  }

  private validateStateful(): boolean {
    const path = this.endpoint.path || '';
    const lastSlash = path.lastIndexOf('/');
    if (lastSlash !== -1 && path.substring(lastSlash).includes(':')) {
      this.showAlert(this.i18n.t('httpEndpoint.validation.statefulPathVariable'));
      return false;
    }
    if (this.endpoint.responseBody && validateJson(this.endpoint.responseBody)) {
      this.showAlert(this.i18n.t('httpEndpoint.validation.statefulJson'));
      return false;
    }
    if (this.endpoint.statefulIdFieldName && !/^[a-zA-Z]+$/.test(this.endpoint.statefulIdFieldName)) {
      this.showAlert(this.i18n.t('httpEndpoint.validation.statefulIdField'));
      return false;
    }
    return true;
  }

  private defaultEndpoint(): EndpointForm {
    return {
      path: null,
      method: null,
      status: ACTIVE_STATUS,
      mockType: 'SEQ',
      contentType: null,
      httpStatusCode: 200,
      responseBody: null,
      proxyTimeout: 0,
      webSocketTimeout: 0,
      sseHeartbeat: 0,
      wsPushIdOnConnect: false,
      ssePushIdOnConnect: false,
      randomiseDefinitions: false,
      randomiseLatency: false,
      randomiseLatencyRangeMinMillis: 0,
      randomiseLatencyRangeMaxMillis: 0,
      definitions: [],
      rules: [],
      customJsSyntax: null,
      statefulIdFieldName: 'id',
      statefulIdFieldLocation: null
    };
  }

  private defaultRule(): RuleResponse {
    return {
      extId: null,
      orderNo: 0,
      responseContentType: 'application/json',
      httpStatusCode: 200,
      responseBody: null,
      sleepInMillis: 0,
      suspend: false,
      responseHeaders: {},
      groups: []
    };
  }

  private defaultSeq(): RestMockDefinition {
    return {
      extId: null,
      orderNo: 0,
      httpStatusCode: 200,
      responseContentType: 'application/json',
      responseBody: null,
      sleepInMillis: 0,
      suspend: false,
      frequencyCount: 1,
      frequencyPercentage: 0,
      responseHeaders: {}
    };
  }

  private defaultCondition(): RuleCondition {
    return {
      matchType: this.getMatchingTypes()[0],
      fieldName: null,
      comparator: RULE_COMPARATORS[0],
      dataType: RULE_COMPARATORS[0].dataType,
      caseSensitive: false,
      matchValue: null
    };
  }

  private mapHeadersToList(headers: Record<string, string>): ResponseHeaderRow[] {
    return Object.entries(headers || {}).map(([name, value]) => ({ name, value }));
  }

  private buildHeaderMap(rows: ResponseHeaderRow[], warn?: boolean): Record<string, string> | null {
    const map: Record<string, string> = {};
    for (const row of rows) {
      if (this.isBlank(row.name) || this.isBlank(row.value)) {
        if (warn) {
          this.showAlert(this.i18n.t('httpEndpoint.headers.errors.blankFields'));
        }
        return null;
      }
      if (row.name && map[row.name]) {
        this.showAlert(this.i18n.t('httpEndpoint.headers.errors.duplicate', { name: row.name }));
        return null;
      }
      if (row.name) {
        map[row.name] = row.value || '';
      }
    }
    return map;
  }

  private cloneRule(rule: RuleResponse): RuleResponse {
    return {
      ...rule,
      responseHeaders: { ...rule.responseHeaders },
      groups: rule.groups.map((group) => ({
        ...group,
        conditions: group.conditions.map((cond) => ({ ...cond }))
      }))
    };
  }

  private cloneGroup(group: RuleGroup): RuleGroup {
    return {
      ...group,
      conditions: group.conditions.map((cond) => ({ ...cond }))
    };
  }

  private resetEditor(): void {
    this.endpoint = this.defaultEndpoint();
    this.responseHeaderList = [];
    this.proxyEndpoint = { contentType: null, httpStatusCode: null, responseBody: null };
    this.activeWsClients = [];
    this.activeSseClients = [];
    this.pathPlaceholderKey = this.httpPathPlaceholderKey;
    this.defaultCtxPathPrefix = null;
    this.updateCustomJsHighlight('');
  }

  private getUserCtxPushPath(): string {
    if (this.defaultCtxPathPrefix) {
      return `${this.defaultCtxPathPrefix}${this.endpoint.path || ''}`;
    }
    return this.endpoint.path || '';
  }

  private defaultJsTemplate(): string {
    // language=javascript
    return (
        'function handleResponse(request, response) {\n\n' +
        '  // Reading the Request...\n' +
        '  // request.path;\n' +
        '  // request.pathVars.name;\n' +
        '  // request.body;\n' +
        '  // var jsonObj = JSON.parse(request.body);\n' +
        "  // request.headers['X-Inbound-Header'];\n" +
        "  // request.parameters['first-name'];\n" +
        '  // request.parameters.lastName;\n\n' +
        "  // Parsing XML... (e.g. <xml foo=\"FOO\"><bar><baz>BAZ</baz></bar></xml>)\n" +
        '  // var data = fromXML(request.body);\n' +
        '  // var bazValue = data.xml.bar.baz; // returns "BAZ"\n' +
        '  // var fooValue = data.xml["@foo"]; // returns "FOO"\n\n' +
        '  // Setting the Response...\n' +
        "  // response.contentType = 'application/json';\n" +
        '  // response.status = 200;\n' +
        "  // response.body = '{ \"msg\" : \"hello world...\" }';\n" +
        "  // response.headers['X-Outbound-Header'] = 'foobar';\n\n" +
        '  // Reading in Key/Value data...\n' +
        "  // lookUpKvp('name');\n" +
        '  // lookUpKvp(request.parameters.lastname);\n\n' +
        '  return response;\n' +
        '}\n'
    );
  }

  private loadTableData(): void {
    this.api.get<RestMockResponse[]>('/restmock').subscribe({
      next: (data) => {
        const filtered = this.filterOutStateful(data ?? []);
        this.allRestServices = this.batchByBasePath(filtered);
        this.restServices = this.allRestServices.map((group) => ({ ...group, data: [...group.data] }));
      },
      error: (err) => {
        if (err?.status === 401) {
          this.showAlert(this.i18n.t('common.errors.loginRequired'));
          return;
        }
        this.showAlert(this.i18n.t('common.errors.generic'));
      }
    });
  }

  private loadServerStatus(): void {
    this.api.get<ServerStatusResponse>('/mockedserver/rest/status').subscribe({
      next: (data) => {
        this.mockServerStatus = data.running ? this.statusRunning : this.statusStopped;
      },
      error: () => this.showAlert(this.i18n.t('common.errors.generic'))
    });
  }

  private loadTunnel(): void {
    this.api.get<TunnelResponse>('/tunnel').subscribe({
      next: (data) => {
        this.tunnelEnabled = data?.enabled ?? false;
        this.activeTunnelUrl = data?.enabled ? data.uri : null;
      },
      error: (err) => {
        if (err?.status === 401) {
          this.showAlert(this.i18n.t('common.errors.loginRequired'));
          return;
        }
        this.showAlert(this.i18n.t('common.errors.generic'));
      }
    });
  }

  toggleTunnel(): void {
    if (this.isReadOnly || this.tunnelBusy) {
      return;
    }

    this.tunnelBusy = true;
    const enabled = !this.tunnelEnabled;
    this.api.put<TunnelResponse>('/tunnel', { enabled }).subscribe({
      next: (data) => {
        this.tunnelBusy = false;
        this.tunnelEnabled = data?.enabled ?? enabled;
        this.activeTunnelUrl = data?.enabled ? data.uri : null;
      },
      error: (err) => {
        this.tunnelBusy = false;
        if (err?.status === 400) {
          this.showAlert(err.error?.message ?? this.i18n.t('common.errors.invalidData'));
          return;
        }
        if (err?.status === 401) {
          this.showAlert(this.i18n.t('common.errors.loginRequired'));
          return;
        }
        this.showAlert(this.i18n.t('common.errors.generic'));
      }
    });
  }

  private filterOutStateful(data: RestMockResponse[]): RestMockResponse[] {
    return data.filter((rec) => !(rec.mockType === 'STATEFUL' && !rec.statefulParent));
  }

  private batchByBasePath(data: RestMockResponse[]): RestMockGroup[] {
    const batched: RestMockGroup[] = [];
    data.forEach((rec) => {
      const path = rec.path;
      const basePathIndex = path.indexOf('/', 1);
      const basePath =
        basePathIndex > -1 && basePathIndex + 1 < path.length ? path.substring(0, basePathIndex) : path;
      this.batchData(batched, rec, basePath);
    });
    return batched;
  }

  private batchData(batched: RestMockGroup[], rec: RestMockResponse, basePath: string): void {
    let current = batched.find((group) => group.basePath === basePath);
    if (!current) {
      current = {
        basePath,
        isOpen: false,
        data: []
      };
      batched.push(current);
    }
    current.data.push(rec);
  }

  private countActiveDefinitions(definitions: RestMockDefinition[]): number {
    return definitions.filter((def) => !def.suspend).length;
  }

  private isBlank(value: string | null | undefined): boolean {
    return !value || value.trim().length === 0;
  }

  private isNumeric(value: unknown): boolean {
    return value !== null && value !== undefined && !Number.isNaN(Number(value));
  }

  private showAlert(message: string, type: Alert['type'] = 'danger'): void {
    this.alerts = [{ message, type }];
  }
}
