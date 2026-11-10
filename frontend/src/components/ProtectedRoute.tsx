import { Navigate, Outlet } from 'react-router';
import { useCurrentUser } from '../hooks/useCurrentUser';

export default function ProtectedRoute() {
  const { data: user, isLoading, isError } = useCurrentUser();

  if (isLoading) {
    return <p className="p-8">Caricamento...</p>;
  }

  if (isError) {
    return (
      <p className="p-8 text-red-600">
        Impossibile verificare la sessione. Riprova più tardi.
      </p>
    );
  }

  if (!user) {
    return <Navigate to="/login" replace />;
  }

  return <Outlet />;
}