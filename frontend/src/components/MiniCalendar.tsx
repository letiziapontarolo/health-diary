import { useState } from 'react';
import { ChevronLeft, ChevronRight } from 'lucide-react';
import { categoryColors, localDateString, type MedicalEvent } from '../api/medicalEvents';
import { useMedicalEvents } from '../hooks/useMedicalEvents';

const weekdays = ['L', 'M', 'M', 'G', 'V', 'S', 'D'];

export default function MiniCalendar() {
  const [displayedDate, setDisplayedDate] = useState(() => new Date(new Date().getFullYear(), new Date().getMonth(), 1));
  const year = displayedDate.getFullYear();
  const month = displayedDate.getMonth();
  const fromDate = localDateString(new Date(year, month, 1));
  const toDate = localDateString(new Date(year, month + 1, 0));
  const { data: events = [], isPending, isError } = useMedicalEvents({ fromDate, toDate });
  const today = new Date();
  const start = (new Date(year, month, 1).getDay() + 6) % 7;
  const daysInMonth = new Date(year, month + 1, 0).getDate();
  const eventsByDay = new Map<number, MedicalEvent[]>();
  for (const event of events) {
    if (event.status === 'CANCELLED') continue;
    const day = Number(event.eventDate.slice(8, 10));
    eventsByDay.set(day, [...(eventsByDay.get(day) ?? []), event]);
  }

  return (
    <div className="w-full">
      <div className="mb-4 flex items-center justify-between">
        <button type="button" onClick={() => setDisplayedDate(new Date(year, month - 1, 1))} aria-label="Mese precedente" className="cursor-pointer rounded-lg p-2 text-slate-600 hover:bg-slate-100"><ChevronLeft size={20} /></button>
        <h3 className="font-semibold capitalize text-slate-900">{new Intl.DateTimeFormat('it-IT', { month: 'long', year: 'numeric' }).format(displayedDate)}</h3>
        <button type="button" onClick={() => setDisplayedDate(new Date(year, month + 1, 1))} aria-label="Mese successivo" className="cursor-pointer rounded-lg p-2 text-slate-600 hover:bg-slate-100"><ChevronRight size={20} /></button>
      </div>
      <div className="mb-3 flex justify-end">
        <button type="button" onClick={() => setDisplayedDate(new Date(today.getFullYear(), today.getMonth(), 1))} disabled={month === today.getMonth() && year === today.getFullYear()} className="cursor-pointer rounded-md px-3 py-1 text-xs font-medium text-sky-700 hover:bg-sky-50 disabled:cursor-default disabled:opacity-40">Oggi</button>
      </div>
      {isPending && <p role="status" className="mb-2 text-sm text-slate-500">Caricamento calendario...</p>}
      {isError && <p role="alert" className="mb-2 text-sm text-red-600">Impossibile caricare gli eventi del mese.</p>}
      <div className="grid grid-cols-7 gap-1 text-center">
        {weekdays.map((day, index) => <div key={index} className="py-2 text-xs font-medium text-slate-500">{day}</div>)}
        {Array.from({ length: start }, (_, index) => <div key={`empty-${index}`} />)}
        {Array.from({ length: daysInMonth }, (_, index) => {
          const day = index + 1;
          const isToday = day === today.getDate() && month === today.getMonth() && year === today.getFullYear();
          const dayEvents = eventsByDay.get(day) ?? [];
          const colors = [...new Set(dayEvents.map(event => categoryColors[event.category]))];
          return <div key={day} title={dayEvents.map(event => event.serviceName).join(', ') || undefined} className={`flex min-h-11 flex-col items-center justify-center rounded-lg text-sm ${isToday ? 'bg-sky-600 font-semibold text-white' : 'text-slate-700'}`}>
            <span>{day}</span>
            <div className="mt-1 flex gap-0.5" aria-label={dayEvents.length ? `${dayEvents.length} prestazioni` : undefined}>
              {colors.slice(0, 3).map(color => <span key={color} className={`h-1.5 w-1.5 rounded-full ${color}`} />)}
            </div>
          </div>;
        })}
      </div>
    </div>
  );
}
