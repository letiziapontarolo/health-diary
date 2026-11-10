import { NavLink, Outlet } from 'react-router';
import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router';
import { useQueryClient } from '@tanstack/react-query';
import { logoutUser } from '../api/client';
import {
    LayoutDashboard,
    ClipboardList,
    Files,
    CalendarDays,
    UserRound,
    HeartPulse,
    X,
    Menu,
    LogOut
} from 'lucide-react';

const navigation = [
    { label: 'Dashboard', path: '/dashboard', icon: LayoutDashboard },
    { label: 'Prestazioni', path: '/prestazioni', icon: ClipboardList },
    { label: 'Documenti', path: '/documenti', icon: Files },
    { label: 'Calendario', path: '/calendario', icon: CalendarDays },
    { label: 'Profilo', path: '/profilo', icon: UserRound },
];

export default function AppLayout() {
    const [isSidebarOpen, setIsSidebarOpen] = useState(false);
    const navigate = useNavigate();
    const queryClient = useQueryClient();

    useEffect(() => {
        if (!isSidebarOpen) return;

        function handleKeyDown(event: KeyboardEvent) {
            if (event.key === 'Escape') {
                setIsSidebarOpen(false);
            }
        }

        window.addEventListener('keydown', handleKeyDown);

        return () => {
            window.removeEventListener('keydown', handleKeyDown);
        };
    }, [isSidebarOpen]);

    async function handleLogout() {
        try {
            await logoutUser();

            queryClient.setQueryData(['currentUser'], null);
            queryClient.removeQueries({
                queryKey: ['currentUser'],
                type: 'inactive',
            });

            navigate('/login', { replace: true });
        } catch (error) {
            console.error('Errore durante il logout:', error);
        }
    }

    return (
        <div className="flex min-h-screen bg-slate-50">
            {/* Pulsante menu mobile */}
            <button
                type="button"
                onClick={() => setIsSidebarOpen(true)}
                aria-label="Apri menu"
                className="fixed left-4 top-4 z-30 rounded-lg border border-slate-200 bg-white p-3 text-slate-700 shadow-sm md:hidden"
            >
                <Menu size={22} />
            </button>
            {/* Sidebar */}
            {isSidebarOpen && (
                <button
                    type="button"
                    aria-label="Chiudi menu"
                    onClick={() => setIsSidebarOpen(false)}
                    className="fixed inset-0 z-40 bg-black/40 md:hidden"
                />
            )}
            <aside
                className={`fixed inset-y-0 left-0 z-50 h-dvh w-64 overflow-y-auto border-r border-slate-200 bg-white p-6 transition-transform duration-200 md:sticky md:top-0 md:shrink-0 md:translate-x-0 ${isSidebarOpen ? 'translate-x-0' : '-translate-x-full'
                    }`}
            >
                <div className="flex items-center gap-3">
                    <div className="rounded-xl bg-sky-100 p-2 text-sky-700">
                        <HeartPulse size={24} />
                    </div>
                    <button
                        type="button"
                        onClick={() => setIsSidebarOpen(false)}
                        aria-label="Chiudi menu"
                        className="absolute right-4 top-4 rounded-lg p-2 text-slate-500 hover:bg-slate-100 md:hidden"
                    >
                        <X size={20} />
                    </button>

                    <h1 className="text-xl font-bold text-slate-900">
                        Health Diary
                    </h1>
                </div>

                <p className="mt-2 text-sm text-slate-500">
                    Il tuo diario personale
                </p>

                {/* Navigazione */}
                <nav aria-label="Navigazione principale" className="mt-10">
                    <ul className="space-y-2">
                        {navigation.map((item) => (
                            <li key={item.path}>
                                <NavLink
                                    to={item.path}
                                    onClick={() => setIsSidebarOpen(false)}
                                    className={({ isActive }) =>
                                        `block rounded-lg px-4 py-3 text-sm font-medium transition-colors ${isActive
                                            ? 'bg-sky-100 text-sky-800'
                                            : 'text-slate-600 hover:bg-slate-100 hover:text-slate-900'
                                        }`
                                    }
                                >
                                    <span className="flex items-center gap-3">
                                        <item.icon size={20} strokeWidth={1.8} />
                                        {item.label}
                                    </span>
                                </NavLink>
                            </li>
                        ))}
                    </ul>
                </nav>
                <div className="mt-4 border-t border-slate-200 pt-4">
                    <button
                        type="button"
                        onClick={handleLogout}
                        className="cursor-pointer flex w-full items-center gap-3 rounded-lg px-4 py-3 text-sm font-medium text-red-600 transition-colors hover:bg-red-50"
                    >
                        <LogOut size={20} strokeWidth={1.8} />
                        Esci
                    </button>
                </div>
            </aside>

            {/* Contenuto principale */}
            <main className="min-w-0 flex-1 px-4 pb-8 pt-20 md:p-8">
                <Outlet />
            </main>
        </div>
    );
}