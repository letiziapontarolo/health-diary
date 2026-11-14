export type MedicalEventStatus = 'SCHEDULED' | 'COMPLETED' | 'CANCELLED';
export type MedicalEventCategory = 'LABORATORY' | 'SPECIALIST' | 'DIAGNOSTIC' | 'SURGICAL' | 'THERAPEUTIC';

export interface MedicalEvent {
  id: number;
  serviceId: number;
  serviceName: string;
  category: MedicalEventCategory;
  eventDate: string;
  eventTime: string | null;
  status: MedicalEventStatus;
  statusMode: string;
  doctorId: number | null;
  doctorName: string | null;
  facilityId: number | null;
  facilityName: string | null;
  notes: string | null;
  reportText: string | null;
  reportNotExpected: boolean;
  reportMissing: boolean;
  optionIds: number[];
}

export interface MedicalEventFilters {
  status?: MedicalEventStatus;
  fromDate?: string;
  toDate?: string;
}

export async function getMedicalEvents(filters: MedicalEventFilters = {}): Promise<MedicalEvent[]> {
  const params = new URLSearchParams();
  for (const [key, value] of Object.entries(filters)) {
    if (value !== undefined) params.set(key, value);
  }
  const query = params.toString();
  const response = await fetch(`/api/medical-events${query ? `?${query}` : ''}`, {
    credentials: 'include',
  });
  if (!response.ok) throw new Error('Impossibile caricare le prestazioni.');
  return response.json() as Promise<MedicalEvent[]>;
}

export function localDateString(date: Date): string {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

export function formatEventDate(date: string, time?: string | null): string {
  const [year, month, day] = date.split('-').map(Number);
  const label = new Intl.DateTimeFormat('it-IT', { day: 'numeric', month: 'short', year: 'numeric' })
    .format(new Date(year, month - 1, day));
  return time ? `${label} · ${time.slice(0, 5)}` : label;
}

export const categoryColors: Record<MedicalEventCategory, string> = {
  LABORATORY: 'bg-violet-500',
  SPECIALIST: 'bg-sky-500',
  DIAGNOSTIC: 'bg-amber-500',
  SURGICAL: 'bg-rose-500',
  THERAPEUTIC: 'bg-emerald-500',
};
