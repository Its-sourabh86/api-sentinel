import { Outlet } from 'react-router-dom';
import Sidebar from './Sidebar';

export default function AppLayout() {
  return (
    <div className="flex h-screen overflow-hidden bg-[#0B0F19] text-slate-100 font-sans selection:bg-indigo-500 selection:text-white">
      <Sidebar />
      <div className="flex-1 overflow-y-auto bg-[#0B0F19]">
        <Outlet />
      </div>
    </div>
  );
}
