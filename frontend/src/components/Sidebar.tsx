import { Link, useLocation } from 'react-router-dom';
import { clearToken } from '../auth/tokenStore';

export default function Sidebar() {
  const location = useLocation();

  const isMonitors =
    location.pathname === '/' ||
    (location.pathname.startsWith('/monitors') && location.pathname !== '/monitors/new');
  const isIncidents = location.pathname.startsWith('/incidents');

  return (
    <aside className="flex h-screen w-64 flex-col border-r border-slate-800/80 bg-[#0E1422] text-slate-300 select-none">
      {/* Brand Header */}
      <div className="flex h-16 items-center gap-3 border-b border-slate-800/80 px-5">
        <div className="relative flex h-9 w-9 items-center justify-center rounded-lg bg-indigo-600/90 text-white shadow-lg shadow-indigo-500/20">
          <svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeWidth={2}
              d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z"
            />
          </svg>
          <span className="absolute -top-0.5 -right-0.5 flex h-2.5 w-2.5">
            <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-emerald-400 opacity-75"></span>
            <span className="relative inline-flex rounded-full h-2.5 w-2.5 bg-emerald-500"></span>
          </span>
        </div>
        <div>
          <span className="text-base font-bold tracking-tight text-white flex items-center gap-1.5">
            API Sentinel
            <span className="rounded bg-emerald-500/10 px-1.5 py-0.2 text-[9px] font-mono font-semibold text-emerald-400 border border-emerald-500/20">
              LIVE
            </span>
          </span>
          <span className="block text-[10px] font-medium uppercase tracking-wider text-slate-500">
            Observability Platform
          </span>
        </div>
      </div>

      {/* Quick Action */}
      <div className="p-4">
        <Link
          to="/monitors/new"
          className="flex w-full items-center justify-center gap-2 rounded-lg bg-indigo-600 hover:bg-indigo-500 px-4 py-2.5 text-xs font-bold uppercase tracking-wider text-white shadow-lg shadow-indigo-600/30 transition-all hover:scale-[1.01]"
        >
          <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2.5} d="M12 4v16m8-8H4" />
          </svg>
          + New Monitor
        </Link>
      </div>

      {/* Navigation Links */}
      <nav className="flex-1 space-y-1.5 px-3 py-2">
        <div className="px-3 pb-1 text-[10px] font-bold uppercase tracking-wider text-slate-500">
          Dashboards
        </div>

        <Link
          to="/"
          className={`flex items-center gap-3 rounded-lg px-3 py-2.5 text-xs font-semibold tracking-wide transition-all ${
            isMonitors
              ? 'bg-slate-800/90 text-white shadow-md border-l-2 border-indigo-500'
              : 'text-slate-400 hover:bg-slate-800/50 hover:text-slate-200'
          }`}
        >
          <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeWidth={2}
              d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z"
            />
          </svg>
          Overview & Telemetry
        </Link>

        <Link
          to="/incidents"
          className={`flex items-center gap-3 rounded-lg px-3 py-2.5 text-xs font-semibold tracking-wide transition-all ${
            isIncidents
              ? 'bg-slate-800/90 text-white shadow-md border-l-2 border-rose-500'
              : 'text-slate-400 hover:bg-slate-800/50 hover:text-slate-200'
          }`}
        >
          <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeWidth={2}
              d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"
            />
          </svg>
          Incident Alerts
        </Link>
      </nav>

      {/* User / Sign Out Footer */}
      <div className="border-t border-slate-800/80 p-4">
        <button
          onClick={clearToken}
          className="flex w-full items-center gap-3 rounded-lg px-3 py-2.5 text-xs font-semibold text-slate-400 hover:bg-rose-950/30 hover:text-rose-400 transition-colors"
        >
          <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeWidth={2}
              d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1"
            />
          </svg>
          Sign Out
        </button>
      </div>
    </aside>
  );
}
