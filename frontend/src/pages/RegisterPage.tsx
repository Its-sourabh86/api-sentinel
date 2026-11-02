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
      const data = await apiFetch<LoginResponse>('/api/v1/auth/login', {
        method: 'POST',
        body: JSON.stringify({ email, password }),
      });
      setToken(data.accessToken, data.expiresInSeconds);
    } catch (e) {
      if (e instanceof ApiError && e.status === 429) {
        setError(`Rate limited. Try again in ${e.retryAfterSeconds ?? 'a few'} seconds.`);
      } else if (e instanceof ApiError && e.status === 409) {
        setError('This email is already registered. Try signing in.');
      } else if (e instanceof ApiError && e.status === 400) {
        setError('Please check email format and password length.');
      } else {
        setError('Could not reach Sentinel server. Please verify backend service.');
      }
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-[#0B0F19] p-4 font-mono text-slate-100">
      <form onSubmit={handleSubmit}
            className="flex w-full max-w-sm flex-col gap-4 rounded-xl bg-[#111827] border border-slate-800 p-8 shadow-2xl">
        <div>
          <div className="flex items-center gap-2 mb-1">
            <span className="h-2 w-2 rounded-full bg-indigo-400 animate-ping" />
            <span className="text-[10px] font-bold tracking-widest text-indigo-400 uppercase">FLEET ONBOARDING</span>
          </div>
          <h1 className="text-2xl font-black tracking-tight text-white">API SENTINEL</h1>
          <p className="text-xs text-slate-500 mt-0.5">Register new operator account</p>
        </div>

        <div className="space-y-3">
          <div>
            <label className="text-[10px] uppercase font-bold text-slate-400">Operator Email</label>
            <input className="mt-1 w-full rounded-lg bg-[#0E1422] border border-slate-700 p-2.5 text-xs text-white placeholder-slate-500 focus:outline-none focus:border-indigo-500"
                   type="email" autoComplete="email" placeholder="operator@apisentinel.dev" required value={email}
                   onChange={(e) => setEmail(e.target.value)} />
          </div>

          <div>
            <label className="text-[10px] uppercase font-bold text-slate-400">Master Password (8-72 chars)</label>
            <input className="mt-1 w-full rounded-lg bg-[#0E1422] border border-slate-700 p-2.5 text-xs text-white placeholder-slate-500 focus:outline-none focus:border-indigo-500"
                   type="password" autoComplete="new-password" placeholder="••••••••" required value={password}
                   onChange={(e) => setPassword(e.target.value)} />
          </div>
        </div>

        {error && <p role="alert" className="text-xs text-rose-400 bg-rose-950/30 border border-rose-900/40 p-2.5 rounded-lg">{error}</p>}

        <button type="submit" disabled={loading}
                className="rounded-lg bg-indigo-600 hover:bg-indigo-500 p-2.5 text-xs font-bold uppercase tracking-wider text-white shadow-lg shadow-indigo-600/30 transition-all hover:scale-105 disabled:opacity-50 mt-1">
          {loading ? 'Creating...' : 'Register Operator →'}
        </button>

        <p className="text-xs text-slate-500 text-center pt-2 border-t border-slate-800/80">
          Already registered?{' '}
          <Link to="/login" className="font-bold text-indigo-400 hover:text-indigo-300 underline">Sign in</Link>
        </p>
      </form>
    </div>
  );
}
