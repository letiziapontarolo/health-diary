import { useQuery } from '@tanstack/react-query';
import { getMedicalEvents, type MedicalEventFilters } from '../api/medicalEvents';

export function useMedicalEvents(filters: MedicalEventFilters = {}) {
  return useQuery({
    queryKey: ['medical-events', filters],
    queryFn: () => getMedicalEvents(filters),
  });
}
