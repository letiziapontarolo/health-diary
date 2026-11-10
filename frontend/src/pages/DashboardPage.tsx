import { useCurrentUser } from '../hooks/useCurrentUser';
import DashboardCard from '../components/DashboardCard';
import MiniCalendar from '../components/MiniCalendar';
import QuickActions from '../components/QuickActions';

export default function DashboardPage() {
    const { data: user } = useCurrentUser();

    return (
        <div>
            <header className="mb-8">
                <h1 className="text-3xl font-bold text-slate-900">
                    Bentornato, {user?.firstName}!
                </h1>

                <p className="mt-2 text-slate-600">
                    Ecco il riepilogo del tuo diario della salute.
                </p>
            </header>
            <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
                <DashboardCard title="Prossimi appuntamenti">
                    <p className="text-sm text-slate-500">
                        Nessun appuntamento programmato.
                    </p>
                </DashboardCard>

                <DashboardCard title="Terapie di oggi">
                    <p className="text-sm text-slate-500">
                        Nessuna terapia presente.
                    </p>
                </DashboardCard>
            </div>
            <div className="mt-6 grid grid-cols-1 items-start gap-6 lg:grid-cols-2">
                {/* Colonna sinistra: calendario */}
                <DashboardCard title="Calendario">
                    <MiniCalendar />
                </DashboardCard>

                {/* Colonna destra: widget impilati */}
                <div className="flex flex-col gap-6">
                    <DashboardCard title="Azioni rapide">
                        <QuickActions />
                    </DashboardCard>

                    <DashboardCard title="Attività recenti">
                        <p className="text-sm text-slate-500">
                            Nessuna attività recente.
                        </p>
                    </DashboardCard>
                </div>
            </div>
        </div>
    );
}