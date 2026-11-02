const LABELS: Record<string, string> = {
  STATUS_MISMATCH: 'Unexpected HTTP status',
  JSON_ASSERTION_FAILED: 'Business rule failed',
  SCHEMA_VIOLATION: 'Schema violation',
  STALE_DATA: 'Stale data',
  INVALID_JSON: 'Invalid JSON',
  TIMEOUT: 'Timed out',
  CONNECTION_FAILED: 'Connection failed',
  BLOCKED_TARGET: 'Blocked target',
  INVALID_URL: 'Invalid URL',
  RESPONSE_TOO_LARGE: 'Response too large',
  PROBE_ERROR: 'Probe error',
};

export function failureLabel(code: string | null): string {
  if (!code) return 'Check failed';
  return LABELS[code] ?? code;
}