export interface LoginResponse {
  accessToken: string;
  tokenType: string;
  expiresInSeconds: number;
}

export interface UserResponse {
  id: number;
  email: string;
  role: 'USER' | 'ADMIN';
  createdAt: string;
}

export interface Monitor {
  id: number;
  name: string;
  url: string;
  method: 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE' | 'HEAD';
  intervalSeconds: number;
  timeoutMs: number;
  expectedStatus: number;
  enabled: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface LastCheck {
  status: 'PASSED' | 'FAILED';
  failureCode: string | null;
  reason: string | null;
  httpStatus: number | null;
  durationMs: number;
  checkedAt: string;
}

export interface MonitorListItem {
  monitor: Monitor;
  lastCheck: LastCheck | null;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface CheckResult {
  id: number;
  startedAt: string;
  durationMs: number;
  httpStatus: number | null;
  status: 'PASSED' | 'FAILED';
  failureCode: string | null;
  reason: string | null;
  outcome: string;
  trigger: 'MANUAL' | 'SCHEDULED';
}

export interface Assertion {
  id: number;
  type: string;
  path: string;
  operator: string;
  expectedValue: string | null;
  createdAt: string;
}

export interface Incident {
  id: number;
  monitorId: number;
  status: 'OPEN' | 'RESOLVED';
  failureCode: string | null;
  reason: string | null;
  failureCount: number;
  openedAt: string;
  lastFailureAt: string;
  resolvedAt: string | null;
}

export type HttpMethod = 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE' | 'HEAD';

export type AssertionOperator =
  | 'EQ' | 'NEQ' | 'GT' | 'GTE' | 'LT' | 'LTE'
  | 'EXISTS' | 'NOT_EMPTY' | 'AGE_LTE';

export interface NewMonitor {
  name: string;
  url: string;
  method: HttpMethod;
  intervalSeconds: number;
  timeoutMs: number;
  expectedStatus: number;
}

export interface NewAssertion {
  path: string;
  operator: AssertionOperator;
  expectedValue?: string;
}

export interface ProbeRunResponse {
  monitorId: number;
  status: 'PASSED' | 'FAILED';
  failureCode: string | null;
  reason: string | null;
  outcome: string;
  httpStatus: number | null;
  durationMs: number;
}
