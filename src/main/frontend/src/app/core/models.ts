export type AlertType = 'success' | 'danger' | 'warning' | 'info';

export interface Alert {
  type: AlertType;
  message: string;
}

export interface SimpleMessageResponse {
  message: string;
}

export interface RestMockResponse {
  extId: string;
  path: string;
  userCtxPath: string;
  method: string;
  status: string;
  mockType: string;
  dateCreated: string;
  statefulParent?: boolean;
}

export interface RestMockDefinition {
  extId: string | null;
  orderNo: number;
  httpStatusCode: number;
  responseContentType: string;
  responseBody: string | null;
  sleepInMillis: number;
  suspend: boolean;
  frequencyCount: number;
  frequencyPercentage: number;
  responseHeaders: Record<string, string>;
}

export interface RuleCondition {
  matchType: RuleMatchingType;
  fieldName: string | null;
  comparator: RuleComparator;
  dataType: string | null;
  caseSensitive: boolean;
  matchValue: string | null;
}

export interface RuleGroup {
  extId: string | null;
  orderNo: number;
  conditions: RuleCondition[];
}

export interface RuleResponse {
  extId: string | null;
  orderNo: number;
  responseContentType: string;
  httpStatusCode: number;
  responseBody: string | null;
  sleepInMillis: number;
  suspend: boolean;
  responseHeaders: Record<string, string>;
  groups: RuleGroup[];
}

export interface RestMockDetail extends RestMockResponse {
  createdBy: string;
  proxyTimeoutInMillis: number;
  webSocketTimeoutInMillis: number;
  sseHeartBeatInMillis: number;
  proxyPushIdOnConnect: boolean;
  randomiseDefinitions: boolean;
  randomiseLatency: boolean;
  randomiseLatencyRangeMinMillis: number;
  randomiseLatencyRangeMaxMillis: number;
  definitions: RestMockDefinition[];
  customJsSyntax: string | null;
  rules: RuleResponse[];
  statefulDefaultResponseBody?: string | null;
  statefulIdFieldName?: string | null;
  statefulIdFieldLocation?: string | null;
}

export interface RestMockGroup {
  basePath: string;
  isOpen: boolean;
  data: RestMockResponse[];
}

export interface ServerStatusResponse {
  running: boolean;
  port: number;
}

export interface TunnelResponse {
  enabled: boolean;
  uri: string;
}

export interface S3BucketResponse {
  extId: string;
  bucket: string;
  status: string;
  syncMode: string;
  dateCreated: string;
}

export interface S3Node {
  extId: string;
  name: string;
  children: S3Node[];
  files: S3File[];
  expanded?: boolean;
}

export interface S3File {
  extId: string;
  name: string;
}

export interface S3BucketDetail extends S3BucketResponse {
  createdBy: string;
  dateCreated: string;
  userCtxPath: string | null;
  children: S3Node[];
  files: S3File[];
}

export interface MailMockResponse {
  externalId: string;
  address: string;
  status: string;
  dateCreated: string;
  messageCount: number;
  saveReceivedMail: boolean;
}

export interface MailMessage {
  extId?: string;
  cacheID?: string;
  from: string;
  subject: string;
  dateReceived: string;
  attachmentsCount: number;
  body?: string;
}

export interface MailMessagePage {
  pageData: MailMessage[];
  totalRecords: number;
  recordsPerPage: number;
}

export interface MailMockDetail {
  externalId: string;
  address: string;
  status: string;
  dateReceived: string;
  saveReceivedMail: boolean;
  messages?: MailMessagePage;
}

export interface MailAttachment {
  extId?: string;
  name: string;
}

export interface AttachmentContent {
  name: string;
  mimeType: string;
  base64Content: string;
}

export interface UserResponse {
  extId: string;
  username: string;
  fullName: string;
  role: string;
  dateCreated: string;
  passwordResetToken?: string | null;
}

export interface KvpResponse {
  extId: string;
  key: string;
  value: string;
}

export interface JwtPayload {
  role?: string;
  name?: string;
  username?: string;
}

export interface RuleComparator {
  dropDownName: string;
  tableName: string;
  value: string;
  dataType: string;
}

export interface RuleMatchingType {
  name: string;
  value: string;
  fieldPlaceholderText: string;
}
