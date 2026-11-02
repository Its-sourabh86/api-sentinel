import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ApiError } from "../api/client";
import { listMonitors, pauseMonitor, resumeMonitor } from "../api/monitors";
import type { Monitor, MonitorListItem } from "../api/types";
import { failureLabel } from "../lib/labels";
import { timeAgo } from "../lib/time";
import { Link } from "react-router-dom";

type Health = "failing" | "pending" | "healthy" | "paused";

function healthOf(item: MonitorListItem): Health {
  if (!item.monitor.enabled) return "paused";
  if (!item.lastCheck) return "pending";
  return item.lastCheck.status === "FAILED" ? "failing" : "healthy";
}

const STYLES: Record<
  Health,
  { label: string; border: string; badge: string; dot: string }
> = {
  failing: {
    label: "Failing",
    border: "border-l-red-500",
    badge: "bg-red-100 text-red-800",
    dot: "bg-red-500",
  },
  pending: {
    label: "Waiting for first check",
    border: "border-l-amber-400",
    badge: "bg-amber-100 text-amber-800",
    dot: "bg-amber-400",
  },
  healthy: {
    label: "Healthy",
    border: "border-l-green-500",
    badge: "bg-green-100 text-green-800",
    dot: "bg-green-500",
  },
  paused: {
    label: "Paused",
    border: "border-l-slate-300",
    badge: "bg-slate-200 text-slate-700",
    dot: "bg-slate-400",
  },
};

const ORDER: Record<Health, number> = {
  failing: 0,
  pending: 1,
  healthy: 2,
  paused: 3,
};

function SummaryCard({
  label,
  value,
  valueClass,
}: {
  label: string;
  value: number;
  valueClass: string;
}) {
  return (
    <div className="rounded-lg bg-white p-4 shadow-sm">
      <p className="text-xs font-medium uppercase tracking-wide text-slate-500">
        {label}
      </p>
      <p className={`mt-1 text-2xl font-bold ${valueClass}`}>{value}</p>
    </div>
  );
}

function MonitorCard({
  item,
  onToggle,
  busy,
}: {
  item: MonitorListItem;
  onToggle: (m: Monitor) => void;
  busy: boolean;
}) {
  const { monitor, lastCheck } = item;
  const health = healthOf(item);
  const style = STYLES[health];

  return (
    <li
      className={`rounded-lg border-l-4 bg-white p-5 shadow-sm transition-shadow hover:shadow-md ${style.border}`}
    >
      <div className="flex items-start justify-between gap-4">
        <div className="min-w-0 flex-1">
          <div className="flex items-center gap-2">
            <span
              className={`inline-block h-2 w-2 rounded-full ${style.dot}`}
              aria-hidden="true"
            />
            <Link
              to={`/monitors/${monitor.id}`}
              className="truncate font-semibold text-slate-900 hover:text-indigo-600 hover:underline"
            >
              {monitor.name}
            </Link>
            <span
              className={`rounded-full px-2 py-0.5 text-xs font-medium ${style.badge}`}
            >
              {style.label}
            </span>
          </div>

          <p className="mt-1 truncate font-mono text-xs text-slate-500">
            <span className="font-semibold text-slate-700">{monitor.method}</span>{" "}
            {monitor.url}
          </p>
        </div>

        <button
          className="rounded border border-slate-300 px-3 py-1 text-xs font-medium text-slate-700 hover:bg-slate-50 disabled:opacity-50"
          disabled={busy}
          onClick={() => onToggle(monitor)}
        >
          {monitor.enabled ? "Pause" : "Resume"}
        </button>
      </div>

      <div className="mt-3 flex flex-wrap gap-x-6 gap-y-1 text-xs text-slate-600">
        <span>Every {monitor.intervalSeconds}s</span>
        {lastCheck ? (
          <>
            <span>Last check {timeAgo(lastCheck.checkedAt)}</span>
            <span>{lastCheck.durationMs} ms</span>
            {lastCheck.httpStatus !== null && (
              <span>HTTP {lastCheck.httpStatus}</span>
            )}
          </>
        ) : (
          <span>No checks yet</span>
        )}
      </div>

      {health === "failing" && lastCheck && (
        <div className="mt-3 rounded bg-red-50 p-3 text-sm text-red-900">
          <p className="font-medium">{failureLabel(lastCheck.failureCode)}</p>
          {lastCheck.reason && (
            <p className="mt-0.5 break-words text-red-800">
              {lastCheck.reason}
            </p>
          )}
        </div>
      )}
    </li>
  );
}

export default function MonitorsPage() {
  const queryClient = useQueryClient();

  const { data, isPending, isError, error, refetch, dataUpdatedAt } = useQuery({
    queryKey: ["monitors"],
    queryFn: listMonitors,
    refetchInterval: 15_000,
  });

  const toggle = useMutation({
    mutationFn: (monitor: Monitor) =>
      monitor.enabled ? pauseMonitor(monitor.id) : resumeMonitor(monitor.id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["monitors"] });
    },
  });

  const sorted = data
    ? [...data].sort(
        (a, b) =>
          ORDER[healthOf(a)] - ORDER[healthOf(b)] ||
          a.monitor.name.localeCompare(b.monitor.name),
      )
    : [];

  const counts = { failing: 0, pending: 0, healthy: 0, paused: 0 };
  sorted.forEach((item) => {
    counts[healthOf(item)] += 1;
  });

  return (
    <div className="mx-auto max-w-5xl p-8">
      <div className="mb-6 flex flex-wrap items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-slate-900">Monitors</h1>
          <p className="text-sm text-slate-500">
            Real-time status and health across your configured endpoints.
          </p>
        </div>

        <Link
          to="/monitors/new"
          className="inline-flex items-center gap-2 rounded-lg bg-indigo-600 px-4 py-2 text-sm font-semibold text-white shadow-sm hover:bg-indigo-500 transition-colors"
        >
          <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" />
          </svg>
          Add Monitor
        </Link>
      </div>

      {isPending && <p className="text-slate-600">Loading monitors...</p>}

      {isError && (
        <div
          role="alert"
          className="rounded border border-red-200 bg-red-50 p-4 text-sm text-red-800"
        >
          <p>
            {error instanceof ApiError && error.status === 429
              ? `Too many requests. Try again in ${error.retryAfterSeconds ?? "a few"} seconds.`
              : "Could not load monitors."}
          </p>
          <button className="mt-2 underline" onClick={() => refetch()}>
            Try again
          </button>
        </div>
      )}

      {data && (
        <>
          <div className="grid grid-cols-2 gap-4 sm:grid-cols-4">
            <SummaryCard
              label="Failing"
              value={counts.failing}
              valueClass="text-red-600"
            />
            <SummaryCard
              label="Healthy"
              value={counts.healthy}
              valueClass="text-green-600"
            />
            <SummaryCard
              label="Waiting"
              value={counts.pending}
              valueClass="text-amber-600"
            />
            <SummaryCard
              label="Paused"
              value={counts.paused}
              valueClass="text-slate-600"
            />
          </div>

          <div className="mb-3 mt-8 flex items-baseline justify-between">
            <h2 className="text-base font-semibold text-slate-900">
              Active Monitors ({data.length})
            </h2>
            {dataUpdatedAt > 0 && (
              <p className="text-xs text-slate-500">
                Updated {timeAgo(new Date(dataUpdatedAt).toISOString())} (auto-refreshes every 15s)
              </p>
            )}
          </div>

          {data.length === 0 ? (
            <div className="rounded-lg bg-white p-10 text-center shadow-sm">
              <div className="mx-auto flex h-12 w-12 items-center justify-center rounded-full bg-indigo-50 text-indigo-600">
                <svg className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" />
                </svg>
              </div>
              <p className="mt-3 font-semibold text-slate-900">No monitors created yet</p>
              <p className="mt-1 text-sm text-slate-500">
                Get started by creating your first API endpoint to monitor.
              </p>
              <Link
                to="/monitors/new"
                className="mt-4 inline-block rounded-lg bg-indigo-600 px-4 py-2 text-sm font-semibold text-white shadow-sm hover:bg-indigo-500"
              >
                Create Monitor
              </Link>
            </div>
          ) : (
            <ul className="space-y-3">
              {sorted.map((item) => (
                <MonitorCard
                  key={item.monitor.id}
                  item={item}
                  busy={toggle.isPending}
                  onToggle={(monitor) => toggle.mutate(monitor)}
                />
              ))}
            </ul>
          )}
        </>
      )}
    </div>
  );
}
