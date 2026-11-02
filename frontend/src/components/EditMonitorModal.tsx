import { useState } from 'react';
import type { FormEvent } from 'react';
import { ApiError } from '../api/client';
import { updateMonitor } from '../api/monitors';
import type { HttpMethod, Monitor } from '../api/types';

const METHODS: HttpMethod[] = ['GET', 'POST', 'PUT', 'PATCH', 'DELETE', 'HEAD'];
const INPUT = 'w-full rounded-lg bg-[#0E1422] border border-slate-700 p-2.5 text-xs font-mono text-white placeholder-slate-500 focus:outline-none focus:border-indigo-500 disabled:opacity-50';

interface Props {
  monitor: Monitor;
  onClose: () => void;
  onSuccess: (updated: Monitor) => void;
}

export default function EditMonitorModal({ monitor, onClose, onSuccess }: Props) {
  const [name, setName] = useState(monitor.name);
  const [url, setUrl] = useState(monitor.url);
  const [method, setMethod] = useState<HttpMethod>(monitor.method);
  const [interval, setIntervalValue] = useState(String(monitor.intervalSeconds));
  const [timeout, setTimeoutValue] = useState(String(monitor.timeoutMs));
  const [expectedStatus, setExpectedStatus] = useState(String(monitor.expectedStatus));

  const [errors, setErrors] = useState<Record<string, string>>({});
  const [formError, setFormError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  function validate(): Record<string, string> {
    const e: Record<string, string> = {};
    if (name.trim().length < 1 || name.trim().length > 100) {
      e.name = 'Name must be 1 to 100 characters.';
    }
    const u = url.trim();
    if (!/^https?:\/\//i.test(u) || u.length > 2048) {
      e.url = 'Enter a full URL starting with http:// or https:// (max 2048 characters).';
    }
    const iv = Number(interval);
    if (!Number.isFinite(iv) || iv < 10) {
      e.intervalSeconds = 'Interval must be at least 10 seconds.';
    }
    const to = Number(timeout);
    if (!Number.isFinite(to) || to < 1 || to > 30000) {
      e.timeoutMs = 'Timeout must be between 1 and 30000 ms.';
    }
    const st = Number(expectedStatus);
    if (!Number.isFinite(st) || st < 100 || st > 599) {
      e.expectedStatus = 'Expected status must be between 100 and 599.';
    }
    return e;
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    if (submitting) return;

    setFormError('');
    const validationErrors = validate();
    setErrors(validationErrors);
    if (Object.keys(validationErrors).length > 0) return;

    setSubmitting(true);
    try {
      const updated = await updateMonitor(monitor.id, {
        name: name.trim(),
        url: url.trim(),
        method,
        intervalSeconds: Number(interval),
        timeoutMs: Number(timeout),
        expectedStatus: Number(expectedStatus),
      });
      onSuccess(updated);
    } catch (e) {
      if (e instanceof ApiError && e.status === 400) {
        if (e.violations.length > 0) {
          const mapped: Record<string, string> = {};
          e.violations.forEach((v) => {
            mapped[v.field] = v.message;
          });
          setErrors(mapped);
        } else {
          setFormError(e.message);
        }
      } else if (e instanceof ApiError && e.status === 429) {
        setFormError(`Rate limited. Try again in ${e.retryAfterSeconds ?? 'a few'} seconds.`);
      } else {
        setFormError('Failed to update monitor configuration.');
      }
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/80 p-4 backdrop-blur-sm animate-fade-in font-mono">
      <div className="w-full max-w-lg rounded-xl bg-[#111827] border border-slate-700/80 p-6 shadow-2xl">
        <div className="flex items-center justify-between border-b border-slate-800 pb-3">
          <h2 className="text-base font-bold text-white uppercase tracking-wider flex items-center gap-2">
            <span className="text-indigo-400">⚙</span> Edit Target Configuration
          </h2>
          <button
            onClick={onClose}
            className="text-slate-400 hover:text-white transition-colors"
          >
            ✕
          </button>
        </div>

        <form onSubmit={handleSubmit} className="mt-4 space-y-4" noValidate>
          <div>
            <label className="block text-xs font-semibold text-slate-400 uppercase">
              Target Name
              <input
                className={`${INPUT} mt-1`}
                value={name}
                maxLength={100}
                onChange={(e) => setName(e.target.value)}
              />
            </label>
            {errors.name && <p className="mt-1 text-xs text-rose-400">{errors.name}</p>}
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-400 uppercase">
              Target URL
              <input
                className={`${INPUT} mt-1`}
                value={url}
                onChange={(e) => setUrl(e.target.value)}
              />
            </label>
            {errors.url && <p className="mt-1 text-xs text-rose-400">{errors.url}</p>}
          </div>

          <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
            <div>
              <label className="block text-xs font-semibold text-slate-400 uppercase">
                Method
                <select
                  className={`${INPUT} mt-1`}
                  value={method}
                  onChange={(e) => setMethod(e.target.value as HttpMethod)}
                >
                  {METHODS.map((m) => (
                    <option key={m} value={m}>{m}</option>
                  ))}
                </select>
              </label>
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-400 uppercase">
                Interval (s)
                <input
                  className={`${INPUT} mt-1`}
                  value={interval}
                  inputMode="numeric"
                  onChange={(e) => setIntervalValue(e.target.value)}
                />
              </label>
              {errors.intervalSeconds && (
                <p className="mt-1 text-xs text-rose-400">{errors.intervalSeconds}</p>
              )}
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-400 uppercase">
                Timeout (ms)
                <input
                  className={`${INPUT} mt-1`}
                  value={timeout}
                  inputMode="numeric"
                  onChange={(e) => setTimeoutValue(e.target.value)}
                />
              </label>
              {errors.timeoutMs && (
                <p className="mt-1 text-xs text-rose-400">{errors.timeoutMs}</p>
              )}
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-400 uppercase">
                HTTP Status
                <input
                  className={`${INPUT} mt-1`}
                  value={expectedStatus}
                  inputMode="numeric"
                  onChange={(e) => setExpectedStatus(e.target.value)}
                />
              </label>
              {errors.expectedStatus && (
                <p className="mt-1 text-xs text-rose-400">{errors.expectedStatus}</p>
              )}
            </div>
          </div>

          {formError && <p className="text-xs text-rose-400">{formError}</p>}

          <div className="flex justify-end gap-3 pt-3 border-t border-slate-800">
            <button
              type="button"
              onClick={onClose}
              disabled={submitting}
              className="rounded-lg bg-slate-800 px-4 py-2 text-xs font-bold text-slate-300 hover:bg-slate-700 transition-colors"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={submitting}
              className="rounded-lg bg-indigo-600 px-4 py-2 text-xs font-bold text-white hover:bg-indigo-500 shadow-lg shadow-indigo-600/30 transition-all hover:scale-105 disabled:opacity-50"
            >
              {submitting ? 'Saving...' : 'Apply Changes'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
