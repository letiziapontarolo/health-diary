import { useCurrentUser } from '../hooks/useCurrentUser';
import DashboardCard from '../components/DashboardCard';
import MiniCalendar from '../components/MiniCalendar';
import QuickActions from '../components/QuickActions';
import { formatEventDate, localDateString, type MedicalEvent } from '../api/medicalEvents';
import { useMedicalEvents } from '../hooks/useMedicalEvents';

function EventList({ events }: { events: MedicalEvent[] }) {
  return <ul className="divide-y divide-slate-100">
    {events.map(event => <li key={event.id} className="py-3 first:pt-0 last:pb-0">
      <p className="text-sm font-medium text-slate-800">{event.serviceName}</p>
      <p className="mt-1 text-xs text-slate-500">{formatEventDate(event.eventDate, event.eventTime)}</p>
    </li>)}
  </ul>;
}

export default function DashboardPage() {
  const { data: user } = useCurrentUser();
  const { data: events, isPending, isError } = useMedicalEvents();
  const today = localDateString(new Date());
  const nowTime = new Date().toTimeString().slice(0, 5);
  const upcoming = (events ?? [])
    .filter(event => event.status === 'SCHEDULED' && (event.eventDate > today || (event.eventDate === today && (!event.eventTime || event.eventTime.slice(0, 5) >= nowTime))))
    .sort((a, b) => a.eventDate.localeCompare(b.eventDate) || (a.eventTime ?? '').localeCompare(b.eventTime ?? ''))
    .slice(0, 5);
  const recent = (events ?? [])
    .filter(event => event.status === 'COMPLETED')
    .sort((a, b) => b.eventDate.localeCompare(a.eventDate) || (b.eventTime ?? '').localeCompare(a.eventTime ?? '') || b.id - a.id)
    .slice(0, 5);

  function renderEvents(items: MedicalEvent[], emptyText: string) {
    if (isPending) return <p role="status" className="text-sm text-slate-500">Caricamento prestazioni...</p>;
    if (isError) return <p role="alert" className="text-sm text-red-600">Impossibile caricare le prestazioni. Riprova più tardi.</p>;
    if (!items.length) return <p className="text-sm text-slate-500">{emptyText}</p>;
    return <EventList events={items} />;
  }

  return <div>
    <header className="mb-8">
      <h1 className="text-3xl font-bold text-slate-900">Bentornato, {user?.firstName}!</h1>
      <p className="mt-2 text-slate-600">Ecco il riepilogo del tuo diario della salute.</p>
    </header>
    <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
      <DashboardCard title="Prossimi appuntamenti">{renderEvents(upcoming, 'Nessun appuntamento programmato.')}</DashboardCard>
      <DashboardCard title="Terapie di oggi"><p className="text-sm text-slate-500">Le terapie saranno disponibili prossimamente.</p></DashboardCard>
    </div>
    <div className="mt-6 grid grid-cols-1 items-start gap-6 lg:grid-cols-2">
      <DashboardCard title="Calendario"><MiniCalendar /></DashboardCard>
      <div className="flex flex-col gap-6">
        <DashboardCard title="Azioni rapide"><QuickActions /></DashboardCard>
        <DashboardCard title="Attività recenti">{renderEvents(recent, 'Nessuna prestazione completata.')}</DashboardCard>
      </div>
    </div>
  </div>;
}
