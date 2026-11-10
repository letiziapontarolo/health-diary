import { useState } from 'react';
import { Link, useNavigate } from 'react-router';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';

import { loginUser } from '../api/client';
import { loginSchema, type LoginFormValues } from '../schemas/loginSchema';
import { useQueryClient } from '@tanstack/react-query';

export default function LoginPage() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [serverError, setServerError] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<LoginFormValues>({
    resolver: zodResolver(loginSchema),
    defaultValues: {
      email: '',
      password: '',
    },
  });

  async function onSubmit(data: LoginFormValues) {
    setServerError(null);

    try {
      const user = await loginUser(data);
      queryClient.setQueryData(['currentUser'], user);
      navigate('/dashboard', { replace: true });
    } catch (error) {
      setServerError(
        error instanceof Error
          ? error.message
          : 'Si è verificato un errore imprevisto.'
      );
    }
  }

  return (
    <main className="flex min-h-screen items-center justify-center bg-slate-50 px-4">
      <div className="w-full max-w-md rounded-2xl bg-white p-8 shadow-sm">
        <h1 className="mb-2 text-3xl font-bold text-slate-900">
          Accedi
        </h1>

        <p className="mb-8 text-slate-600">
          Accedi al tuo diario personale.
        </p>

        <form onSubmit={handleSubmit(onSubmit)} className="space-y-5">
          <div>
            <label
              htmlFor="email"
              className="mb-1 block text-sm font-medium text-slate-700"
            >
              Email
            </label>

            <input
              id="email"
              type="email"
              autoComplete="email"
              {...register('email')}
              className="w-full rounded-lg border border-slate-300 px-3 py-2"
            />

            {errors.email && (
              <p className="mt-1 text-sm text-red-600">
                {errors.email.message}
              </p>
            )}
          </div>

          <div>
            <label
              htmlFor="password"
              className="mb-1 block text-sm font-medium text-slate-700"
            >
              Password
            </label>

            <input
              id="password"
              type="password"
              autoComplete="current-password"
              {...register('password')}
              className="w-full rounded-lg border border-slate-300 px-3 py-2"
            />

            {errors.password && (
              <p className="mt-1 text-sm text-red-600">
                {errors.password.message}
              </p>
            )}
          </div>

          {serverError && (
            <p role="alert" className="text-sm text-red-600">
              {serverError}
            </p>
          )}

          <button
            type="submit"
            disabled={isSubmitting}
            className="w-full rounded-lg bg-slate-900 px-4 py-3 font-medium text-white disabled:opacity-50"
          >
            {isSubmitting ? 'Accesso in corso...' : 'Accedi'}
          </button>
        </form>

        <p className="mt-6 text-center text-sm text-slate-600">
          Non hai un account?{' '}
          <Link
            to="/register"
            className="font-medium text-slate-900 underline"
          >
            Registrati
          </Link>
        </p>
      </div>
    </main>
  );
}