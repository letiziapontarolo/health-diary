import { useQuery } from '@tanstack/react-query';
import { getCurrentUser } from '../api/client';

export function useCurrentUser() {
  return useQuery({
    queryKey: ['currentUser'],
    queryFn: getCurrentUser,
    retry: false,
    staleTime: 0,
    refetchOnMount: 'always',
  });
}