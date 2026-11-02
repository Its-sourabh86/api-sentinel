import http from 'k6/http';
import { check } from 'k6';
import { getToken } from './auth.js';

export const options = {
  scenarios: {
    read: {
      executor: 'constant-vus',
      vus: 50,
      duration: '1m',
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
  },
};

export function setup() {
  return { token: getToken() };
}

export default function (data) {
  const id = 1 + Math.floor(Math.random() * 200);
  const res = http.get(
    `http://localhost:8081/api/v1/monitors/${id}/results?page=0&size=20`,
    { headers: { Authorization: `Bearer ${data.token}` } });
  check(res, { 'status is 200': (r) => r.status === 200 });
}