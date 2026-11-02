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
  { value: 'EQ', label: 'equals (==)', needsValue: true, numeric: false },
  { value: 'NEQ', label: 'does not equal (!=)', needsValue: true, numeric: false },
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

const INPUT = 'w-full rounded-lg bg-[#0E1422] border border-slate-700 p-2.5 text-xs font-mono text-white placeholder-slate-500 focus:outline-none focus:border-indigo-500 disabled:opacity-50';

function isNumber(text: string): boolean {
  return text.trim() !== '' && Number.isFinite(Number(text));
}

function Field({
  label, hint, error, children,
}: { label: string; hint?: string; error?: string; children: React.ReactNode }) {
  return (
    <div>
      <label className="block text-xs font-semibold uppercase text-slate-400">
        {label}
        <div className="mt-1 font-normal">{children}</div>
      </label>
      {hint && !error && <p className="mt-1 text-[11px] text-slate-500">{hint}</p>}
      {error && <p role="alert" className="mt-1 text-xs text-rose-400">{error}</p>}
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
        break;
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
    <div className="min-h-screen bg-[#0B0F19] text-slate-100 p-6 lg:p-8 space-y-6 font-mono">
      <div>
        <Link to="/" className="inline-flex items-center gap-1.5 text-xs text-indigo-400 hover:text-indigo-300">
          ← Back to Cluster Overview
        </Link>
      </div>

      <div className="max-w-2xl">
        <h1 className="text-2xl font-black text-white uppercase tracking-tight flex items-center gap-2">
          <span className="text-indigo-400">❖</span> Register Target Endpoint
        </h1>
        <p className="text-xs text-slate-500 mt-1">Configure probe settings, interval frequency, and business rules</p>

        <form onSubmit={handleSubmit} className="mt-6 space-y-5 rounded-xl bg-[#111827] border border-slate-800 p-6 shadow-2xl" noValidate>
          {locked && (
            <div role="status" className="rounded-lg border border-amber-500/40 bg-amber-500/10 p-3 text-xs text-amber-300">
              The monitor was registered, but some rules failed validation. Fix the rule below and press Save.
            </div>
          )}

          <Field label="Target Identifier / Name" error={errors.name}>
            <input className={INPUT} value={name} disabled={locked} maxLength={100}
                   onChange={(e) => setName(e.target.value)} placeholder="Production Payment Gateway" />
          </Field>

          <Field label="Endpoint URI" error={errors.url} hint="Full HTTP/HTTPS address (SSRF validated on every execution)">
            <input className={INPUT} value={url} disabled={locked}
                   onChange={(e) => setUrl(e.target.value)} placeholder="https://api.example.com/v1/health" inputMode="url" />
          </Field>

          <div className="grid grid-cols-2 gap-4 sm:grid-cols-4">
            <Field label="Method">
              <select className={INPUT} value={method} disabled={locked}
                      onChange={(e) => setMethod(e.target.value as HttpMethod)}>
                {METHODS.map((m) => <option key={m} value={m}>{m}</option>)}
              </select>
            </Field>
            <Field label="Interval (s)" error={errors.intervalSeconds}>
              <input className={INPUT} value={interval} disabled={locked} inputMode="numeric"
                     onChange={(e) => setIntervalValue(e.target.value)} />
            </Field>
            <Field label="Timeout (ms)" error={errors.timeoutMs}>
              <input className={INPUT} value={timeout} disabled={locked} inputMode="numeric"
                     onChange={(e) => setTimeoutValue(e.target.value)} />
            </Field>
            <Field label="HTTP Status" error={errors.expectedStatus}>
              <input className={INPUT} value={expectedStatus} disabled={locked} inputMode="numeric"
                     onChange={(e) => setExpectedStatus(e.target.value)} />
            </Field>
          </div>

          <div>
            <div className="flex items-center justify-between border-t border-slate-800 pt-4">
              <h2 className="text-xs font-bold uppercase text-slate-300">Contract Assertions (Optional)</h2>
              <button type="button" className="text-xs font-bold text-indigo-400 hover:text-indigo-300" onClick={addRule}
                      disabled={submitting}>
                + Add Rule
              </button>
            </div>
            <p className="mt-1 text-[11px] text-slate-500">
              Detects silent payload mutations: API returns HTTP 200 but payload data violates contracts. Example: <code>$.data.stock &gt;= 10</code>
            </p>

            <div className="mt-3 space-y-3">
              {rules.map((rule) => {
                const op = OPERATORS.find((o) => o.value === rule.operator)!;
                const done = savedRuleKeys.includes(rule.key);
                return (
                  <div key={rule.key} className="rounded-lg bg-[#0E1422] border border-slate-700/80 p-3 space-y-2">
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
                    <div className="flex items-center justify-between">
                      <p className="text-xs">
                        {done && <span className="text-emerald-400">✓ Saved</span>}
                        {ruleErrors[rule.key] && (
                          <span role="alert" className="text-rose-400">{ruleErrors[rule.key]}</span>
                        )}
                      </p>
                      {!done && (
                        <button type="button" className="text-[11px] text-slate-500 hover:text-rose-400"
                                onClick={() => removeRule(rule.key)} disabled={submitting}>
                          Remove ✕
                        </button>
                      )}
                    </div>
                  </div>
                );
              })}
            </div>
          </div>

          {formError && <p role="alert" className="text-xs text-rose-400">{formError}</p>}

          <div className="flex gap-3 pt-3 border-t border-slate-800">
            <button type="submit" disabled={submitting}
                    className="rounded-lg bg-indigo-600 hover:bg-indigo-500 px-5 py-2 text-xs font-bold uppercase tracking-wider text-white shadow-lg shadow-indigo-600/30 transition-all hover:scale-105 disabled:opacity-50">
              {submitting ? 'Registering...' : locked ? 'Save Remaining Rules' : 'Create Target'}
            </button>
            <Link to="/" className="rounded-lg bg-slate-800 px-4 py-2 text-xs font-bold text-slate-300 hover:bg-slate-700">
              Cancel
            </Link>
          </div>
        </form>
      </div>
    </div>
  );
}
