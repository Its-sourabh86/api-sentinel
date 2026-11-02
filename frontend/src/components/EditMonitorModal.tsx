import { useState } from 'react';
import type { FormEvent } from 'react';
import { ApiError } from '../api/client';
import { updateMonitor } from '../api/monitors';
import type { HttpMethod, Monitor } from '../api/types';

const METHODS: HttpMethod[] = ['GET', 'POST', 'PUT', 'PATCH', 'DELETE', 'HEAD'];
const INPUT = 'w-full rounded border border-slate-300 p-2 text-sm disabled:bg-slate-100';

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
          e.violations.forEach((v) => { mapped[v.field] = v.message; });
          setErrors(mapped);
        } else {
          setErrors({ url: e.message });
        }
      } else if (e instanceof ApiError && e.status === 429) {
        setFormError(`Too many requests. Try again in ${e.retryAfterSeconds ?? 'a few'} seconds.`);
      } else {
        setFormError('Could not update monitor. Please try again.');
      }
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 p-4">
      <div className="w-full max-w-lg rounded-lg bg-white p-6 shadow-xl">
        <div className="flex items-center justify-between pb-3 border-b">
          <h2 className="text-lg font-bold text-slate-900">Edit Monitor</h2>
          <button
            onClick={onClose}
            className="text-slate-400 hover:text-slate-600 text-lg font-bold"
          >
            &times;
          </button>
        </div>

        <form onSubmit={handleSubmit} className="mt-4 space-y-4" noValidate>
          <div>
            <label className="block text-sm font-medium text-slate-800">
              Name
              <input
                className={`${INPUT} mt-1`}
                value={name}
                maxLength={100}
                onChange={(e) => setName(e.target.value)}
              />
            </label>
            {errors.name && <p className="mt-1 text-xs text-red-700">{errors.name}</p>}
          </div>

          <div>
            <label className="block text-sm font-medium text-slate-800">
              URL
              <input
                className={`${INPUT} mt-1`}
                value={url}
                onChange={(e) => setUrl(e.target.value)}
              />
            </label>
            {errors.url && <p className="mt-1 text-xs text-red-700">{errors.url}</p>}
          </div>

          <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
            <div>
              <label className="block text-xs font-medium text-slate-700">
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
              <label className="block text-xs font-medium text-slate-700">
                Interval (s)
                <input
                  className={`${INPUT} mt-1`}
                  value={interval}
                  inputMode="numeric"
                  onChange={(e) => setIntervalValue(e.target.value)}
                />
              </label>
              {errors.intervalSeconds && (
                <p className="mt-1 text-xs text-red-700">{errors.intervalSeconds}</p>
              )}
            </div>

            <div>
              <label className="block text-xs font-medium text-slate-700">
                Timeout (ms)
                <input
                  className={`${INPUT} mt-1`}
                  value={timeout}
                  inputMode="numeric"
                  onChange={(e) => setTimeoutValue(e.target.value)}
                />
              </label>
              {errors.timeoutMs && (
                <p className="mt-1 text-xs text-red-700">{errors.timeoutMs}</p>
              )}
            </div>

            <div>
              <label className="block text-xs font-medium text-slate-700">
                Expected HTTP
                <input
                  className={`${INPUT} mt-1`}
                  value={expectedStatus}
                  inputMode="numeric"
                  onChange={(e) => setExpectedStatus(e.target.value)}
                />
              </label>
              {errors.expectedStatus && (
                <p className="mt-1 text-xs text-red-700">{errors.expectedStatus}</p>
              )}
            </div>
          </div>

          {formError && <p className="text-sm text-red-700">{formError}</p>}

          <div className="flex justify-end gap-3 pt-3 border-t">
            <button
              type="button"
              onClick={onClose}
              disabled={submitting}
              className="rounded border border-slate-300 px-4 py-2 text-sm text-slate-700 hover:bg-slate-50"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={submitting}
              className="rounded bg-slate-900 px-4 py-2 text-sm text-white hover:bg-slate-800 disabled:opacity-50"
            >
              {submitting ? 'Saving...' : 'Save changes'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

