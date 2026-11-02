const KEY = 'sentinel.auth';

interface Stored {
  token: string;
  expiresAt: number; // milliseconds
}

const listeners = new Set<() => void>();

function notify() {
  listeners.forEach((listener) => listener());
}

function read(): Stored | null {
  try {
    const raw = sessionStorage.getItem(KEY);
    if (!raw) return null;
    const stored = JSON.parse(raw) as Stored;
    if (typeof stored.token !== 'string' || typeof stored.expiresAt !== 'number'
        || Date.now() >= stored.expiresAt) {
      sessionStorage.removeItem(KEY);
      return null;
    }
    return stored;
  } catch {
    return null; // storage block ho ya JSON kharab ho
  }
}

export function getToken(): string | null {
  return read()?.token ?? null;
}

export function isLoggedIn(): boolean {
  return getToken() !== null;
}

export function setToken(token: string, expiresInSeconds: number) {
  try {
    sessionStorage.setItem(KEY, JSON.stringify({
      token,
      expiresAt: Date.now() + expiresInSeconds * 1000,
    }));
  } catch {
    // storage nahi chala toh token nahi bachega, user ko dobara login karna padega
  }
  notify();
}

export function clearToken() {
  try {
    sessionStorage.removeItem(KEY);
  } catch {
    // ignore
  }
  notify();
}

export function subscribe(listener: () => void) {
  listeners.add(listener);
  return () => listeners.delete(listener);
}