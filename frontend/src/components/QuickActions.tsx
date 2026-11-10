import { Link } from 'react-router';
import {
    ClipboardPlus,
    FilePlus2,
    CalendarDays,
} from 'lucide-react';

const actions = [
    {
        label: 'Nuova prestazione',
        path: '/prestazioni',
        icon: ClipboardPlus,
    },
    {
        label: 'Carica documento',
        path: '/documenti',
        icon: FilePlus2,
    },
    {
        label: 'Apri calendario',
        path: '/calendario',
        icon: CalendarDays,
    },
];

export default function QuickActions() {
    return (
        <div className="space-y-3">
            {actions.map((action) => (
                <Link
                    key={action.path}
                    to={action.path}
                    className="flex items-center gap-3 rounded-lg border border-slate-200 p-3 text-slate-700 transition-colors hover:border-sky-200 hover:bg-sky-50 hover:text-sky-700"
                >
                    <action.icon
                        size={20}
                        strokeWidth={1.8}
                        className="shrink-0"
                    />

                    <span className="text-sm font-medium">
                        {action.label}
                    </span>
                </Link>
            ))}
        </div>
    );
}