import http from 'k6/http';
import { check } from 'k6';
import exec from 'k6/execution';
import { getToken } from './auth.js';

export const options = {
  scenarios: {
    create: {
      executor: 'shared-iterations',
      vus: 5,
      iterations: 200,
      maxDuration: '2m',
    },
  },
};

export function setup() {
  return { token: getToken() };
}

export default function (data) {
  const n = exec.scenario.iterationInTest;
  const payload = JSON.stringify({
    name: `load-${n}`,
    url: `http://localhost:8081/mock/inventory/${n}?stock=15`,
    intervalSeconds: 10,
    timeoutMs: 3000,
  });
  const res = http.post('http://localhost:8081/api/v1/monitors', payload, {
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${data.token}`,
    },
  });
  check(res, { 'created (201)': (r) => r.status === 201 });
}