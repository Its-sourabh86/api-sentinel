import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { listAllIncidents } from '../api/incidents';
import { failureLabel } from '../lib/labels';
import { formatDateTime } from '../lib/time';

type StatusFilter = 'ALL' | 'OPEN' | 'RESOLVED';

export default function IncidentsPage() {
  const [filter, setFilter] = useState<StatusFilter>('OPEN');
  const [page, setPage] = useState(0);

  const statusParam = filter === 'ALL' ? undefined : filter;

  const { data, isPending, isError } = useQuery({
    queryKey: ['all-incidents', statusParam, page],
    queryFn: () => listAllIncidents(statusParam, page, 15),
    refetchInterval: 10_000,
  });

  const incidents = data?.content ?? [];
  const totalPages = data?.totalPages ?? 1;

  function handleFilterChange(newFilter: StatusFilter) {
    setFilter(newFilter);
    setPage(0);
  }

  return (
    <div className="min-h-screen bg-[#0B0F19] text-slate-100 p-6 lg:p-8 space-y-6">
      {/* Header & Tabs */}
      <div className="flex flex-wrap items-center justify-between gap-4 border-b border-slate-800/80 pb-5">
        <div>
          <div className="flex items-center gap-2">
            <span className="text-xs font-mono font-bold tracking-widest text-rose-400 uppercase">
              ▾ Alert Operations Center
            </span>
          </div>
          <h1 className="text-2xl font-black tracking-tight text-white font-mono mt-0.5">
            INCIDENT AUDIT LOG
          </h1>
          <p className="text-xs font-mono text-slate-500 mt-1">
            Real-time state machine transitions and threshold alerts
          </p>
        </div>

        {/* Filter Pills */}
        <div className="flex rounded-lg bg-[#111827] border border-slate-800 p-1 text-xs font-mono">
          <button
            className={`rounded-md px-3.5 py-1.5 transition-colors font-bold ${
              filter === 'OPEN'
                ? 'bg-rose-500/20 text-rose-400 border border-rose-500/30'
                : 'text-slate-400 hover:text-white'
            }`}
            onClick={() => handleFilterChange('OPEN')}
          >
            ● OPEN ALERTS
          </button>
          <button
            className={`rounded-md px-3.5 py-1.5 transition-colors font-bold ${
              filter === 'RESOLVED'
                ? 'bg-emerald-500/20 text-emerald-400 border border-emerald-500/30'
                : 'text-slate-400 hover:text-white'
            }`}
            onClick={() => handleFilterChange('RESOLVED')}
          >
            ✓ RESOLVED
          </button>
          <button
            className={`rounded-md px-3.5 py-1.5 transition-colors font-bold ${
              filter === 'ALL'
                ? 'bg-slate-800 text-white'
                : 'text-slate-400 hover:text-white'
            }`}
            onClick={() => handleFilterChange('ALL')}
          >
            ALL
          </button>
        </div>
      </div>

      {/* Incidents List */}
      <section className="space-y-3">
        {isPending && (
          <div className="rounded-xl bg-[#111827] border border-slate-800 p-12 text-center text-slate-500 font-mono text-xs">
            Querying active incident stream...
          </div>
        )}

        {isError && (
          <div className="rounded-xl bg-rose-950/20 border border-rose-800 p-6 text-rose-400 font-mono text-xs">
            Failed to query incident ledger from backend service.
          </div>
        )}

        {!isPending && !isError && incidents.length === 0 && (
          <div className="rounded-xl bg-[#111827] border border-slate-800 p-12 text-center text-slate-400 font-mono text-xs">
            {filter === 'OPEN'
              ? '✨ All systems optimal! No active incidents reported.'
              : 'No incidents match the selected filter criteria.'}
          </div>
        )}

        {incidents.map((inc) => (
          <div
            key={inc.id}
            className={`rounded-xl bg-[#111827] border p-5 shadow-xl font-mono transition-all hover:border-slate-700 ${
              inc.status === 'OPEN' ? 'border-rose-900/60 shadow-rose-950/20' : 'border-slate-800/80'
            }`}
          >
            <div className="flex flex-wrap items-center justify-between gap-3">
              <div className="flex items-center gap-3">
                <span
                  className={`rounded px-2.5 py-0.5 text-[10px] font-bold uppercase tracking-wider ${
                    inc.status === 'OPEN'
                      ? 'bg-rose-500/20 text-rose-400 border border-rose-500/40 animate-pulse'
                      : 'bg-emerald-500/20 text-emerald-400 border border-emerald-500/40'
                  }`}
                >
                  {inc.status}
                </span>

                <span className="font-bold text-white text-sm">
                  {failureLabel(inc.failureCode)}
                </span>

                <span className="text-xs text-rose-400/90 bg-rose-950/40 px-2 py-0.5 rounded border border-rose-900/40">
                  {inc.failureCount} sequential failures
                </span>
              </div>

              <Link
                to={`/monitors/${inc.monitorId}`}
                className="text-xs font-bold text-indigo-400 hover:text-indigo-300"
              >
                Inspect Target #{inc.monitorId} →
              </Link>
            </div>

            {inc.reason && (
              <p className="mt-3 rounded-lg bg-[#0E1422] border border-slate-800/90 p-3 text-xs text-rose-300/90 break-words">
                {inc.reason}
              </p>
            )}

            <div className="mt-3 flex flex-wrap items-center justify-between text-[11px] text-slate-500">
              <span>Opened: {formatDateTime(inc.openedAt)}</span>
              {inc.resolvedAt && (
                <span className="text-emerald-400">Resolved: {formatDateTime(inc.resolvedAt)}</span>
              )}
            </div>
          </div>
        ))}
      </section>

      {/* Pagination */}
      {totalPages > 1 && (
        <div className="flex items-center justify-between pt-4 font-mono text-xs text-slate-400">
          <span>Page {page + 1} of {totalPages}</span>
          <div className="flex gap-2">
            <button
              onClick={() => setPage((p) => Math.max(0, p - 1))}
              disabled={page === 0}
              className="rounded bg-slate-800 px-3 py-1.5 disabled:opacity-50"
            >
              Previous
            </button>
            <button
              onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
              disabled={page >= totalPages - 1}
              className="rounded bg-slate-800 px-3 py-1.5 disabled:opacity-50"
            >
              Next
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
