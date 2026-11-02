import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Link, useNavigate, useParams } from 'react-router-dom';
import {
  CartesianGrid,
  Line,
  LineChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts';
import { ApiError } from '../api/client';
import { listIncidents } from '../api/incidents';
import {
  addAssertion,
  deleteAssertion,
  deleteMonitor,
  deleteMonitorSchema,
  getMonitor,
  getMonitorSchema,
  listAssertions,
  listResults,
  pauseMonitor,
  resumeMonitor,
  runMonitorNow,
  setMonitorSchema,
} from '../api/monitors';
import type { AssertionOperator } from '../api/types';
import EditMonitorModal from '../components/EditMonitorModal';
import { failureLabel } from '../lib/labels';
import { percentile } from '../lib/stats';
import { formatDateTime, timeAgo } from '../lib/time';

const OPERATORS: { value: AssertionOperator; label: string; needsValue: boolean }[] = [
  { value: 'GTE', label: 'is at least (>=)', needsValue: true },
  { value: 'GT', label: 'is greater than (>)', needsValue: true },
  { value: 'LTE', label: 'is at most (<=)', needsValue: true },
  { value: 'LT', label: 'is less than (<)', needsValue: true },
  { value: 'EQ', label: 'equals', needsValue: true },
  { value: 'NEQ', label: 'does not equal', needsValue: true },
  { value: 'EXISTS', label: 'exists', needsValue: false },
  { value: 'NOT_EMPTY', label: 'is not empty', needsValue: false },
  { value: 'AGE_LTE', label: 'is not older than (seconds)', needsValue: true },
];

function StatCard({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-lg bg-white p-4 shadow-sm">
      <p className="text-xs font-medium uppercase tracking-wide text-slate-500">{label}</p>
      <p className="mt-1 text-2xl font-bold text-slate-900">{value}</p>
    </div>
  );
}

function Section({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <section className="mt-8">
      <h2 className="mb-3 text-lg font-semibold text-slate-900">{title}</h2>
      {children}
    </section>
  );
}

export default function MonitorDetailPage() {
  const { id } = useParams();
  const monitorId = Number(id);
  const validId = Number.isInteger(monitorId) && monitorId > 0;
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const [isEditing, setIsEditing] = useState(false);
  const [runMessage, setRunMessage] = useState<{ text: string; isError: boolean } | null>(null);

  // Assertion form state
  const [showAddRule, setShowAddRule] = useState(false);
  const [rulePath, setRulePath] = useState('');
  const [ruleOp, setRuleOp] = useState<AssertionOperator>('GTE');
  const [ruleVal, setRuleVal] = useState('');
  const [ruleError, setRuleError] = useState<string | null>(null);

  // Schema editor state
  const [showSchemaEditor, setShowSchemaEditor] = useState(false);
  const [schemaText, setSchemaText] = useState('');
  const [schemaError, setSchemaError] = useState<string | null>(null);

  const monitorQuery = useQuery({
    queryKey: ['monitor', monitorId],
    queryFn: () => getMonitor(monitorId),
    enabled: validId,
  });

  const resultsQuery = useQuery({
    queryKey: ['results', monitorId],
    queryFn: () => listResults(monitorId),
    enabled: validId,
    refetchInterval: 15_000,
  });

  const assertionsQuery = useQuery({
    queryKey: ['assertions', monitorId],
    queryFn: () => listAssertions(monitorId),
    enabled: validId,
  });

  const schemaQuery = useQuery({
    queryKey: ['schema', monitorId],
    queryFn: async () => {
      try {
        const schema = await getMonitorSchema(monitorId);
        setSchemaText(JSON.stringify(schema, null, 2));
        return schema;
      } catch (e) {
        if (e instanceof ApiError && e.status === 404) {
          setSchemaText('');
          return null;
        }
        throw e;
      }
    },
    enabled: validId,
  });

  const incidentsQuery = useQuery({
    queryKey: ['incidents', monitorId],
    queryFn: () => listIncidents(monitorId),
    enabled: validId,
    refetchInterval: 15_000,
  });

  const toggleMutation = useMutation({
    mutationFn: (currentlyEnabled: boolean) =>
      currentlyEnabled ? pauseMonitor(monitorId) : resumeMonitor(monitorId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['monitor', monitorId] });
      queryClient.invalidateQueries({ queryKey: ['monitors'] });
    },
  });

  const deleteMutation = useMutation({
    mutationFn: () => deleteMonitor(monitorId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['monitors'] });
      navigate('/');
    },
  });

  const runMutation = useMutation({
    mutationFn: () => runMonitorNow(monitorId),
    onSuccess: (res) => {
      queryClient.invalidateQueries({ queryKey: ['results', monitorId] });
      queryClient.invalidateQueries({ queryKey: ['incidents', monitorId] });
      queryClient.invalidateQueries({ queryKey: ['monitor', monitorId] });
      queryClient.invalidateQueries({ queryKey: ['monitors'] });
      if (res.status === 'PASSED') {
        setRunMessage({
          text: `Check PASSED in ${res.durationMs}ms (HTTP ${res.httpStatus})`,
          isError: false,
        });
      } else {
        setRunMessage({
          text: `Check FAILED: ${failureLabel(res.failureCode)} ${res.reason ? `(${res.reason})` : ''}`,
          isError: true,
        });
      }
      setTimeout(() => setRunMessage(null), 6000);
    },
    onError: (err: any) => {
      setRunMessage({
        text: `Check failed to run: ${err.message || 'Server error'}`,
        isError: true,
      });
      setTimeout(() => setRunMessage(null), 6000);
    },
  });

  const addRuleMutation = useMutation({
    mutationFn: () => {
      const selectedOp = OPERATORS.find((o) => o.value === ruleOp)!;
      return addAssertion(monitorId, {
        path: rulePath.trim(),
        operator: ruleOp,
        expectedValue: selectedOp.needsValue ? ruleVal.trim() : undefined,
      });
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['assertions', monitorId] });
      setShowAddRule(false);
      setRulePath('');
      setRuleVal('');
      setRuleError(null);
    },
    onError: (err: any) => {
      setRuleError(err.message || 'Failed to save rule.');
    },
  });

  const deleteRuleMutation = useMutation({
    mutationFn: (assertionId: number) => deleteAssertion(monitorId, assertionId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['assertions', monitorId] });
    },
  });

  const saveSchemaMutation = useMutation({
    mutationFn: (parsed: any) => setMonitorSchema(monitorId, parsed),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['schema', monitorId] });
      setShowSchemaEditor(false);
      setSchemaError(null);
    },
    onError: (err: any) => {
      setSchemaError(err.message || 'Failed to update schema.');
    },
  });

  const deleteSchemaMutation = useMutation({
    mutationFn: () => deleteMonitorSchema(monitorId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['schema', monitorId] });
      setSchemaText('');
      setSchemaError(null);
    },
  });

  function handleDelete() {
    if (window.confirm('Are you sure you want to delete this monitor? All check history will be removed.')) {
      deleteMutation.mutate();
    }
  }

  function handleSaveRule(e: React.FormEvent) {
    e.preventDefault();
    setRuleError(null);
    if (!rulePath.trim().startsWith('$')) {
      setRuleError("Path must start with '$' (e.g. $.data.stock)");
      return;
    }
    const op = OPERATORS.find((o) => o.value === ruleOp)!;
    if (op.needsValue && !ruleVal.trim()) {
      setRuleError('Value is required for this operator');
      return;
    }
    addRuleMutation.mutate();
  }

  function handleSaveSchema(e: React.FormEvent) {
    e.preventDefault();
    setSchemaError(null);
    try {
      const parsed = JSON.parse(schemaText);
      saveSchemaMutation.mutate(parsed);
    } catch {
      setSchemaError('Invalid JSON format. Please ensure valid JSON Schema syntax.');
    }
  }

  if (!validId || (monitorQuery.isError && monitorQuery.error instanceof ApiError
      && monitorQuery.error.status === 404)) {
    return (
      <div className="mx-auto max-w-5xl p-8">
        <p className="rounded-lg bg-white p-6 text-slate-700 shadow-sm">Monitor not found.</p>
      </div>
    );
  }

  const monitor = monitorQuery.data;
  const results = resultsQuery.data?.content ?? [];
  const latest = results[0];

  const durations = results.map((r) => r.durationMs);
  const failedCount = results.filter((r) => r.status === 'FAILED').length;
  const p50 = percentile(durations, 50);
  const p95 = percentile(durations, 95);

  const chartData = [...results].reverse().map((r) => ({
    time: new Date(r.startedAt).toLocaleTimeString([], {
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
    }),
    durationMs: r.durationMs,
    failedMs: r.status === 'FAILED' ? r.durationMs : null,
  }));

  const selectedOp = OPERATORS.find((o) => o.value === ruleOp);

  return (
    <div className="mx-auto max-w-5xl p-8">
      <div className="mb-4">
        <Link to="/" className="text-sm font-medium text-slate-500 hover:text-slate-800">
          &larr; Back to all monitors
        </Link>
      </div>

      {runMessage && (
        <div
          role="status"
          className={`mb-4 flex items-center justify-between rounded-lg p-4 text-sm font-medium shadow-sm transition-all ${
            runMessage.isError
              ? 'border border-red-200 bg-red-50 text-red-800'
              : 'border border-green-200 bg-green-50 text-green-800'
          }`}
        >
          <span>{runMessage.text}</span>
          <button
            onClick={() => setRunMessage(null)}
            className="text-xs opacity-75 hover:opacity-100"
          >
            Dismiss
          </button>
        </div>
      )}

      {monitorQuery.isPending && <p className="text-slate-600">Loading...</p>}

      {monitorQuery.isError && !monitor && (
        <p role="alert" className="rounded border border-red-200 bg-red-50 p-4 text-sm text-red-800">
          Could not load this monitor.
        </p>
      )}

      {monitor && (
        <div className="rounded-lg bg-white p-6 shadow-sm">
          <div className="flex flex-wrap items-center justify-between gap-4">
            <div>
              <div className="flex flex-wrap items-center gap-3">
                <h1 className="text-2xl font-bold text-slate-900">{monitor.name}</h1>
                {!monitor.enabled ? (
                  <span className="rounded-full bg-slate-200 px-2.5 py-0.5 text-xs font-medium text-slate-700">
                    Paused
                  </span>
                ) : latest ? (
                  latest.status === 'FAILED' ? (
                    <span className="rounded-full bg-red-100 px-2.5 py-0.5 text-xs font-medium text-red-800">
                      Failing
                    </span>
                  ) : (
                    <span className="rounded-full bg-green-100 px-2.5 py-0.5 text-xs font-medium text-green-800">
                      Healthy
                    </span>
                  )
                ) : (
                  <span className="rounded-full bg-amber-100 px-2.5 py-0.5 text-xs font-medium text-amber-800">
                    Waiting for first check
                  </span>
                )}
              </div>

              <p className="mt-1.5 break-all font-mono text-xs text-slate-500">
                <span className="font-semibold text-slate-700">{monitor.method}</span> {monitor.url}
              </p>
              <p className="mt-1 text-xs text-slate-500">
                Every {monitor.intervalSeconds}s &bull; Timeout {monitor.timeoutMs} ms &bull; Expected HTTP {monitor.expectedStatus}
              </p>
            </div>

            <div className="flex flex-wrap items-center gap-2">
              <button
                disabled={runMutation.isPending}
                onClick={() => runMutation.mutate()}
                className="inline-flex items-center gap-1.5 rounded-lg bg-indigo-600 px-3.5 py-2 text-sm font-semibold text-white shadow-sm hover:bg-indigo-500 disabled:opacity-50 transition-colors"
              >
                {runMutation.isPending ? (
                  <>
                    <svg className="h-4 w-4 animate-spin" viewBox="0 0 24 24" fill="none">
                      <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                      <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8H4z" />
                    </svg>
                    Running...
                  </>
                ) : (
                  <>
                    <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M14.752 11.168l-3.197-2.132A1 1 0 0010 9.87v4.263a1 1 0 001.555.832l3.197-2.132a1 1 0 000-1.664z" />
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                    </svg>
                    Run Check Now
                  </>
                )}
              </button>

              <button
                onClick={() => setIsEditing(true)}
                className="rounded-lg border border-slate-300 px-3.5 py-2 text-sm font-medium text-slate-700 hover:bg-slate-50 transition-colors"
              >
                Edit
              </button>

              <button
                disabled={toggleMutation.isPending}
                onClick={() => toggleMutation.mutate(monitor.enabled)}
                className="rounded-lg border border-slate-300 px-3.5 py-2 text-sm font-medium text-slate-700 hover:bg-slate-50 disabled:opacity-50 transition-colors"
              >
                {monitor.enabled ? 'Pause' : 'Resume'}
              </button>

              <button
                disabled={deleteMutation.isPending}
                onClick={handleDelete}
                className="rounded-lg border border-red-200 bg-red-50 px-3.5 py-2 text-sm font-medium text-red-700 hover:bg-red-100 disabled:opacity-50 transition-colors"
              >
                Delete
              </button>
            </div>
          </div>
        </div>
      )}

      <div className="mt-6 grid grid-cols-2 gap-4 sm:grid-cols-4">
        <StatCard label="Checks loaded" value={String(results.length)} />
        <StatCard label="Failed" value={String(failedCount)} />
        <StatCard label="Latency p50" value={p50 === null ? '-' : `${p50} ms`} />
        <StatCard label="Latency p95" value={p95 === null ? '-' : `${p95} ms`} />
      </div>
      <p className="mt-1 text-xs text-slate-500">
        Percentiles are computed from the last {results.length} loaded checks only.
      </p>

      <Section title="Latency Trend">
        {results.length === 0 ? (
          <p className="rounded-lg bg-white p-6 text-slate-600 shadow-sm">No checks recorded yet.</p>
        ) : (
          <div className="rounded-lg bg-white p-5 shadow-sm">
            <div className="h-64">
              <ResponsiveContainer width="100%" height="100%">
                <LineChart data={chartData} margin={{ top: 8, right: 16, bottom: 0, left: 0 }}>
                  <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" />
                  <XAxis dataKey="time" tick={{ fontSize: 11 }} minTickGap={32} />
                  <YAxis tick={{ fontSize: 11 }} unit=" ms" width={64} />
                  <Tooltip />
                  <Line
                    type="monotone"
                    dataKey="durationMs"
                    stroke="#4f46e5"
                    strokeWidth={2}
                    dot={false}
                    name="Latency"
                  />
                  <Line
                    type="monotone"
                    dataKey="failedMs"
                    stroke="#dc2626"
                    strokeWidth={0}
                    dot={{ r: 4, fill: '#dc2626' }}
                    name="Failed check"
                  />
                </LineChart>
              </ResponsiveContainer>
            </div>
            <p className="mt-2 text-xs text-slate-500">Red dots indicate checks that failed validation.</p>
          </div>
        )}
      </Section>

      {/* Business Rules (Assertions) Section */}
      <Section title="Business Rules (Assertions)">
        <div className="mb-3 flex items-center justify-between">
          <p className="text-xs text-slate-500">
            Rules catch silent API failures: the status code is 200 but response payload is invalid.
          </p>
          <button
            type="button"
            onClick={() => setShowAddRule((prev) => !prev)}
            className="text-xs font-semibold text-indigo-600 hover:text-indigo-800 underline"
          >
            {showAddRule ? 'Cancel' : '+ Add New Rule'}
          </button>
        </div>

        {showAddRule && (
          <form
            onSubmit={handleSaveRule}
            className="mb-4 rounded-lg border border-indigo-100 bg-indigo-50/50 p-4 shadow-sm space-y-3"
          >
            <h3 className="text-sm font-semibold text-slate-900">New Business Assertion</h3>
            <div className="grid gap-3 sm:grid-cols-[1fr_auto_auto]">
              <div>
                <label className="block text-xs font-medium text-slate-700">JSON Path</label>
                <input
                  className="mt-1 w-full rounded border border-slate-300 p-2 text-sm bg-white"
                  placeholder="$.data.stock"
                  value={rulePath}
                  onChange={(e) => setRulePath(e.target.value)}
                  required
                />
              </div>
              <div>
                <label className="block text-xs font-medium text-slate-700">Operator</label>
                <select
                  className="mt-1 w-full rounded border border-slate-300 p-2 text-sm bg-white"
                  value={ruleOp}
                  onChange={(e) => setRuleOp(e.target.value as AssertionOperator)}
                >
                  {OPERATORS.map((op) => (
                    <option key={op.value} value={op.value}>
                      {op.label}
                    </option>
                  ))}
                </select>
              </div>
              {selectedOp?.needsValue && (
                <div>
                  <label className="block text-xs font-medium text-slate-700">Expected Value</label>
                  <input
                    className="mt-1 w-full rounded border border-slate-300 p-2 text-sm bg-white"
                    placeholder="10"
                    value={ruleVal}
                    onChange={(e) => setRuleVal(e.target.value)}
                    required
                  />
                </div>
              )}
            </div>

            {ruleError && <p className="text-xs text-red-700">{ruleError}</p>}

            <div className="flex gap-2">
              <button
                type="submit"
                disabled={addRuleMutation.isPending}
                className="rounded bg-indigo-600 px-3 py-1.5 text-xs font-medium text-white hover:bg-indigo-500 disabled:opacity-50"
              >
                {addRuleMutation.isPending ? 'Saving...' : 'Save Rule'}
              </button>
              <button
                type="button"
                onClick={() => setShowAddRule(false)}
                className="rounded border border-slate-300 px-3 py-1.5 text-xs font-medium text-slate-700 hover:bg-slate-100"
              >
                Cancel
              </button>
            </div>
          </form>
        )}

        {assertionsQuery.data && assertionsQuery.data.length === 0 && (
          <p className="rounded-lg bg-white p-5 text-sm text-slate-600 shadow-sm">
            No business rules configured. This monitor only validates the HTTP response status.
          </p>
        )}

        {assertionsQuery.data && assertionsQuery.data.length > 0 && (
          <ul className="divide-y rounded-lg bg-white shadow-sm">
            {assertionsQuery.data.map((a) => (
              <li key={a.id} className="flex items-center justify-between p-3.5 font-mono text-sm text-slate-800">
                <div>
                  <span className="font-semibold text-indigo-600">{a.path}</span> {a.operator}
                  {a.expectedValue !== null && <span className="font-semibold"> {a.expectedValue}</span>}
                </div>
                <button
                  type="button"
                  title="Delete rule"
                  disabled={deleteRuleMutation.isPending}
                  onClick={() => {
                    if (window.confirm(`Delete rule "${a.path} ${a.operator}"?`)) {
                      deleteRuleMutation.mutate(a.id);
                    }
                  }}
                  className="rounded p-1 text-slate-400 hover:bg-red-50 hover:text-red-600 transition-colors"
                >
                  <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                  </svg>
                </button>
              </li>
            ))}
          </ul>
        )}
      </Section>

      {/* JSON Schema Validation Section */}
      <Section title="JSON Schema Validation">
        <div className="mb-3 flex items-center justify-between">
          <p className="text-xs text-slate-500">
            Validate entire response payloads against a JSON Schema draft-07/2020-12 specification.
          </p>
          <button
            type="button"
            onClick={() => setShowSchemaEditor((prev) => !prev)}
            className="text-xs font-semibold text-indigo-600 hover:text-indigo-800 underline"
          >
            {showSchemaEditor ? 'Close' : schemaQuery.data ? 'Edit Schema' : '+ Add JSON Schema'}
          </button>
        </div>

        {showSchemaEditor && (
          <form
            onSubmit={handleSaveSchema}
            className="mb-4 rounded-lg border border-slate-200 bg-white p-4 shadow-sm space-y-3"
          >
            <label className="block text-xs font-medium text-slate-700">
              JSON Schema Definition (JSON)
            </label>
            <textarea
              className="w-full h-48 rounded border border-slate-300 p-2.5 font-mono text-xs text-slate-800 bg-slate-50 focus:bg-white"
              placeholder={`{\n  "type": "object",\n  "required": ["data"],\n  "properties": {\n    "data": { "type": "object" }\n  }\n}`}
              value={schemaText}
              onChange={(e) => setSchemaText(e.target.value)}
              required
            />
            {schemaError && <p className="text-xs text-red-700">{schemaError}</p>}
            <div className="flex gap-2">
              <button
                type="submit"
                disabled={saveSchemaMutation.isPending}
                className="rounded bg-indigo-600 px-3 py-1.5 text-xs font-medium text-white hover:bg-indigo-500 disabled:opacity-50"
              >
                {saveSchemaMutation.isPending ? 'Saving...' : 'Save Schema'}
              </button>
              {schemaQuery.data && (
                <button
                  type="button"
                  disabled={deleteSchemaMutation.isPending}
                  onClick={() => {
                    if (window.confirm('Remove JSON Schema validation from this monitor?')) {
                      deleteSchemaMutation.mutate();
                      setShowSchemaEditor(false);
                    }
                  }}
                  className="rounded border border-red-300 px-3 py-1.5 text-xs font-medium text-red-700 hover:bg-red-50"
                >
                  Delete Schema
                </button>
              )}
              <button
                type="button"
                onClick={() => setShowSchemaEditor(false)}
                className="rounded border border-slate-300 px-3 py-1.5 text-xs font-medium text-slate-700 hover:bg-slate-100"
              >
                Cancel
              </button>
            </div>
          </form>
        )}

        {!showSchemaEditor && (
          schemaQuery.data ? (
            <div className="rounded-lg bg-white p-4 shadow-sm">
              <div className="flex items-center justify-between pb-2 mb-2 border-b border-slate-100">
                <span className="text-xs font-semibold uppercase tracking-wider text-green-700">
                  Active Schema Contract
                </span>
                <span className="text-xs text-slate-400 font-mono">JSON Schema</span>
              </div>
              <pre className="max-h-48 overflow-y-auto font-mono text-xs text-slate-700 bg-slate-50 p-3 rounded">
                {JSON.stringify(schemaQuery.data, null, 2)}
              </pre>
            </div>
          ) : (
            <p className="rounded-lg bg-white p-5 text-sm text-slate-600 shadow-sm">
              No JSON Schema configured. Responses will not be validated against a JSON contract.
            </p>
          )
        )}
      </Section>

      <Section title="Monitor Incidents">
        {incidentsQuery.data && incidentsQuery.data.content.length === 0 && (
          <p className="rounded-lg bg-white p-5 text-sm text-slate-600 shadow-sm">
            No incidents recorded for this monitor.
          </p>
        )}
        {incidentsQuery.data && incidentsQuery.data.content.length > 0 && (
          <ul className="space-y-2.5">
            {incidentsQuery.data.content.map((incident) => (
              <li key={incident.id} className="rounded-lg bg-white p-4 shadow-sm">
                <div className="flex flex-wrap items-center gap-2">
                  {incident.status === 'OPEN' ? (
                    <span className="rounded-full bg-red-100 px-2.5 py-0.5 text-xs font-semibold text-red-800">
                      Open
                    </span>
                  ) : (
                    <span className="rounded-full bg-green-100 px-2.5 py-0.5 text-xs font-semibold text-green-800">
                      Resolved
                    </span>
                  )}
                  <span className="text-sm font-semibold text-slate-900">
                    {failureLabel(incident.failureCode)}
                  </span>
                  <span className="text-xs text-slate-500">
                    {incident.failureCount} failed checks
                  </span>
                </div>
                {incident.reason && (
                  <p className="mt-2 break-words text-sm text-slate-700 bg-slate-50 p-2.5 rounded border border-slate-200 font-mono text-xs">
                    {incident.reason}
                  </p>
                )}
                <p className="mt-2 text-xs text-slate-500">
                  Opened {formatDateTime(incident.openedAt)}
                  {incident.resolvedAt && `, resolved ${formatDateTime(incident.resolvedAt)}`}
                </p>
              </li>
            ))}
          </ul>
        )}
      </Section>

      <Section title="Recent Check History">
        {resultsQuery.data && results.length > 0 && (
          <div className="overflow-x-auto rounded-lg bg-white shadow-sm">
            <table className="w-full text-left text-sm">
              <thead className="border-b bg-slate-50 text-xs uppercase tracking-wide text-slate-500">
                <tr>
                  <th className="px-4 py-3">Time</th>
                  <th className="px-4 py-3">Result</th>
                  <th className="px-4 py-3">HTTP</th>
                  <th className="px-4 py-3">Latency</th>
                  <th className="px-4 py-3">Details</th>
                </tr>
              </thead>
              <tbody className="divide-y">
                {results.map((r) => (
                  <tr key={r.id} className="align-top hover:bg-slate-50/50">
                    <td className="whitespace-nowrap px-4 py-3 text-slate-600" title={formatDateTime(r.startedAt)}>
                      {timeAgo(r.startedAt)}
                      {r.trigger === 'MANUAL' && (
                        <span className="ml-1.5 rounded bg-slate-100 px-1.5 py-0.5 text-xs text-slate-600 font-medium">manual</span>
                      )}
                    </td>
                    <td className="px-4 py-3">
                      {r.status === 'FAILED' ? (
                        <span className="inline-block rounded-full bg-red-100 px-2.5 py-0.5 text-xs font-semibold text-red-800">
                          Failed
                        </span>
                      ) : (
                        <span className="inline-block rounded-full bg-green-100 px-2.5 py-0.5 text-xs font-semibold text-green-800">
                          Passed
                        </span>
                      )}
                    </td>
                    <td className="px-4 py-3 text-slate-700 font-mono text-xs">{r.httpStatus ?? '-'}</td>
                    <td className="whitespace-nowrap px-4 py-3 text-slate-700 font-mono text-xs">{r.durationMs} ms</td>
                    <td className="px-4 py-3 text-slate-700">
                      {r.status === 'FAILED' && (
                        <>
                          <span className="font-semibold text-slate-900">{failureLabel(r.failureCode)}</span>
                          {r.reason && <span className="block mt-0.5 break-words text-slate-600 font-mono text-xs">{r.reason}</span>}
                        </>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
        {resultsQuery.data && resultsQuery.data.totalElements > results.length && (
          <p className="mt-2 text-xs text-slate-500">
            Showing the latest {results.length} of {resultsQuery.data.totalElements} checks.
          </p>
        )}
      </Section>

      {isEditing && monitor && (
        <EditMonitorModal
          monitor={monitor}
          onClose={() => setIsEditing(false)}
          onSuccess={() => {
            setIsEditing(false);
            queryClient.invalidateQueries({ queryKey: ['monitor', monitorId] });
            queryClient.invalidateQueries({ queryKey: ['monitors'] });
          }}
        />
      )}
    </div>
  );
}
