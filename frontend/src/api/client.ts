import { clearToken, getToken } from '../auth/tokenStore';

const BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8081';

export interface Violation {
  field: string;
  message: string;
}

export class ApiError extends Error {
  status: number;
  retryAfterSeconds?: number;
  violations: Violation[];

  constructor(
    status: number,
    message: string,
    retryAfterSeconds?: number,
    violations: Violation[] = [],
  ) {
    super(message);
    this.status = status;
    this.retryAfterSeconds = retryAfterSeconds;
    this.violations = violations;
  }
}

export async function apiFetch<T>(path: string, options: RequestInit = {}): Promise<T> {
  const token = getToken();

  const response = await fetch(`${BASE_URL}${path}`, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...options.headers,
    },
  });

  if (!response.ok) {
    // Token bheja tha aur 401 mila: token expire ya kharab hai, logout kar do.
    // Login ki galat password wali 401 mein token hota hi nahi, wo logout nahi karti.
    if (response.status === 401 && token) {
      clearToken();
    }

    let message = response.statusText;
    let violations: Violation[] = [];
    try {
      const body = await response.json();
      if (body?.message) message = body.message;
      if (Array.isArray(body?.violations)) violations = body.violations;
    } catch {
      // body JSON nahi tha, statusText hi theek hai
    }

    const retryAfter = response.headers.get('Retry-After');
    throw new ApiError(
      response.status,
      message,
      retryAfter ? Number(retryAfter) : undefined,
      violations,
    );
  }

  if (response.status === 204) return undefined as T;
  return (await response.json()) as T;
}