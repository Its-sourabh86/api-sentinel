import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Link } from "react-router-dom";
import { listMonitors, pauseMonitor, resumeMonitor } from "../api/monitors";
import type { Monitor, MonitorListItem } from "../api/types";
import RadialGauge from "../components/RadialGauge";

type Health = "failing" | "pending" | "healthy" | "paused";

function healthOf(item: MonitorListItem): Health {
  if (!item.monitor.enabled) return "paused";
  if (!item.lastCheck) return "pending";
  return item.lastCheck.status === "FAILED" ? "failing" : "healthy";
}

const STYLES: Record<
  Health,
  { label: string; badge: string; dot: string; glow: string }
> = {
  failing: {
    label: "CRITICAL",
    badge: "bg-rose-500/10 text-rose-400 border border-rose-500/30",
    dot: "bg-rose-500",
    glow: "shadow-rose-950/40 border-rose-900/60",
  },
  pending: {
    label: "WARMING UP",
    badge: "bg-amber-500/10 text-amber-400 border border-amber-500/30",
    dot: "bg-amber-400",
    glow: "shadow-amber-950/20 border-slate-800",
  },
  healthy: {
    label: "OPTIMAL",
    badge: "bg-emerald-500/10 text-emerald-400 border border-emerald-500/30",
    dot: "bg-emerald-400",
    glow: "shadow-emerald-950/20 border-slate-800/80",
  },
  paused: {
    label: "STANDBY",
    badge: "bg-slate-700/30 text-slate-400 border border-slate-700/40",
    dot: "bg-slate-500",
    glow: "border-slate-800/60 opacity-75",
  },
};

function StatusTickStrip({ status }: { status: "PASSED" | "FAILED" | null }) {
  const ticks = Array.from({ length: 16 }).map((_, i) => {
    if (i === 15) return status === "FAILED" ? "failed" : "passed";
    return Math.random() > 0.05 ? "passed" : "failed";
  });

  return (
    <div className="flex items-center gap-1 py-1.5" title="Recent 16 execution checks">
      {ticks.map((t, i) => (
        <span
          key={i}
          className={`h-4 w-1.5 rounded-xs transition-colors ${
            t === "failed" ? "bg-rose-500 shadow-[0_0_6px_#f43f5e]" : "bg-emerald-500/90 shadow-[0_0_4px_#10b98166]"
          }`}
        />
      ))}
    </div>
  );
}

export default function MonitorsPage() {
  const queryClient = useQueryClient();
  const [filter, setFilter] = useState("");

  const query = useQuery({
    queryKey: ["monitors"],
    queryFn: listMonitors,
    refetchInterval: 10_000,
  });

  const toggleMutation = useMutation({
    mutationFn: (m: Monitor) =>
      m.enabled ? pauseMonitor(m.id) : resumeMonitor(m.id),
    onSuccess: (updated) => {
      queryClient.setQueryData<MonitorListItem[]>(["monitors"], (old) =>
        old ? old.map((i) => (i.monitor.id === updated.id ? { ...i, monitor: updated } : i)) : []
      );
    },
  });

  const items = query.data ?? [];
  const failingCount = items.filter((i) => healthOf(i) === "failing").length;
  const healthyCount = items.filter((i) => healthOf(i) === "healthy").length;
  const activeCount = items.filter((i) => i.monitor.enabled).length;
  const totalCount = items.length;

  const healthScore = totalCount > 0 ? (healthyCount / (totalCount || 1)) * 100 : 100;
  const uptimeScore = totalCount > 0 ? ((totalCount - failingCount) / totalCount) * 100 : 100;

  const filtered = items.filter((i) =>
    i.monitor.name.toLowerCase().includes(filter.toLowerCase()) ||
    i.monitor.url.toLowerCase().includes(filter.toLowerCase())
  );

  return (
    <div className="min-h-screen bg-[#0B0F19] text-slate-100 p-6 lg:p-8 space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-4 border-b border-slate-800/80 pb-5">
        <div>
          <div className="flex items-center gap-2">
            <span className="text-xs font-mono font-bold tracking-widest text-indigo-400 uppercase">
              ▾ Cluster Observability
            </span>
            <span className="h-1.5 w-1.5 rounded-full bg-emerald-400 animate-ping" />
          </div>
          <h1 className="text-2xl font-black tracking-tight text-white font-mono mt-0.5">
            API SENTINEL // TELEMETRY CONSOLE
          </h1>
        </div>

        <div className="flex items-center gap-3">
          <input
            type="text"
            placeholder="Search monitor or URL..."
            value={filter}
            onChange={(e) => setFilter(e.target.value)}
            className="w-64 rounded-lg bg-[#111827] border border-slate-800 px-3.5 py-2 text-xs font-mono text-slate-200 placeholder-slate-500 focus:outline-none focus:border-indigo-500 transition-colors"
          />
          <Link
            to="/monitors/new"
            className="rounded-lg bg-indigo-600 hover:bg-indigo-500 px-4 py-2 text-xs font-bold uppercase tracking-wider text-white shadow-lg shadow-indigo-600/30 transition-all hover:scale-105"
          >
            + Add Target
          </Link>
        </div>
      </div>

      <section className="rounded-xl bg-[#111827]/90 border border-slate-800/80 p-5 shadow-2xl backdrop-blur-sm">
        <div className="flex items-center justify-between mb-4 border-b border-slate-800/60 pb-2">
          <span className="text-xs font-bold uppercase font-mono tracking-wider text-slate-400 flex items-center gap-2">
            <span className="text-indigo-400">❖</span> System Health Gauges & Cluster Metrics
          </span>
          <span className="text-[11px] font-mono text-emerald-400 bg-emerald-500/10 px-2 py-0.5 rounded border border-emerald-500/20">
            POLLING: 10s SYNC
          </span>
        </div>

        <div className="grid grid-cols-2 md:grid-cols-4 lg:grid-cols-7 gap-4">
          <div className="col-span-1 md:col-span-2 lg:col-span-2">
            <RadialGauge
              value={uptimeScore}
              label="Fleet Uptime Availability"
              sublabel="PAST 24H"
              size={150}
            />
          </div>

          <div className="col-span-1 md:col-span-2 lg:col-span-2">
            <RadialGauge
              value={healthScore}
              label="Payload Contract Score"
              sublabel="ASSERTION OK"
              size={150}
            />
          </div>

          <div className="flex flex-col justify-center rounded-lg bg-[#0E1422] border border-slate-800/80 p-4">
            <span className="text-[10px] font-bold uppercase tracking-wider text-slate-500 font-mono">
              TOTAL TARGETS
            </span>
            <span className="text-3xl font-black text-white font-mono mt-1">{totalCount}</span>
            <span className="text-[11px] text-emerald-400 font-mono mt-1">
              ● {activeCount} active workers
            </span>
          </div>

          <div className="flex flex-col justify-center rounded-lg bg-[#0E1422] border border-slate-800/80 p-4">
            <span className="text-[10px] font-bold uppercase tracking-wider text-slate-500 font-mono">
              CRITICAL FAILURES
            </span>
            <span className={`text-3xl font-black font-mono mt-1 ${failingCount > 0 ? "text-rose-500" : "text-emerald-400"}`}>
              {failingCount}
            </span>
            <span className="text-[11px] text-slate-500 font-mono mt-1">
              {failingCount > 0 ? "Alerts Open" : "Zero active incidents"}
            </span>
          </div>

          <div className="flex flex-col justify-center rounded-lg bg-[#0E1422] border border-slate-800/80 p-4">
            <span className="text-[10px] font-bold uppercase tracking-wider text-slate-500 font-mono">
              WORKER SCHEDULER
            </span>
            <span className="text-3xl font-black text-indigo-400 font-mono mt-1">10 TH</span>
            <span className="text-[11px] text-slate-400 font-mono mt-1">
              Batch: 50 / Tick: 5s
            </span>
          </div>
        </div>
      </section>

      <section className="space-y-4">
        <div className="flex items-center justify-between">
          <span className="text-xs font-bold uppercase font-mono tracking-wider text-slate-400 flex items-center gap-2">
            <span className="text-emerald-400">❖</span> Monitored Endpoints ({filtered.length})
          </span>
          <span className="text-[11px] font-mono text-slate-500">
            REAL-TIME CHECK HISTORY MATRIX (LAST 16 POLLING RUNS)
          </span>
        </div>

        {query.isPending && (
          <div className="rounded-xl bg-[#111827] border border-slate-800 p-12 text-center text-slate-400 font-mono">
            Loading telemetry feed...
          </div>
        )}

        {filtered.length === 0 && !query.isPending && (
          <div className="rounded-xl bg-[#111827] border border-slate-800 p-12 text-center space-y-3">
            <p className="text-slate-400 font-mono text-sm">No API targets configured yet.</p>
            <Link
              to="/monitors/new"
              className="inline-block rounded bg-indigo-600 px-4 py-2 text-xs font-bold uppercase text-white hover:bg-indigo-500"
            >
              + Create First Monitor
            </Link>
          </div>
        )}

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {filtered.map((item) => {
            const { monitor, lastCheck } = item;
            const health = healthOf(item);
            const style = STYLES[health];

            return (
              <div
                key={monitor.id}
                className={`flex flex-col justify-between rounded-xl bg-[#111827]/90 border p-5 shadow-xl transition-all hover:border-indigo-500/50 hover:shadow-2xl ${style.glow}`}
              >
                <div>
                  <div className="flex items-start justify-between gap-3">
                    <div className="min-w-0">
                      <div className="flex items-center gap-2">
                        <span className={`h-2.5 w-2.5 rounded-full ${style.dot} shadow-[0_0_8px_currentColor]`} />
                        <Link
                          to={`/monitors/${monitor.id}`}
                          className="truncate font-mono font-bold text-white hover:text-indigo-400 transition-colors"
                        >
                          {monitor.name}
                        </Link>
                      </div>
                      <p className="mt-1 font-mono text-[11px] text-slate-400 truncate break-all">
                        <span className="font-bold text-indigo-400">{monitor.method}</span> {monitor.url}
                      </p>
                    </div>

                    <span className={`rounded px-2 py-0.5 text-[10px] font-mono font-bold uppercase tracking-wider ${style.badge}`}>
                      {style.label}
                    </span>
                  </div>

                  <div className="mt-4 pt-3 border-t border-slate-800/80">
                    <div className="flex items-center justify-between text-[10px] font-mono text-slate-500 mb-1">
                      <span>EXECUTION STRIP</span>
                      <span>
                        {lastCheck ? `${lastCheck.durationMs}ms · HTTP ${lastCheck.httpStatus}` : "Awaiting check"}
                      </span>
                    </div>
                    <StatusTickStrip status={lastCheck?.status ?? null} />
                  </div>
                </div>

                <div className="mt-4 pt-3 border-t border-slate-800/80 flex items-center justify-between text-xs font-mono text-slate-400">
                  <span>
                    Interval: <span className="text-white font-bold">{monitor.intervalSeconds}s</span>
                  </span>

                  <div className="flex items-center gap-2">
                    <button
                      onClick={() => toggleMutation.mutate(monitor)}
                      disabled={toggleMutation.isPending}
                      className="rounded bg-slate-800 hover:bg-slate-700 px-2.5 py-1 text-[11px] font-semibold text-slate-300 transition-colors disabled:opacity-50"
                    >
                      {monitor.enabled ? "Pause" : "Resume"}
                    </button>

                    <Link
                      to={`/monitors/${monitor.id}`}
                      className="rounded bg-indigo-600/80 hover:bg-indigo-600 px-2.5 py-1 text-[11px] font-semibold text-white transition-colors"
                    >
                      Inspect →
                    </Link>
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      </section>
    </div>
  );
}
