import { apiFetch } from './client';
import type { Incident, PageResponse } from './types';

export function listIncidents(monitorId: number): Promise<PageResponse<Incident>> {
  return apiFetch<PageResponse<Incident>>(`/api/v1/incidents?monitorId=${monitorId}&size=10`);
}

export function listAllIncidents(
  status?: 'OPEN' | 'RESOLVED',
  page = 0,
  size = 20
): Promise<PageResponse<Incident>> {
  const query = new URLSearchParams({ page: String(page), size: String(size) });
  if (status) query.set('status', status);
  return apiFetch<PageResponse<Incident>>(`/api/v1/incidents?${query.toString()}`);
}
