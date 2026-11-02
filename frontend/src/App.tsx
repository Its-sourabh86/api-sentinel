import { useEffect, useSyncExternalStore } from "react";
import { Navigate, Route, Routes } from "react-router-dom";
import LoginPage from "./pages/LoginPage";
import RegisterPage from "./pages/RegisterPage";
import MonitorsPage from "./pages/MonitorsPage";
import MonitorDetailPage from "./pages/MonitorDetailPage";
import CreateMonitorPage from "./pages/CreateMonitorPage";
import IncidentsPage from "./pages/IncidentsPage";
import AppLayout from "./components/AppLayout";
import { isLoggedIn, subscribe } from "./auth/tokenStore";
import { queryClient } from "./queryClient";

export default function App() {
  const loggedIn = useSyncExternalStore(subscribe, isLoggedIn);

  // Logout par cache saaf: agla user pichhle user ka data na dekh paye
  useEffect(() => {
    if (!loggedIn) {
      queryClient.clear();
    }
  }, [loggedIn]);

  return (
    <Routes>
      <Route
        path="/login"
        element={loggedIn ? <Navigate to="/" replace /> : <LoginPage />}
      />
      <Route
        path="/register"
        element={loggedIn ? <Navigate to="/" replace /> : <RegisterPage />}
      />

      {/* Protected routes wrapped in AppLayout with Sidebar */}
      <Route
        element={loggedIn ? <AppLayout /> : <Navigate to="/login" replace />}
      >
        <Route path="/" element={<MonitorsPage />} />
        <Route path="/incidents" element={<IncidentsPage />} />
        <Route path="/monitors/new" element={<CreateMonitorPage />} />
        <Route path="/monitors/:id" element={<MonitorDetailPage />} />
      </Route>

      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
