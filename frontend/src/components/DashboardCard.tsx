import type { ReactNode } from 'react';

interface DashboardCardProps {
  title: string;
  children: ReactNode;
}

export default function DashboardCard({
  title,
  children,
}: DashboardCardProps) {
  return (
    <section className="rounded-xl border border-slate-200 bg-white p-6 shadow-sm">
      <h2 className="mb-4 text-lg font-semibold text-slate-900">
        {title}
      </h2>

      {children}
    </section>
  );
}