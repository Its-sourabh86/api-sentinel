import { useState } from 'react';
import type { FormEvent } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useQueryClient } from '@tanstack/react-query';
import { ApiError } from '../api/client';
import { addAssertion, createMonitor } from '../api/monitors';
import type { AssertionOperator, HttpMethod } from '../api/types';

const METHODS: HttpMethod[] = ['GET', 'POST', 'PUT', 'PATCH', 'DELETE', 'HEAD'];

const OPERATORS: { value: AssertionOperator; label: string; needsValue: boolean; numeric: boolean }[] = [
  { value: 'GTE', label: 'is at least (>=)', needsValue: true, numeric: true },
  { value: 'GT', label: 'is greater than (>)', needsValue: true, numeric: true },
  { value: 'LTE', label: 'is at most (<=)', needsValue: true, numeric: true },
  { value: 'LT', label: 'is less than (<)', needsValue: true, numeric: true },
  { value: 'EQ', label: 'equals', needsValue: true, numeric: false },
  { value: 'NEQ', label: 'does not equal', needsValue: true, numeric: false },
  { value: 'EXISTS', label: 'exists', needsValue: false, numeric: false },
  { value: 'NOT_EMPTY', label: 'is not empty', needsValue: false, numeric: false },
  { value: 'AGE_LTE', label: 'is not older than (seconds)', needsValue: true, numeric: true },
];

interface RuleDraft {
  key: number;
  path: string;
  operator: AssertionOperator;
  expectedValue: string;
}

type Errors = Record<string, string>;

const INPUT = 'w-full rounded border border-slate-300 p-2 text-sm disabled:bg-slate-100';

function isNumber(text: string): boolean {
  return text.trim() !== '' && Number.isFinite(Number(text));
}

function Field({
  label, hint, error, children,
}: { label: string; hint?: string; error?: string; children: React.ReactNode }) {
  return (
    <div>
      <label className="block text-sm font-medium text-slate-800">
        {label}
        <div className="mt-1 font-normal">{children}</div>
      </label>
      {hint && !error && <p className="mt-1 text-xs text-slate-500">{hint}</p>}
      {error && <p role="alert" className="mt-1 text-xs text-red-700">{error}</p>}
    </div>
  );
}

export default function CreateMonitorPage() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const [name, setName] = useState('');
  const [url, setUrl] = useState('');
  const [method, setMethod] = useState<HttpMethod>('GET');
  const [interval, setIntervalValue] = useState('60');
  const [timeout, setTimeoutValue] = useState('3000');
  const [expectedStatus, setExpectedStatus] = useState('200');
  const [rules, setRules] = useState<RuleDraft[]>([]);
  const [nextKey, setNextKey] = useState(1);

  const [errors, setErrors] = useState<Errors>({});
  const [ruleErrors, setRuleErrors] = useState<Record<number, string>>({});
  const [formError, setFormError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  // Partial failure ke liye: monitor ban chuka ho toh uska id aur save ho chuke rules yaad rakho
  const [createdId, setCreatedId] = useState<number | null>(null);
  const [savedRuleKeys, setSavedRuleKeys] = useState<number[]>([]);

  const locked = createdId !== null;

  function validate(): { errors: Errors; ruleErrors: Record<number, string> } {
    const e: Errors = {};
    if (name.trim().length < 1 || name.trim().length > 100) e.name = 'Name must be 1 to 100 characters.';
    const u = url.trim();
    if (!/^https?:\/\//i.test(u) || u.length > 2048) {
      e.url = 'Enter a full URL starting with http:// or https:// (max 2048 characters).';
    }
    if (!isNumber(interval) || Number(interval) < 10) e.intervalSeconds = 'Interval must be at least 10 seconds.';
    if (!isNumber(timeout) || Number(timeout) < 1 || Number(timeout) > 30000) {
      e.timeoutMs = 'Timeout must be between 1 and 30000 ms.';
    }
    if (!isNumber(expectedStatus) || Number(expectedStatus) < 100 || Number(expectedStatus) > 599) {
      e.expectedStatus = 'Expected status must be between 100 and 599.';
    }

    const re: Record<number, string> = {};
    for (const rule of rules) {
      const op = OPERATORS.find((o) => o.value === rule.operator)!;
      if (!rule.path.trim().startsWith('$')) {
        re[rule.key] = "Path must start with '$' (example: $.data.stock).";
      } else if (op.needsValue && rule.expectedValue.trim() === '') {
        re[rule.key] = 'This rule needs a value.';
      } else if (op.numeric && !isNumber(rule.expectedValue)) {
        re[rule.key] = 'This rule needs a number.';
      }
    }
    return { errors: e, ruleErrors: re };
  }

  function updateRule(key: number, patch: Partial<RuleDraft>) {
    setRules((current) => current.map((r) => (r.key === key ? { ...r, ...patch } : r)));
  }

  function addRule() {
    setRules((current) => [...current, { key: nextKey, path: '', operator: 'GTE', expectedValue: '' }]);
    setNextKey((k) => k + 1);
  }

  function removeRule(key: number) {
    setRules((current) => current.filter((r) => r.key !== key));
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    if (submitting) return;

    setFormError('');
    const result = validate();
    setErrors(result.errors);
    setRuleErrors(result.ruleErrors);
    if (Object.keys(result.errors).length > 0 || Object.keys(result.ruleErrors).length > 0) return;

    setSubmitting(true);
    let id = createdId;
    try {
      if (id === null) {
        const monitor = await createMonitor({
          name: name.trim(),
          url: url.trim(),
          method,
          intervalSeconds: Number(interval),
          timeoutMs: Number(timeout),
          expectedStatus: Number(expectedStatus),
        });
        id = monitor.id;
        setCreatedId(id);
      }
    } catch (e) {
      if (e instanceof ApiError && e.status === 400) {
        if (e.violations.length > 0) {
          const mapped: Errors = {};
          e.violations.forEach((v) => { mapped[v.field] = v.message; });
          setErrors(mapped);
        } else {
          // Jaise SSRF guard ka "url points to a disallowed address"
          setErrors({ url: e.message });
        }
      } else if (e instanceof ApiError && e.status === 429) {
        setFormError(`Too many requests. Try again in ${e.retryAfterSeconds ?? 'a few'} seconds.`);
      } else {
        setFormError('Could not create the monitor. Please try again.');
      }
      setSubmitting(false);
      return;
    }

    // Rules ek-ek karke. Jo pehle save ho chuke unhe dobara nahi bhejte.
    const saved = new Set(savedRuleKeys);
    const newRuleErrors: Record<number, string> = {};
    for (const rule of rules) {
      if (saved.has(rule.key)) continue;
      const op = OPERATORS.find((o) => o.value === rule.operator)!;
      try {
        await addAssertion(id, {
          path: rule.path.trim(),
          operator: rule.operator,
          expectedValue: op.needsValue ? rule.expectedValue.trim() : undefined,
        });
        saved.add(rule.key);
        setSavedRuleKeys([...saved]);
      } catch (e) {
        newRuleErrors[rule.key] =
          e instanceof ApiError && e.status === 400 ? e.message : 'Could not save this rule.';
        break; // aage ke rules rok do, user theek karke dobara Save dabaye
      }
    }

    setSubmitting(false);
    if (Object.keys(newRuleErrors).length > 0) {
      setRuleErrors(newRuleErrors);
      return;
    }

    await queryClient.invalidateQueries({ queryKey: ['monitors'] });
    navigate(`/monitors/${id}`);
  }

  return (
    <div className="mx-auto max-w-2xl p-8">
      <div className="mb-4">
        <Link to="/" className="text-sm font-medium text-slate-500 hover:text-slate-800">
          &larr; Back to all monitors
        </Link>
      </div>

      <h1 className="text-2xl font-bold text-slate-900">New monitor</h1>

      <form onSubmit={handleSubmit} className="mt-4 space-y-5 rounded-lg bg-white p-6 shadow-sm" noValidate>
        {locked && (
          <div role="status" className="rounded border border-amber-200 bg-amber-50 p-3 text-sm text-amber-900">
            The monitor was created, but some rules could not be saved. Fix the rule below and press
            Save again, or <Link className="underline" to={`/monitors/${createdId}`}>open the monitor</Link>.
          </div>
        )}

        <Field label="Name" error={errors.name}>
          <input className={INPUT} value={name} disabled={locked} maxLength={100}
                 onChange={(e) => setName(e.target.value)} placeholder="Inventory API" />
        </Field>

        <Field label="URL" error={errors.url} hint="The endpoint to check, for example https://api.example.com/inventory/123">
          <input className={INPUT} value={url} disabled={locked}
                 onChange={(e) => setUrl(e.target.value)} placeholder="https://" inputMode="url" />
        </Field>

        <div className="grid grid-cols-2 gap-4 sm:grid-cols-4">
          <Field label="Method">
            <select className={INPUT} value={method} disabled={locked}
                    onChange={(e) => setMethod(e.target.value as HttpMethod)}>
              {METHODS.map((m) => <option key={m} value={m}>{m}</option>)}
            </select>
          </Field>
          <Field label="Every (sec)" error={errors.intervalSeconds}>
            <input className={INPUT} value={interval} disabled={locked} inputMode="numeric"
                   onChange={(e) => setIntervalValue(e.target.value)} />
          </Field>
          <Field label="Timeout (ms)" error={errors.timeoutMs}>
            <input className={INPUT} value={timeout} disabled={locked} inputMode="numeric"
                   onChange={(e) => setTimeoutValue(e.target.value)} />
          </Field>
          <Field label="Expected HTTP" error={errors.expectedStatus}>
            <input className={INPUT} value={expectedStatus} disabled={locked} inputMode="numeric"
                   onChange={(e) => setExpectedStatus(e.target.value)} />
          </Field>
        </div>

        <div>
          <div className="flex items-center justify-between">
            <h2 className="text-sm font-semibold text-slate-900">Business rules (optional)</h2>
            <button type="button" className="text-sm text-slate-800 underline" onClick={addRule}
                    disabled={submitting}>
              + Add rule
            </button>
          </div>
          <p className="mt-1 text-xs text-slate-500">
            Rules catch silent failures: the API returns HTTP 200 but the data is wrong. Example:
            {' '}<code>$.data.stock</code> is at least <code>10</code>.
          </p>

          <div className="mt-3 space-y-3">
            {rules.map((rule) => {
              const op = OPERATORS.find((o) => o.value === rule.operator)!;
              const done = savedRuleKeys.includes(rule.key);
              return (
                <div key={rule.key} className="rounded border border-slate-200 p-3">
                  <div className="grid gap-2 sm:grid-cols-[1fr_auto_auto]">
                    <input className={INPUT} placeholder="$.data.stock" value={rule.path}
                           disabled={done} aria-label="JSON path"
                           onChange={(e) => updateRule(rule.key, { path: e.target.value })} />
                    <select className={INPUT} value={rule.operator} disabled={done} aria-label="Operator"
                            onChange={(e) => updateRule(rule.key, { operator: e.target.value as AssertionOperator })}>
                      {OPERATORS.map((o) => <option key={o.value} value={o.value}>{o.label}</option>)}
                    </select>
                    {op.needsValue && (
                      <input className={INPUT} placeholder="value" value={rule.expectedValue}
                             disabled={done} aria-label="Value"
                             onChange={(e) => updateRule(rule.key, { expectedValue: e.target.value })} />
                    )}
                  </div>
                  <div className="mt-1 flex items-center justify-between">
                    <p className="text-xs">
                      {done && <span className="text-green-700">Saved</span>}
                      {ruleErrors[rule.key] && (
                        <span role="alert" className="text-red-700">{ruleErrors[rule.key]}</span>
                      )}
                    </p>
                    {!done && (
                      <button type="button" className="text-xs text-slate-600 underline"
                              onClick={() => removeRule(rule.key)} disabled={submitting}>
                        Remove
                      </button>
                    )}
                  </div>
                </div>
              );
            })}
          </div>
        </div>

        {formError && <p role="alert" className="text-sm text-red-700">{formError}</p>}

        <div className="flex gap-3">
          <button type="submit" disabled={submitting}
                  className="rounded bg-slate-900 px-4 py-2 text-sm text-white disabled:opacity-50">
            {submitting ? 'Saving...' : locked ? 'Save remaining rules' : 'Create monitor'}
          </button>
          <Link to="/" className="rounded border border-slate-300 px-4 py-2 text-sm text-slate-800">
            Cancel
          </Link>
        </div>
      </form>
    </div>
  );
}
