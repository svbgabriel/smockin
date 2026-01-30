import { Component, OnDestroy, OnInit } from '@angular/core';
import { DatePipe, KeyValuePipe, NgFor, NgIf } from '@angular/common';
import { FormsModule } from '@angular/forms';

import { ApiService } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';
import { AlertsComponent } from '../../shared/alerts/alerts.component';
import { ModalComponent } from '../../shared/modal/modal.component';
import { Alert, ServerStatusResponse, SimpleMessageResponse } from '../../core/models';
import { HTTP_METHODS } from '../../core/constants';
import { formatJson, validateJson, validateXml } from '../../core/formatters';
import { I18nPipe } from '../../core/i18n.pipe';
import { I18nService } from '../../core/i18n.service';

type ActivityType = 'TRAFFIC' | 'S3';

interface LiveLogMessage {
  type: 'TRAFFIC' | 'S3' | 'BLOCKED_RESPONSE';
  payload: LiveLogTraffic | LiveLogS3;
}

interface LiveLogTraffic {
  id: string;
  direction: 'REQUEST' | 'RESPONSE';
  date: string;
  proxied: boolean;
  content: LiveLogInboundContent | LiveLogOutboundContent;
}

interface LiveLogInboundContent {
  method: string;
  url: string;
  headers: Record<string, string>;
  body: string;
  requestParams?: Record<string, string>;
}

interface LiveLogOutboundContent {
  url: string;
  headers: Record<string, string>;
  body: string;
  status: number;
}

interface LiveLogS3 {
  id: string | null;
  date: string;
  information: string;
}

interface ActivityFeedItem {
  id: string;
  type: ActivityType;
  proxied?: boolean;
  request?: LiveLogRequest;
  response?: LiveLogResponse | null;
  amendedResponse?: AmendedResponse | null;
  s3Action?: LiveLogS3;
}

interface LiveLogRequest {
  method: string;
  url: string;
  headers: Record<string, string>;
  body: string;
  requestParams?: Record<string, string>;
  date: string;
}

interface LiveLogResponse {
  status: number;
  headers: Record<string, string>;
  body: string;
  date: string;
  isMockedResponse?: boolean;
  origin?: string;
}

interface HeaderRow {
  key: string | null;
  value: string | null;
}

interface AmendedResponse {
  traceId: string;
  status: number | null;
  headers: HeaderRow[];
  body: string | null;
}

interface BlockedEndpoint {
  id: string | null;
  method: string | null;
  path: string | null;
}

@Component({
  selector: 'app-live-feed',
  standalone: true,
  imports: [AlertsComponent, DatePipe, FormsModule, I18nPipe, KeyValuePipe, ModalComponent, NgFor, NgIf],
  templateUrl: './live-feed.component.html',
  styleUrls: ['./live-feed.component.css']
})
export class LiveFeedComponent implements OnInit, OnDestroy {
  alerts: Alert[] = [];
  blockedAlerts: Alert[] = [];
  activityFeed: ActivityFeedItem[] = [];
  selectedFeed: ActivityFeedItem | null = null;
  searchTerm = '';
  wsEstablished = false;
  responseInterceptorEnabled = false;
  endpointsToBlock: BlockedEndpoint[] = [];
  showBlockedModal = false;
  paths: string[] = [];
  userModeActive = false;

  readonly httpMethods = [...HTTP_METHODS, 'HEAD'];

  private wsSocket: WebSocket | null = null;
  private heartbeatId: number | null = null;

  readonly traceIdHeader = 'X-Smockin-Trace-ID';
  private readonly liveLoggingAmendment = 'LIVE_LOGGING_AMENDMENT';
  private readonly enableLiveLogBlocking = 'ENABLE_LIVE_LOG_BLOCKING';
  private readonly disableLiveLogBlocking = 'DISABLE_LIVE_LOG_BLOCKING';
  private readonly proxiedDownstreamHeader = 'X-Proxied-Downstream-Url';

  constructor(
    private readonly api: ApiService,
    private readonly auth: AuthService,
    private readonly i18n: I18nService
  ) {}

  ngOnInit(): void {
    this.checkUserMode();
  }

  ngOnDestroy(): void {
    this.terminate();
  }

  get blockedCount(): number {
    return this.endpointsToBlock.filter((item) => item.id).length;
  }

  get filteredFeed(): ActivityFeedItem[] {
    const term = this.searchTerm.trim().toLowerCase();
    if (!term) {
      return this.activityFeed;
    }

    return this.activityFeed.filter((item) => {
      if (item.type === 'S3') {
        const info = item.s3Action?.information?.toLowerCase() ?? '';
        return info.includes(term);
      }
      const request = item.request;
      const response = item.response;
      return (
        (request?.url?.toLowerCase() ?? '').includes(term) ||
        (request?.method?.toLowerCase() ?? '').includes(term) ||
        String(response?.status ?? '').includes(term)
      );
    });
  }

  connect(): void {
    this.alerts = [];

    if (this.isConnected()) {
      return;
    }

    if (this.userModeActive && !this.auth.getToken()) {
      this.showAlert(this.i18n.t('liveFeed.errors.loginRequired'));
      return;
    }

    this.api.get<ServerStatusResponse>('/mockedserver/rest/status').subscribe({
      next: (data) => {
        if (!data?.running) {
          this.showAlert(this.i18n.t('liveFeed.errors.serverNotRunning'));
          return;
        }
        this.openSocket();
      },
      error: () => this.showAlert(this.i18n.t('common.errors.generic'))
    });
  }

  clearFeed(): void {
    this.activityFeed = [];
    this.selectedFeed = null;
  }

  selectFeed(item: ActivityFeedItem): void {
    this.selectedFeed = item;
  }

  toggleResponseInterceptor(): void {
    this.responseInterceptorEnabled = !this.responseInterceptorEnabled;
    const payload = {
      type: this.responseInterceptorEnabled ? this.enableLiveLogBlocking : this.disableLiveLogBlocking
    };
    this.sendWsMessage(payload);

    if (!this.responseInterceptorEnabled) {
      this.endpointsToBlock = [];
    }
  }

  openBlockedEndpoints(): void {
    this.showBlockedModal = true;
    this.blockedAlerts = [];
    this.loadPaths();
    if (this.endpointsToBlock.length === 0) {
      this.addEndpointRow();
    }
  }

  closeBlockedEndpoints(): void {
    this.showBlockedModal = false;
  }

  addEndpointRow(): void {
    this.endpointsToBlock.push({ id: null, method: null, path: null });
  }

  addEndpoint(endpoint: BlockedEndpoint): void {
    this.blockedAlerts = [];

    if (this.isBlank(endpoint.method)) {
      this.showBlockedAlert(this.i18n.t('liveFeed.errors.methodRequired'));
      return;
    }
    if (this.isBlank(endpoint.path)) {
      this.showBlockedAlert(this.i18n.t('liveFeed.errors.pathRequired'));
      return;
    }
    if (endpoint.path && !endpoint.path.startsWith('/')) {
      this.showBlockedAlert(this.i18n.t('liveFeed.errors.pathPrefix'));
      return;
    }

    const req = { method: endpoint.method, path: endpoint.path };
    this.api.post<void>('/mockedserver/config/RESTFUL/live-logging-block/endpoint', req).subscribe({
      next: () => {
        endpoint.id = this.generateId();
        if (!this.endpointsToBlock.find((item) => item.id === null)) {
          this.addEndpointRow();
        }
      },
      error: (err) => {
        if (err?.status === 400) {
          this.showBlockedAlert(err.error?.message ?? this.i18n.t('common.errors.invalidData'));
          return;
        }
        this.showBlockedAlert(this.i18n.t('common.errors.generic'));
      }
    });
  }

  removeEndpoint(endpoint: BlockedEndpoint): void {
    if (!endpoint.method || !endpoint.path) {
      return;
    }

    const params = `?method=${encodeURIComponent(endpoint.method)}&path=${encodeURIComponent(endpoint.path)}`;
    this.api.delete<void>(`/mockedserver/config/RESTFUL/live-logging-block/endpoint${params}`).subscribe({
      next: () => {
        this.endpointsToBlock = this.endpointsToBlock.filter((item) => item !== endpoint);
        if (this.endpointsToBlock.length === 0) {
          this.addEndpointRow();
        }
      },
      error: () => this.showBlockedAlert(this.i18n.t('common.errors.generic'))
    });
  }

  releaseBlockedResponse(): void {
    this.alerts = [];

    if (!this.selectedFeed?.amendedResponse) {
      return;
    }

    const amended = this.selectedFeed.amendedResponse;
    const status = Number(amended.status);

    if (Number.isNaN(status)) {
      this.showAlert(this.i18n.t('liveFeed.errors.responseStatusNumeric'));
      return;
    }

    if (!this.areHeadersPopulated(amended.headers)) {
      this.showAlert(this.i18n.t('liveFeed.errors.responseHeadersMissing'));
      return;
    }

    if (this.hasDuplicateHeaders(amended.headers)) {
      this.showAlert(this.i18n.t('liveFeed.errors.responseHeadersDuplicate'));
      return;
    }

    const payload = {
      type: this.liveLoggingAmendment,
      payload: {
        traceId: amended.traceId,
        status,
        headers: this.headersListToMap(amended.headers),
        body: amended.body
      }
    };

    this.sendWsMessage(payload);
    this.selectedFeed.amendedResponse = null;
  }

  addAmendedHeaderRow(): void {
    if (!this.selectedFeed?.amendedResponse) {
      return;
    }
    if (!this.areHeadersPopulated(this.selectedFeed.amendedResponse.headers)) {
      this.showAlert(this.i18n.t('liveFeed.errors.headersPopulateFirst'));
      return;
    }
    this.selectedFeed.amendedResponse.headers.push({ key: null, value: null });
  }

  removeAmendedHeader(key: string | null): void {
    if (!this.selectedFeed?.amendedResponse || !key) {
      return;
    }
    this.selectedFeed.amendedResponse.headers = this.selectedFeed.amendedResponse.headers.filter(
      (header) => header.key !== key
    );
  }

  formatAmendedJson(): void {
    if (!this.selectedFeed?.amendedResponse?.body) {
      return;
    }
    const validation = validateJson(this.selectedFeed.amendedResponse.body);
    if (validation) {
      this.showAlert(validation);
      return;
    }
    this.selectedFeed.amendedResponse.body = formatJson(this.selectedFeed.amendedResponse.body).value;
  }

  formatAmendedXml(): void {
    if (!this.selectedFeed?.amendedResponse?.body) {
      return;
    }
    const validation = validateXml(this.selectedFeed.amendedResponse.body);
    if (validation) {
      this.showAlert(this.i18n.t('liveFeed.errors.formatXml', { error: validation }));
      return;
    }
    this.selectedFeed.amendedResponse.body = this.prettyPrintXml(this.selectedFeed.amendedResponse.body);
  }

  extractContentType(headers: HeaderRow[]): string | null {
    const match = headers.find((header) => header.key === 'Content-Type');
    return match?.value ?? null;
  }

  isConnected(): boolean {
    return this.wsSocket !== null;
  }

  private openSocket(): void {
    const protocol = window.location.protocol === 'https:' ? 'wss' : 'ws';
    const host = window.location.host;
    const token = this.auth.getToken() ?? 'null';
    const wsUrl = `${protocol}://${host}/liveLoggingFeed/false/${token}`;

    try {
      this.wsSocket = new WebSocket(wsUrl);
      this.applyWsListeners();
    } catch (err) {
      this.showAlert(this.i18n.t('liveFeed.errors.unableToConnect', { url: wsUrl }));
      this.wsSocket = null;
    }
  }

  private applyWsListeners(): void {
    if (!this.wsSocket) {
      return;
    }

    this.wsSocket.onopen = () => {
      this.clearFeed();
      this.wsEstablished = true;
      this.startHeartbeat();
    };

    this.wsSocket.onmessage = (event) => {
      this.handleMessage(event.data);
    };

    this.wsSocket.onerror = () => {
      this.showAlert(this.i18n.t('liveFeed.errors.unableToConnectFeed'));
      this.wsEstablished = false;
      this.stopHeartbeat();
      this.wsSocket = null;
    };

    this.wsSocket.onclose = () => {
      this.wsEstablished = false;
      this.stopHeartbeat();
      this.wsSocket = null;
    };
  }

  private handleMessage(payload: string): void {
    let message: LiveLogMessage | null = null;
    try {
      message = JSON.parse(payload);
    } catch (err) {
      return;
    }

    if (!message) {
      return;
    }

    if (message.type === 'BLOCKED_RESPONSE') {
      this.applyBlockedResponse(message.payload as LiveLogTraffic);
      return;
    }

    if (message.type === 'TRAFFIC') {
      this.applyTraffic(message.payload as LiveLogTraffic);
      return;
    }

    if (message.type === 'S3') {
      this.applyS3(message.payload as LiveLogS3);
    }
  }

  private applyTraffic(payload: LiveLogTraffic): void {
    if (payload.direction === 'REQUEST') {
      const content = payload.content as LiveLogInboundContent;
      const request: LiveLogRequest = {
        method: content.method,
        url: content.url,
        headers: content.headers || {},
        body: content.body,
        requestParams: content.requestParams,
        date: payload.date
      };

      this.activityFeed.push({
        id: payload.id,
        type: 'TRAFFIC',
        request,
        response: null,
        proxied: payload.proxied
      });
      return;
    }

    if (payload.direction === 'RESPONSE') {
      const content = payload.content as LiveLogOutboundContent;
      const response: LiveLogResponse = {
        status: content.status,
        headers: content.headers || {},
        body: content.body,
        date: payload.date
      };

      if (payload.proxied) {
        this.applyProxiedResponseOrigin(response);
      }

      const match = this.activityFeed.find((item) => item.id === payload.id);
      if (match) {
        match.response = response;
        match.amendedResponse = null;
      }
    }
  }

  private applyBlockedResponse(payload: LiveLogTraffic): void {
    const content = payload.content as LiveLogOutboundContent;
    const match = this.activityFeed.find((item) => item.id === payload.id);
    const headers = this.headersMapToList(content.headers || {});

    if (match) {
      match.response = {
        status: content.status,
        headers: content.headers || {},
        body: content.body,
        date: payload.date
      };
      match.amendedResponse = {
        traceId: payload.id,
        status: content.status,
        headers,
        body: content.body
      };
    }
  }

  private applyS3(payload: LiveLogS3): void {
    this.activityFeed.push({
      id: payload.id || this.generateId(),
      type: 'S3',
      s3Action: payload
    });
  }

  private applyProxiedResponseOrigin(response: LiveLogResponse): void {
    response.isMockedResponse = true;
    response.origin = 'Mock Server';

    for (const headerName of Object.keys(response.headers)) {
      if (headerName === this.proxiedDownstreamHeader) {
        response.isMockedResponse = false;
        response.origin = response.headers[headerName];
        break;
      }
    }
  }

  private sendWsMessage(payload: unknown): void {
    if (this.wsSocket && this.wsSocket.readyState === WebSocket.OPEN) {
      this.wsSocket.send(JSON.stringify(payload));
    }
  }

  private startHeartbeat(): void {
    this.stopHeartbeat();
    this.heartbeatId = window.setInterval(() => {
      if (this.wsSocket && this.wsSocket.readyState === WebSocket.OPEN) {
        this.wsSocket.send('');
      } else {
        this.stopHeartbeat();
      }
    }, 30000);
  }

  private stopHeartbeat(): void {
    if (this.heartbeatId) {
      window.clearInterval(this.heartbeatId);
      this.heartbeatId = null;
    }
  }

  private terminate(): void {
    if (this.wsSocket) {
      this.wsSocket.close();
      this.wsSocket = null;
    }
    this.stopHeartbeat();
  }

  private headersListToMap(headersList: HeaderRow[]): Record<string, string> {
    const headers: Record<string, string> = {};
    for (const header of headersList) {
      if (header.key) {
        headers[header.key] = header.value ?? '';
      }
    }
    return headers;
  }

  private headersMapToList(headersMap: Record<string, string>): HeaderRow[] {
    return Object.keys(headersMap).map((key) => ({
      key,
      value: headersMap[key]
    }));
  }

  private areHeadersPopulated(headers: HeaderRow[]): boolean {
    for (const header of headers) {
      if (header.key !== this.traceIdHeader && this.isBlank(header.key)) {
        return false;
      }
    }
    return true;
  }

  private hasDuplicateHeaders(headers: HeaderRow[]): boolean {
    const seen = new Set<string>();
    for (const header of headers) {
      if (!header.key) {
        continue;
      }
      if (seen.has(header.key)) {
        return true;
      }
      seen.add(header.key);
    }
    return false;
  }

  private checkUserMode(): void {
    this.api.get<SimpleMessageResponse>('/user/mode').subscribe({
      next: (data) => {
        this.userModeActive = data?.message === 'ACTIVE';
        this.connect();
      },
      error: () => {
        this.userModeActive = false;
        this.connect();
      }
    });
  }

  private loadPaths(): void {
    this.api.get<Array<{ path: string }>>('/restmock').subscribe({
      next: (data) => {
        this.paths = data?.map((item) => item.path) ?? [];
      },
      error: () => {
        this.paths = [];
      }
    });
  }

  private prettyPrintXml(input: string): string {
    const PADDING = '  ';
    const regex = /(>)(<)(\/*)/g;
    const xml = input.replace(regex, '$1\n$2$3');
    let pad = 0;
    return xml
      .split('\n')
      .map((line) => {
        let indent = 0;
        if (line.match(/.+<\/\w[^>]*>$/)) {
          indent = 0;
        } else if (line.match(/^<\/\w/)) {
          pad = Math.max(pad - 1, 0);
        } else if (line.match(/^<\w[^>]*[^\/]>.*$/)) {
          indent = 1;
        }

        const padded = `${PADDING.repeat(pad)}${line}`;
        pad += indent;
        return padded;
      })
      .join('\n');
  }

  private isBlank(value: string | null | undefined): boolean {
    return value == null || String(value).trim().length === 0;
  }

  private generateId(): string {
    if (typeof crypto !== 'undefined' && 'randomUUID' in crypto) {
      return crypto.randomUUID();
    }
    return `id-${Date.now()}-${Math.random().toString(16).slice(2)}`;
  }

  private showAlert(message: string, type: Alert['type'] = 'danger'): void {
    this.alerts = [{ message, type }];
  }

  private showBlockedAlert(message: string, type: Alert['type'] = 'danger'): void {
    this.blockedAlerts = [{ message, type }];
  }
}
