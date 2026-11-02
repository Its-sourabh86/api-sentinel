import http from 'k6/http';

const BASE = __ENV.BASE_URL || 'http://localhost:8081';
const EMAIL = 'loadtest@example.com';
// Sirf local load test ka throwaway account. Isse kahin aur use mat karna.
const PASSWORD = __ENV.LOADTEST_PASSWORD || 'loadtest-password-123';
const HEADERS = { 'Content-Type': 'application/json' };

export function getToken() {
  const body = JSON.stringify({ email: EMAIL, password: PASSWORD });
  // Pehli baar 201, dobara chalane par 409: dono theek hain
  http.post(`${BASE}/api/v1/auth/register`, body, { headers: HEADERS });

  const res = http.post(`${BASE}/api/v1/auth/login`, body, { headers: HEADERS });
  if (res.status !== 200) {
    throw new Error(`login failed: ${res.status}`);
  }
  return res.json('accessToken');
}