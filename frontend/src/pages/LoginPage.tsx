import { useState } from 'react';
import type { FormEvent } from 'react';
import { Link } from 'react-router-dom';
import { ApiError, apiFetch } from '../api/client';
import type { LoginResponse } from '../api/types';
import { setToken } from '../auth/tokenStore';

export default function LoginPage() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setError('');
    setLoading(true);
    try {
      const data = await apiFetch<LoginResponse>('/api/v1/auth/login', {
        method: 'POST',
        body: JSON.stringify({ email, password }),
      });
      setToken(data.accessToken, data.expiresInSeconds);
    } catch (e) {
      if (e instanceof ApiError && e.status === 429) {
        setError(`Too many attempts. Try again in ${e.retryAfterSeconds ?? 'a few'} seconds.`);
      } else if (e instanceof ApiError && e.status === 401) {
        setError('Invalid email or password.');
      } else {
        setError('Could not reach the server. Please try again.');
      }
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-slate-50 p-4">
      <form onSubmit={handleSubmit}
            className="flex w-full max-w-sm flex-col gap-3 rounded-lg bg-white p-6 shadow">
        <h1 className="text-2xl font-bold text-slate-900">API Sentinel</h1>
        <p className="text-sm text-slate-600">Sign in to your account</p>

        <input className="rounded border p-2" type="email" autoComplete="email"
               placeholder="email" required value={email}
               onChange={(e) => setEmail(e.target.value)} />
        <input className="rounded border p-2" type="password" autoComplete="current-password"
               placeholder="password" required value={password}
               onChange={(e) => setPassword(e.target.value)} />

        {error && <p role="alert" className="text-sm text-red-700">{error}</p>}

        <button type="submit" disabled={loading}
                className="rounded bg-slate-900 p-2 text-white disabled:opacity-50">
          {loading ? 'Signing in...' : 'Sign in'}
        </button>

        <p className="text-sm text-slate-600">
          No account?{' '}
          <Link to="/register" className="font-medium text-slate-900 underline">Create one</Link>
        </p>
      </form>
    </div>
  );
}