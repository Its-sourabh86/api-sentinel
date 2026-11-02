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

  const { data, isPending, isError, refetch } = useQuery({
    queryKey: ['all-incidents', statusParam, page],
    queryFn: () => listAllIncidents(statusParam, page, 15),
    refetchInterval: 15_000,
  });

  const incidents = data?.content ?? [];
  const totalPages = data?.totalPages ?? 1;

  function handleFilterChange(newFilter: StatusFilter) {
    setFilter(newFilter);
    setPage(0);
  }

  return (
    <div className="mx-auto max-w-5xl p-8">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-slate-900">Incidents</h1>
          <p className="text-sm text-slate-500">
            Track active and historical failures across all your monitors.
          </p>
        </div>

        <div className="flex rounded-lg bg-slate-200 p-1 text-sm font-medium">
          <button
            className={`rounded-md px-3.5 py-1.5 transition-colors ${
              filter === 'OPEN'
                ? 'bg-white text-slate-900 shadow-sm font-semibold'
                : 'text-slate-600 hover:text-slate-900'
            }`}
            onClick={() => handleFilterChange('OPEN')}
          >
            Open
          </button>
          <button
            className={`rounded-md px-3.5 py-1.5 transition-colors ${
              filter === 'RESOLVED'
                ? 'bg-white text-slate-900 shadow-sm font-semibold'
                : 'text-slate-600 hover:text-slate-900'
            }`}
            onClick={() => handleFilterChange('RESOLVED')}
          >
            Resolved
          </button>
          <button
            className={`rounded-md px-3.5 py-1.5 transition-colors ${
              filter === 'ALL'
                ? 'bg-white text-slate-900 shadow-sm font-semibold'
                : 'text-slate-600 hover:text-slate-900'
            }`}
            onClick={() => handleFilterChange('ALL')}
          >
            All
          </button>
        </div>
      </div>

      {isPending && <p className="mt-6 text-slate-600">Loading incidents...</p>}

      {isError && (
        <div
          role="alert"
          className="mt-6 rounded border border-red-200 bg-red-50 p-4 text-sm text-red-800"
        >
          <p>Could not load incidents.</p>
          <button className="mt-2 underline" onClick={() => refetch()}>
            Try again
          </button>
        </div>
      )}

      {data && incidents.length === 0 && (
        <div className="mt-6 rounded-lg bg-white p-10 text-center shadow-sm">
          <div className="mx-auto flex h-12 w-12 items-center justify-center rounded-full bg-emerald-50 text-emerald-600">
            <svg className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
            </svg>
          </div>
          <p className="mt-3 text-base font-semibold text-slate-800">
            {filter === 'OPEN'
              ? 'No active incidents'
              : 'No incidents found'}
          </p>
          <p className="mt-1 text-sm text-slate-500">
            {filter === 'OPEN'
              ? 'All your monitored APIs are running smoothly!'
              : 'There are no incident records matching this filter.'}
          </p>
        </div>
      )}

      {incidents.length > 0 && (
        <div className="mt-6 space-y-3">
          {incidents.map((incident) => (
            <div
              key={incident.id}
              className="rounded-lg bg-white p-5 shadow-sm transition-shadow hover:shadow-md"
            >
              <div className="flex flex-wrap items-center justify-between gap-2">
                <div className="flex items-center gap-2.5">
                  {incident.status === 'OPEN' ? (
                    <span className="inline-flex items-center gap-1.5 rounded-full bg-red-100 px-2.5 py-0.5 text-xs font-semibold text-red-800">
                      <span className="h-1.5 w-1.5 rounded-full bg-red-600 animate-pulse" />
                      Open
                    </span>
                  ) : (
                    <span className="inline-flex items-center gap-1.5 rounded-full bg-green-100 px-2.5 py-0.5 text-xs font-semibold text-green-800">
                      <span className="h-1.5 w-1.5 rounded-full bg-green-600" />
                      Resolved
                    </span>
                  )}
                  <span className="font-semibold text-slate-900">
                    {failureLabel(incident.failureCode)}
                  </span>
                </div>

                <Link
                  to={`/monitors/${incident.monitorId}`}
                  className="text-xs font-medium text-indigo-600 hover:text-indigo-800 hover:underline"
                >
                  View Monitor #{incident.monitorId} &rarr;
                </Link>
              </div>

              {incident.reason && (
                <p className="mt-2.5 break-words font-mono text-xs text-slate-700 bg-slate-50 p-3 rounded border border-slate-200">
                  {incident.reason}
                </p>
              )}

              <div className="mt-3 flex flex-wrap gap-x-6 gap-y-1 text-xs text-slate-500">
                <span>Failed checks: <strong className="text-slate-700">{incident.failureCount}</strong></span>
                <span>Opened: {formatDateTime(incident.openedAt)}</span>
                {incident.resolvedAt && (
                  <span>Resolved: {formatDateTime(incident.resolvedAt)}</span>
                )}
              </div>
            </div>
          ))}

          {totalPages > 1 && (
            <div className="mt-6 flex items-center justify-between pt-2">
              <button
                disabled={page <= 0}
                onClick={() => setPage((p) => Math.max(0, p - 1))}
                className="rounded border border-slate-300 bg-white px-3.5 py-1.5 text-sm font-medium text-slate-700 hover:bg-slate-50 disabled:opacity-50"
              >
                &larr; Previous
              </button>
              <span className="text-xs text-slate-500">
                Page {page + 1} of {totalPages}
              </span>
              <button
                disabled={page + 1 >= totalPages}
                onClick={() => setPage((p) => p + 1)}
                className="rounded border border-slate-300 bg-white px-3.5 py-1.5 text-sm font-medium text-slate-700 hover:bg-slate-50 disabled:opacity-50"
              >
                Next &rarr;
              </button>
            </div>
          )}
        </div>
      )}
    </div>
  );
}
