import { Routes, Route } from 'react-router';

import HomePage from './pages/HomePage';
import LoginPage from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';
import DashboardPage from './pages/DashboardPage';
import ProtectedRoute from './components/ProtectedRoute';
import PublicOnlyRoute from './components/PublicOnlyRoute';
import AppLayout from './components/AppLayout';
import PlaceholderPage from './components/PlaceholderPage';

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
        <Route element={<AppLayout />}>
          <Route path="/dashboard" element={<DashboardPage />} />

          <Route
            path="/prestazioni"
            element={
              <PlaceholderPage
                title="Prestazioni"
                description="Qui potrai gestire analisi, visite e diagnostica."
              />
            }
          />

          <Route
            path="/documenti"
            element={
              <PlaceholderPage
                title="Documenti"
                description="Qui potrai organizzare e consultare i tuoi documenti."
              />
            }
          />

          <Route
            path="/calendario"
            element={
              <PlaceholderPage
                title="Calendario"
                description="Qui potrai visualizzare appuntamenti ed eventi."
              />
            }
          />

          <Route
            path="/profilo"
            element={
              <PlaceholderPage
                title="Profilo"
                description="Qui potrai gestire le informazioni del tuo account."
              />
            }
          />
        </Route>
      </Route>
    </Routes>
  );
}