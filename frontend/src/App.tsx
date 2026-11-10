import { Routes, Route } from 'react-router';

import HomePage from './pages/HomePage';
import LoginPage from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';
import DashboardPage from './pages/DashboardPage';
import ProtectedRoute from './components/ProtectedRoute';
import PublicOnlyRoute from './components/PublicOnlyRoute';

export default function App() {
  return (
    <Routes>
      {/* Homepage pubblica */}
      <Route path="/" element={<HomePage />} />

      {/* Pagine riservate agli utenti non autenticati */}
      <Route element={<PublicOnlyRoute />}>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />
      </Route>

      {/* Pagine riservate agli utenti autenticati */}
      <Route element={<ProtectedRoute />}>
        <Route path="/dashboard" element={<DashboardPage />} />
      </Route>
    </Routes>
  );
}