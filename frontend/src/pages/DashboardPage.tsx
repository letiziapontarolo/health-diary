export default function DashboardPage() {
  return (
    <main className="min-h-screen bg-slate-50 p-8">
      <h1 className="text-3xl font-bold text-slate-900">
        Dashboard
      </h1>

      <p className="mt-4 text-slate-600">
        Questa pagina è accessibile soltanto agli utenti autenticati.
      </p>
    </main>
  );
}