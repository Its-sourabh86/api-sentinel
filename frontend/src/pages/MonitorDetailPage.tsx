import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Link, useNavigate, useParams } from 'react-router-dom';
import {
  Area,
  AreaChart,
  CartesianGrid,
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
import RadialGauge from '../components/RadialGauge';
import { failureLabel } from '../lib/labels';
import { percentile } from '../lib/stats';
import { formatDateTime, timeAgo } from '../lib/time';

const OPERATORS: { value: AssertionOperator; label: string; needsValue: boolean }[] = [
  { value: 'GTE', label: 'is at least (>=)', needsValue: true },
  { value: 'GT', label: 'is greater than (>)', needsValue: true },
  { value: 'LTE', label: 'is at most (<=)', needsValue: true },
  { value: 'LT', label: 'is less than (<)', needsValue: true },
  { value: 'EQ', label: 'equals (==)', needsValue: true },
  { value: 'NEQ', label: 'does not equal (!=)', needsValue: true },
  { value: 'EXISTS', label: 'exists', needsValue: false },
  { value: 'NOT_EMPTY', label: 'is not empty', needsValue: false },
  { value: 'AGE_LTE', label: 'is not older than (seconds)', needsValue: true },
];

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
    refetchInterval: 10_000,
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
    refetchInterval: 10_000,
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

  if (!validId || (monitorQuery.isError && monitorQuery.error instanceof ApiError && monitorQuery.error.status === 404)) {
    return (
      <div className="min-h-screen bg-[#0B0F19] p-8 text-slate-100 font-mono">
        <p className="rounded-xl bg-[#111827] border border-slate-800 p-6">Target monitor not found in registry.</p>
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

  const uptimePercentage = results.length > 0 ? ((results.length - failedCount) / results.length) * 100 : 100;

  // Chart data for AreaChart with gradient
  const chartData = [...results].reverse().map((r) => ({
    time: new Date(r.startedAt).toLocaleTimeString([], {
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
    }),
    durationMs: r.durationMs,
    isFailed: r.status === 'FAILED',
  }));

  const selectedOp = OPERATORS.find((o) => o.value === ruleOp);

  return (
    <div className="min-h-screen bg-[#0B0F19] text-slate-100 p-6 lg:p-8 space-y-6">
      {/* Back Link */}
      <div>
        <Link to="/" className="inline-flex items-center gap-1.5 font-mono text-xs text-indigo-400 hover:text-indigo-300">
          ← Back to Cluster Overview
        </Link>
      </div>

      {/* Real-time Status Alert Banner */}
      {runMessage && (
        <div
          role="status"
          className={`flex items-center justify-between rounded-xl p-4 font-mono text-xs font-bold border shadow-lg ${
            runMessage.isError
              ? 'border-rose-500/50 bg-rose-500/10 text-rose-300'
              : 'border-emerald-500/50 bg-emerald-500/10 text-emerald-300'
          }`}
        >
          <span>{runMessage.text}</span>
          <button onClick={() => setRunMessage(null)} className="opacity-75 hover:opacity-100">✕</button>
        </div>
      )}

      {/* MONITOR TELEMETRY HEADER CARD */}
      {monitor && (
        <div className="rounded-xl bg-[#111827] border border-slate-800/80 p-6 shadow-2xl">
          <div className="flex flex-wrap items-center justify-between gap-4">
            <div>
              <div className="flex items-center gap-3">
                <span className={`h-3 w-3 rounded-full ${!monitor.enabled ? 'bg-slate-500' : latest?.status === 'FAILED' ? 'bg-rose-500 animate-pulse' : 'bg-emerald-400 shadow-[0_0_10px_#10b981]'}`} />
                <h1 className="text-2xl font-black font-mono text-white tracking-tight">{monitor.name}</h1>
                <span className={`rounded px-2.5 py-0.5 text-[10px] font-mono font-bold uppercase tracking-wider ${
                  !monitor.enabled
                    ? 'bg-slate-800 text-slate-400 border border-slate-700'
                    : latest?.status === 'FAILED'
                    ? 'bg-rose-500/20 text-rose-400 border border-rose-500/40'
                    : 'bg-emerald-500/20 text-emerald-400 border border-emerald-500/40'
                }`}>
                  {!monitor.enabled ? 'STANDBY' : latest?.status === 'FAILED' ? 'CRITICAL FAILURE' : 'HEALTHY'}
                </span>
              </div>

              <p className="mt-2 font-mono text-xs text-slate-400 break-all">
                <span className="font-bold text-indigo-400">{monitor.method}</span> {monitor.url}
              </p>
              <p className="mt-1 font-mono text-[11px] text-slate-500">
                POLL FREQUENCY: {monitor.intervalSeconds}s · TIMEOUT: {monitor.timeoutMs}ms · EXPECTED HTTP: {monitor.expectedStatus}
              </p>
            </div>

            {/* Action Buttons */}
            <div className="flex flex-wrap items-center gap-2">
              <button
                disabled={runMutation.isPending}
                onClick={() => runMutation.mutate()}
                className="inline-flex items-center gap-2 rounded-lg bg-emerald-600 hover:bg-emerald-500 px-4 py-2 text-xs font-bold font-mono uppercase text-white shadow-lg shadow-emerald-600/30 transition-all hover:scale-105 disabled:opacity-50"
              >
                {runMutation.isPending ? 'PROBING...' : '⚡ RUN CHECK NOW'}
              </button>

              <button
                onClick={() => setIsEditing(true)}
                className="rounded-lg bg-slate-800 hover:bg-slate-700 border border-slate-700 px-3.5 py-2 text-xs font-mono text-slate-200 transition-colors"
              >
                Edit Target
              </button>

              <button
                disabled={toggleMutation.isPending}
                onClick={() => toggleMutation.mutate(monitor.enabled)}
                className="rounded-lg bg-slate-800 hover:bg-slate-700 border border-slate-700 px-3.5 py-2 text-xs font-mono text-slate-200 transition-colors disabled:opacity-50"
              >
                {monitor.enabled ? 'Pause' : 'Resume'}
              </button>

              <button
                disabled={deleteMutation.isPending}
                onClick={handleDelete}
                className="rounded-lg bg-rose-950/40 hover:bg-rose-900/60 border border-rose-800/80 px-3.5 py-2 text-xs font-mono text-rose-400 transition-colors disabled:opacity-50"
              >
                Delete
              </button>
            </div>
          </div>
        </div>
      )}

      {/* METRIC GAUGES & STATS BAR */}
      <div className="grid grid-cols-2 md:grid-cols-4 lg:grid-cols-6 gap-4">
        {/* Radial Gauge 1 */}
        <div className="col-span-1 md:col-span-2">
          <RadialGauge
            value={uptimePercentage}
            label="Uptime Availability"
            sublabel={`${results.length} CHECKS`}
            size={140}
          />
        </div>

        {/* Stat Cards */}
        <div className="flex flex-col justify-center rounded-xl bg-[#111827] border border-slate-800/80 p-4 font-mono">
          <span className="text-[10px] font-bold text-slate-500 uppercase">CHECKS SAMPLED</span>
          <span className="text-3xl font-black text-white mt-1">{results.length}</span>
          <span className="text-[11px] text-slate-500 mt-1">Rolling window</span>
        </div>

        <div className="flex flex-col justify-center rounded-xl bg-[#111827] border border-slate-800/80 p-4 font-mono">
          <span className="text-[10px] font-bold text-slate-500 uppercase">FAILED RATIO</span>
          <span className={`text-3xl font-black mt-1 ${failedCount > 0 ? 'text-rose-500' : 'text-emerald-400'}`}>
            {failedCount}
          </span>
          <span className="text-[11px] text-slate-500 mt-1">{failedCount > 0 ? 'Violations' : 'Zero errors'}</span>
        </div>

        <div className="flex flex-col justify-center rounded-xl bg-[#111827] border border-slate-800/80 p-4 font-mono">
          <span className="text-[10px] font-bold text-slate-500 uppercase">LATENCY (P50)</span>
          <span className="text-3xl font-black text-indigo-400 mt-1">{p50 === null ? '-' : `${p50} ms`}</span>
          <span className="text-[11px] text-slate-500 mt-1">Median response</span>
        </div>

        <div className="flex flex-col justify-center rounded-xl bg-[#111827] border border-slate-800/80 p-4 font-mono">
          <span className="text-[10px] font-bold text-slate-500 uppercase">LATENCY (P95)</span>
          <span className="text-3xl font-black text-cyan-400 mt-1">{p95 === null ? '-' : `${p95} ms`}</span>
          <span className="text-[11px] text-slate-500 mt-1">95th percentile</span>
        </div>
      </div>

      {/* GRAFANA-STYLE GLOWING AREA CHART */}
      <section className="rounded-xl bg-[#111827] border border-slate-800/80 p-5 shadow-2xl">
        <div className="flex items-center justify-between mb-4 border-b border-slate-800/60 pb-2 font-mono">
          <span className="text-xs font-bold uppercase tracking-wider text-slate-400 flex items-center gap-2">
            <span className="text-indigo-400">📈</span> Response Latency Telemetry (ms)
          </span>
          <span className="text-[11px] text-slate-500">REAL-TIME PROBE TIMELINE</span>
        </div>

        {results.length === 0 ? (
          <div className="p-12 text-center text-slate-500 font-mono text-xs">No probe checks executed yet.</div>
        ) : (
          <div className="h-64">
            <ResponsiveContainer width="100%" height="100%">
              <AreaChart data={chartData} margin={{ top: 10, right: 10, bottom: 0, left: 0 }}>
                <defs>
                  <linearGradient id="latencyGlow" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="#10B981" stopOpacity={0.4} />
                    <stop offset="95%" stopColor="#10B981" stopOpacity={0.0} />
                  </linearGradient>
                </defs>
                <CartesianGrid strokeDasharray="3 3" stroke="#1E293B" />
                <XAxis dataKey="time" stroke="#64748B" tick={{ fontSize: 10, fill: '#64748B' }} />
                <YAxis stroke="#64748B" unit=" ms" tick={{ fontSize: 10, fill: '#64748B' }} width={50} />
                <Tooltip
                  contentStyle={{
                    backgroundColor: '#0E1422',
                    borderColor: '#1E293B',
                    borderRadius: '8px',
                    fontFamily: 'monospace',
                    fontSize: '11px',
                    color: '#F1F5F9',
                  }}
                />
                <Area
                  type="monotone"
                  dataKey="durationMs"
                  stroke="#10B981"
                  strokeWidth={2}
                  fillOpacity={1}
                  fill="url(#latencyGlow)"
                  name="Latency"
                />
              </AreaChart>
            </ResponsiveContainer>
          </div>
        )}
      </section>

      {/* BUSINESS RULES & ASSERTIONS */}
      <section className="rounded-xl bg-[#111827] border border-slate-800/80 p-5 shadow-2xl">
        <div className="flex items-center justify-between mb-4 border-b border-slate-800/60 pb-2">
          <span className="text-xs font-bold uppercase font-mono tracking-wider text-slate-400 flex items-center gap-2">
            <span className="text-emerald-400">🛡️</span> Business Contract Assertions
          </span>
          <button
            type="button"
            onClick={() => setShowAddRule((p) => !p)}
            className="text-xs font-mono font-bold text-indigo-400 hover:text-indigo-300"
          >
            {showAddRule ? 'Close Form' : '+ Add New Assertion'}
          </button>
        </div>

        {showAddRule && (
          <form onSubmit={handleSaveRule} className="mb-4 rounded-lg bg-[#0E1422] border border-indigo-500/30 p-4 space-y-3 font-mono">
            <div className="grid gap-3 sm:grid-cols-3">
              <div>
                <label className="text-[10px] uppercase text-slate-400">JSON Path</label>
                <input
                  className="mt-1 w-full rounded bg-[#111827] border border-slate-700 p-2 text-xs text-white"
                  placeholder="$.data.stock"
                  value={rulePath}
                  onChange={(e) => setRulePath(e.target.value)}
                  required
                />
              </div>
              <div>
                <label className="text-[10px] uppercase text-slate-400">Operator</label>
                <select
                  className="mt-1 w-full rounded bg-[#111827] border border-slate-700 p-2 text-xs text-white"
                  value={ruleOp}
                  onChange={(e) => setRuleOp(e.target.value as AssertionOperator)}
                >
                  {OPERATORS.map((op) => (
                    <option key={op.value} value={op.value}>{op.label}</option>
                  ))}
                </select>
              </div>
              {selectedOp?.needsValue && (
                <div>
                  <label className="text-[10px] uppercase text-slate-400">Expected Value</label>
                  <input
                    className="mt-1 w-full rounded bg-[#111827] border border-slate-700 p-2 text-xs text-white"
                    placeholder="10"
                    value={ruleVal}
                    onChange={(e) => setRuleVal(e.target.value)}
                    required
                  />
                </div>
              )}
            </div>

            {ruleError && <p className="text-xs text-rose-400">{ruleError}</p>}

            <div className="flex gap-2">
              <button
                type="submit"
                disabled={addRuleMutation.isPending}
                className="rounded bg-indigo-600 px-3 py-1.5 text-xs font-bold text-white hover:bg-indigo-500"
              >
                {addRuleMutation.isPending ? 'Saving...' : 'Save Assertion'}
              </button>
              <button
                type="button"
                onClick={() => setShowAddRule(false)}
                className="rounded bg-slate-800 px-3 py-1.5 text-xs text-slate-400"
              >
                Cancel
              </button>
            </div>
          </form>
        )}

        {assertionsQuery.data && assertionsQuery.data.length === 0 && (
          <p className="text-xs font-mono text-slate-500">No custom rules. Validating HTTP status code only.</p>
        )}

        {assertionsQuery.data && assertionsQuery.data.length > 0 && (
          <ul className="divide-y divide-slate-800/80">
            {assertionsQuery.data.map((a) => (
              <li key={a.id} className="flex items-center justify-between py-2.5 font-mono text-xs">
                <div>
                  <span className="font-bold text-indigo-400">{a.path}</span> <span className="text-slate-400">{a.operator}</span>
                  {a.expectedValue !== null && <span className="font-bold text-emerald-400"> {a.expectedValue}</span>}
                </div>
                <button
                  type="button"
                  disabled={deleteRuleMutation.isPending}
                  onClick={() => {
                    if (window.confirm(`Delete rule "${a.path}"?`)) deleteRuleMutation.mutate(a.id);
                  }}
                  className="text-slate-500 hover:text-rose-400 transition-colors"
                >
                  Delete ✕
                </button>
              </li>
            ))}
          </ul>
        )}
      </section>

      {/* JSON SCHEMA VALIDATOR */}
      <section className="rounded-xl bg-[#111827] border border-slate-800/80 p-5 shadow-2xl font-mono">
        <div className="flex items-center justify-between mb-4 border-b border-slate-800/60 pb-2">
          <span className="text-xs font-bold uppercase tracking-wider text-slate-400 flex items-center gap-2">
            <span className="text-cyan-400">📄</span> JSON Schema Contract
          </span>
          <button
            type="button"
            onClick={() => setShowSchemaEditor((p) => !p)}
            className="text-xs font-bold text-cyan-400 hover:text-cyan-300"
          >
            {showSchemaEditor ? 'Close' : schemaQuery.data ? 'Edit Contract' : '+ Add JSON Schema'}
          </button>
        </div>

        {showSchemaEditor && (
          <form onSubmit={handleSaveSchema} className="space-y-3">
            <textarea
              className="w-full h-44 rounded-lg bg-[#0E1422] border border-slate-700 p-3 font-mono text-xs text-emerald-400 focus:outline-none focus:border-cyan-500"
              placeholder={`{\n  "type": "object",\n  "required": ["data"]\n}`}
              value={schemaText}
              onChange={(e) => setSchemaText(e.target.value)}
              required
            />
            {schemaError && <p className="text-xs text-rose-400">{schemaError}</p>}
            <div className="flex gap-2">
              <button
                type="submit"
                disabled={saveSchemaMutation.isPending}
                className="rounded bg-cyan-600 px-3 py-1.5 text-xs font-bold text-white hover:bg-cyan-500"
              >
                Save Schema
              </button>
              {schemaQuery.data && (
                <button
                  type="button"
                  onClick={() => {
                    if (window.confirm('Delete schema?')) {
                      deleteSchemaMutation.mutate();
                      setShowSchemaEditor(false);
                    }
                  }}
                  className="rounded bg-rose-950 px-3 py-1.5 text-xs font-bold text-rose-400"
                >
                  Delete
                </button>
              )}
            </div>
          </form>
        )}

        {!showSchemaEditor && (
          schemaQuery.data ? (
            <pre className="max-h-40 overflow-y-auto rounded-lg bg-[#0E1422] p-3 text-xs text-emerald-400 border border-slate-800">
              {JSON.stringify(schemaQuery.data, null, 2)}
            </pre>
          ) : (
            <p className="text-xs text-slate-500">No JSON Schema configured. Responses will not be validated against a JSON contract.</p>
          )
        )}
      </section>

      {/* MONITOR INCIDENTS LOG */}
      <section className="rounded-xl bg-[#111827] border border-slate-800/80 p-5 shadow-2xl font-mono">
        <div className="flex items-center justify-between mb-4 border-b border-slate-800/60 pb-2">
          <span className="text-xs font-bold uppercase tracking-wider text-slate-400 flex items-center gap-2">
            <span className="text-rose-400">🚨</span> Monitor Incidents Ledger
          </span>
          <span className="text-[11px] text-slate-500">
            {incidentsQuery.data?.content.length ?? 0} INCIDENTS RECORDED
          </span>
        </div>

        {incidentsQuery.data && incidentsQuery.data.content.length === 0 && (
          <p className="text-xs text-slate-500">No incident events recorded for this target.</p>
        )}

        {incidentsQuery.data && incidentsQuery.data.content.length > 0 && (
          <div className="space-y-2.5">
            {incidentsQuery.data.content.map((inc) => (
              <div key={inc.id} className="rounded-lg bg-[#0E1422] border border-slate-800 p-3 flex items-center justify-between">
                <div className="flex items-center gap-3">
                  <span className={`rounded px-2 py-0.5 text-[10px] font-bold uppercase ${
                    inc.status === 'OPEN' ? 'bg-rose-500/20 text-rose-400 border border-rose-500/30' : 'bg-emerald-500/20 text-emerald-400 border border-emerald-500/30'
                  }`}>
                    {inc.status}
                  </span>
                  <span className="text-xs text-white font-bold">{failureLabel(inc.failureCode)}</span>
                  <span className="text-[11px] text-slate-400">{inc.failureCount} failed checks</span>
                </div>
                <span className="text-[11px] text-slate-500">{formatDateTime(inc.openedAt)}</span>
              </div>
            ))}
          </div>
        )}
      </section>

      {/* RECENT CHECK HISTORY TABLE */}
      <section className="rounded-xl bg-[#111827] border border-slate-800/80 p-5 shadow-2xl font-mono">
        <div className="flex items-center justify-between mb-4 border-b border-slate-800/60 pb-2">
          <span className="text-xs font-bold uppercase tracking-wider text-slate-400 flex items-center gap-2">
            <span className="text-indigo-400">⏱</span> Recent Execution History
          </span>
          <span className="text-[11px] text-slate-500">SHOWING LAST {results.length} CHECKS</span>
        </div>

        {results.length > 0 && (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead className="border-b border-slate-800 text-[10px] text-slate-500 uppercase">
                <tr>
                  <th className="py-2.5 px-3">Timestamp</th>
                  <th className="py-2.5 px-3">Status</th>
                  <th className="py-2.5 px-3">HTTP</th>
                  <th className="py-2.5 px-3">Latency</th>
                  <th className="py-2.5 px-3">Trigger</th>
                  <th className="py-2.5 px-3">Diagnostics</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800/60">
                {results.map((r) => (
                  <tr key={r.id} className="hover:bg-slate-800/30 transition-colors">
                    <td className="py-2.5 px-3 text-slate-400" title={formatDateTime(r.startedAt)}>
                      {timeAgo(r.startedAt)}
                    </td>
                    <td className="py-2.5 px-3">
                      <span className={`rounded px-1.5 py-0.5 text-[10px] font-bold uppercase ${
                        r.status === 'FAILED' ? 'bg-rose-500/20 text-rose-400 border border-rose-500/30' : 'bg-emerald-500/20 text-emerald-400 border border-emerald-500/30'
                      }`}>
                        {r.status}
                      </span>
                    </td>
                    <td className="py-2.5 px-3 text-slate-300 font-bold">{r.httpStatus ?? '-'}</td>
                    <td className="py-2.5 px-3 text-indigo-400 font-bold">{r.durationMs} ms</td>
                    <td className="py-2.5 px-3 text-slate-500 uppercase">{r.trigger}</td>
                    <td className="py-2.5 px-3 text-slate-300">
                      {r.status === 'FAILED' ? (
                        <span className="text-rose-400">
                          {failureLabel(r.failureCode)}: {r.reason}
                        </span>
                      ) : (
                        <span className="text-slate-500">Passed</span>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>

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
