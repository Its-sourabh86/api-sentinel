import { useState } from 'react';
import type { FormEvent } from 'react';
import { Link } from 'react-router-dom';
import { ApiError, apiFetch } from '../api/client';
import type { LoginResponse, UserResponse } from '../api/types';
import { setToken } from '../auth/tokenStore';

export default function RegisterPage() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setError('');

    // Backend bhi yahi check karta hai (8 se 72). Yahan sirf user ko jaldi batane ke liye.
    if (password.length < 8 || password.length > 72) {
      setError('Password must be 8 to 72 characters.');
      return;
    }

    setLoading(true);
    try {
      await apiFetch<UserResponse>('/api/v1/auth/register', {
        method: 'POST',
        body: JSON.stringify({ email, password }),
      });
      // Account ban gaya: ab khud login kar dete hain
      const data = await apiFetch<LoginResponse>('/api/v1/auth/login', {
        method: 'POST',
        body: JSON.stringify({ email, password }),
      });
      setToken(data.accessToken, data.expiresInSeconds);
    } catch (e) {
      if (e instanceof ApiError && e.status === 429) {
        setError(`Too many attempts. Try again in ${e.retryAfterSeconds ?? 'a few'} seconds.`);
      } else if (e instanceof ApiError && e.status === 409) {
        setError('This email is already registered. Try signing in.');
      } else if (e instanceof ApiError && e.status === 400) {
        setError('Please check your email and password and try again.');
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
        <p className="text-sm text-slate-600">Create your account</p>

        <input className="rounded border p-2" type="email" autoComplete="email"
               placeholder="email" required value={email}
               onChange={(e) => setEmail(e.target.value)} />
        <input className="rounded border p-2" type="password" autoComplete="new-password"
               placeholder="password (8 to 72 characters)" required value={password}
               onChange={(e) => setPassword(e.target.value)} />

        {error && <p role="alert" className="text-sm text-red-700">{error}</p>}

        <button type="submit" disabled={loading}
                className="rounded bg-slate-900 p-2 text-white disabled:opacity-50">
          {loading ? 'Creating account...' : 'Create account'}
        </button>

        <p className="text-sm text-slate-600">
          Already have an account?{' '}
          <Link to="/login" className="font-medium text-slate-900 underline">Sign in</Link>
        </p>
      </form>
    </div>
  );
}