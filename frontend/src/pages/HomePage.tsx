import { Link } from 'react-router';
import { useCurrentUser } from '../hooks/useCurrentUser';
import { useState } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { logoutUser } from '../api/client';

export default function HomePage() {
  const { data: user, isLoading, isError } = useCurrentUser();
  const queryClient = useQueryClient();
  const [isLoggingOut, setIsLoggingOut] = useState(false);
  const [logoutError, setLogoutError] = useState<string | null>(null);

  async function handleLogout() {
    setIsLoggingOut(true);
    setLogoutError(null);

    try {
      await logoutUser();
      queryClient.setQueryData(['currentUser'], null);
      queryClient.removeQueries({
        queryKey: ['currentUser'],
        type: 'inactive',
      });
    } catch (error) {
      setLogoutError(
        error instanceof Error
          ? error.message
          : 'Si è verificato un errore imprevisto.'
      );
    } finally {
      setIsLoggingOut(false);
    }
  }

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

  return (
    <main className="min-h-screen bg-slate-50 p-8">
      <div className="mx-auto max-w-3xl">
        <h1 className="mb-6 text-3xl font-bold text-slate-900">
          Health Diary
        </h1>

        {user ? (
          <div className="rounded-xl bg-white p-6 shadow-sm">
            <h2 className="mb-2 text-xl font-semibold">
              Bentornato, {user.firstName}! 🩵
            </h2>

            <p className="text-slate-600">
              Hai effettuato l'accesso con {user.email}.
            </p>

            {logoutError && (
              <p role="alert" className="mt-4 text-sm text-red-600">
                {logoutError}
              </p>
            )}

            <button
              type="button"
              onClick={handleLogout}
              disabled={isLoggingOut}
              className="mt-6 rounded-lg bg-slate-900 px-4 py-2 text-white disabled:opacity-50"
            >
              {isLoggingOut ? 'Uscita in corso...' : 'Esci'}
            </button>
          </div>
        ) : (
          <div className="rounded-xl bg-white p-6 shadow-sm">
            <p className="mb-4 text-slate-600">
              Accedi o registrati per utilizzare il tuo diario personale.
            </p>

            <div className="flex gap-4">
              <Link
                to="/login"
                className="rounded-lg bg-slate-900 px-4 py-2 text-white"
              >
                Accedi
              </Link>

              <Link
                to="/register"
                className="rounded-lg border border-slate-300 px-4 py-2"
              >
                Registrati
              </Link>
            </div>
          </div>
        )}
      </div>
    </main>
  );
}