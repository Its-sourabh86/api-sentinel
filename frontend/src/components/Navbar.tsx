import { Link, useLocation } from 'react-router-dom';
import { clearToken } from '../auth/tokenStore';

export default function Navbar() {
  const location = useLocation();

  const isMonitors = location.pathname === '/' || location.pathname.startsWith('/monitors');
  const isIncidents = location.pathname.startsWith('/incidents');

  return (
    <header className="bg-slate-900 px-6 py-4">
      <div className="mx-auto flex max-w-4xl items-center justify-between">
        <div className="flex items-center gap-6">
          <Link to="/" className="text-xl font-bold tracking-tight text-white">
            API Sentinel
          </Link>
          <nav className="flex items-center gap-4 text-sm font-medium">
            <Link
              to="/"
              className={isMonitors && !isIncidents ? 'text-white' : 'text-slate-400 hover:text-slate-200'}
            >
              Monitors
            </Link>
            <Link
              to="/incidents"
              className={isIncidents ? 'text-white' : 'text-slate-400 hover:text-slate-200'}
            >
              Incidents
            </Link>
          </nav>
        </div>
        <button
          className="rounded border border-slate-600 px-3 py-1.5 text-sm text-slate-100 hover:bg-slate-800"
          onClick={clearToken}
        >
          Sign out
        </button>
      </div>
    </header>
  );
}

