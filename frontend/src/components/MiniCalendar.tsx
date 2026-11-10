import { useState } from 'react';
import { ChevronLeft, ChevronRight } from 'lucide-react';

const weekdays = ['L', 'M', 'M', 'G', 'V', 'S', 'D'];

export default function MiniCalendar() {
    const today = new Date();

    const [displayedDate, setDisplayedDate] = useState(
        () => new Date(today.getFullYear(), today.getMonth(), 1)
    );

    const year = displayedDate.getFullYear();
    const month = displayedDate.getMonth();

    function changeMonth(offset: number) {
        setDisplayedDate(
            (current) =>
                new Date(current.getFullYear(), current.getMonth() + offset, 1)
        );
    }

    function goToToday() {
        const currentDate = new Date();

        setDisplayedDate(
            new Date(currentDate.getFullYear(), currentDate.getMonth(), 1)
        );
    }

    const firstDay = new Date(year, month, 1);
    const daysInMonth = new Date(year, month + 1, 0).getDate();

    const startingDay = (firstDay.getDay() + 6) % 7;

    const days = Array.from(
        { length: daysInMonth },
        (_, index) => index + 1
    );

    const monthLabel = new Intl.DateTimeFormat('it-IT', {
        month: 'long',
        year: 'numeric',
    }).format(displayedDate);

    return (
        <div className="w-full">
            <div className="mb-4 flex items-center justify-between">
                <button
                    type="button"
                    onClick={() => changeMonth(-1)}
                    aria-label="Mese precedente"
                    className="cursor-pointer rounded-lg p-2 text-slate-600 hover:bg-slate-100"
                >
                    <ChevronLeft size={20} />
                </button>

                <h3 className="font-semibold capitalize text-slate-900">
                    {monthLabel}
                </h3>

                <button
                    type="button"
                    onClick={() => changeMonth(1)}
                    aria-label="Mese successivo"
                    className="cursor-pointer rounded-lg p-2 text-slate-600 hover:bg-slate-100"
                >
                    <ChevronRight size={20} />
                </button>
            </div>

            <div className="mb-3 flex justify-end">
                <button
                    type="button"
                    onClick={goToToday}
                    disabled={
                        month === today.getMonth() &&
                        year === today.getFullYear()
                    }
                    className="cursor-pointer rounded-md px-3 py-1 text-xs font-medium text-sky-700 hover:bg-sky-50 disabled:cursor-default disabled:opacity-40"
                >
                    Oggi
                </button>
            </div>

            <div className="grid grid-cols-7 gap-1 text-center">
                {weekdays.map((day, index) => (
                    <div
                        key={index}
                        className="py-2 text-xs font-medium text-slate-500"
                    >
                        {day}
                    </div>
                ))}

                {Array.from({ length: startingDay }, (_, index) => (
                    <div key={`empty-${index}`} />
                ))}

                {days.map((day) => {
                    const isToday =
                        day === today.getDate() &&
                        month === today.getMonth() &&
                        year === today.getFullYear();

                    return (
                        <div
                            key={day}
                            className={`flex h-9 items-center justify-center rounded-lg text-sm ${isToday
                                ? 'bg-sky-600 font-semibold text-white'
                                : 'text-slate-700'
                                }`}
                        >
                            {day}
                        </div>
                    );
                })}
            </div>
        </div>
    );
}