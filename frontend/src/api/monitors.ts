import { apiFetch } from './client';
import type {
  Assertion, CheckResult, Monitor, MonitorListItem, NewAssertion, NewMonitor, PageResponse, ProbeRunResponse,
} from './types';

export function listMonitors(): Promise<MonitorListItem[]> {
  return apiFetch<MonitorListItem[]>('/api/v1/monitors');
}

export function getMonitor(id: number): Promise<Monitor> {
  return apiFetch<Monitor>(`/api/v1/monitors/${id}`);
}

export function updateMonitor(id: number, body: Partial<NewMonitor>): Promise<Monitor> {
  return apiFetch<Monitor>(`/api/v1/monitors/${id}`, {
    method: 'PATCH',
    body: JSON.stringify(body),
  });
}

export function deleteMonitor(id: number): Promise<void> {
  return apiFetch<void>(`/api/v1/monitors/${id}`, {
    method: 'DELETE',
  });
}

export function pauseMonitor(id: number): Promise<Monitor> {
  return apiFetch<Monitor>(`/api/v1/monitors/${id}/pause`, { method: 'POST' });
}

export function resumeMonitor(id: number): Promise<Monitor> {
  return apiFetch<Monitor>(`/api/v1/monitors/${id}/resume`, { method: 'POST' });
}

export function runMonitorNow(id: number): Promise<ProbeRunResponse> {
  return apiFetch<ProbeRunResponse>(`/api/v1/monitors/${id}/run`, { method: 'POST' });
}

export function listResults(id: number, size = 50): Promise<PageResponse<CheckResult>> {
  return apiFetch<PageResponse<CheckResult>>(`/api/v1/monitors/${id}/results?page=0&size=${size}`);
}

export function listAssertions(id: number): Promise<Assertion[]> {
  return apiFetch<Assertion[]>(`/api/v1/monitors/${id}/assertions`);
}

export function addAssertion(monitorId: number, body: NewAssertion): Promise<Assertion> {
  return apiFetch<Assertion>(`/api/v1/monitors/${monitorId}/assertions`, {
    method: 'POST',
    body: JSON.stringify(body),
  });
}

export function deleteAssertion(monitorId: number, assertionId: number): Promise<void> {
  return apiFetch<void>(`/api/v1/monitors/${monitorId}/assertions/${assertionId}`, {
    method: 'DELETE',
  });
}

export function createMonitor(body: NewMonitor): Promise<Monitor> {
  return apiFetch<Monitor>('/api/v1/monitors', {
    method: 'POST',
    body: JSON.stringify(body),
  });
}

export function getMonitorSchema(monitorId: number): Promise<any> {
  return apiFetch<any>(`/api/v1/monitors/${monitorId}/schema`);
}

export function setMonitorSchema(monitorId: number, schema: any): Promise<any> {
  return apiFetch<any>(`/api/v1/monitors/${monitorId}/schema`, {
    method: 'PUT',
    body: JSON.stringify(schema),
  });
}

export function deleteMonitorSchema(monitorId: number): Promise<void> {
  return apiFetch<void>(`/api/v1/monitors/${monitorId}/schema`, {
    method: 'DELETE',
  });
}
