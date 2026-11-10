import { useState } from 'react'
import { Link, useNavigate } from 'react-router'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { registerUser } from '../api/client'
import {
  registerSchema,
  type RegisterFormValues,
} from '../schemas/registerSchema'

export default function RegisterPage() {
  const navigate = useNavigate()
  const [serverError, setServerError] = useState<string | null>(null)

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<RegisterFormValues>({
    resolver: zodResolver(registerSchema),
  })

  async function onSubmit(data: RegisterFormValues) {
    setServerError(null)

    try {
      await registerUser(data)
      navigate('/login', { replace: true })
    } catch (error) {
      setServerError(
        error instanceof Error
          ? error.message
          : 'Si è verificato un errore durante la registrazione',
      )
    }
  }

  return (
    <main className="min-h-screen bg-slate-50 px-4 py-12">
      <div className="mx-auto max-w-md rounded-2xl bg-white p-8 shadow-sm">
        <h1 className="text-3xl font-bold text-slate-900">
          Crea il tuo account
        </h1>

        <p className="mt-2 text-sm text-slate-600">
          Inizia a organizzare le tue informazioni sanitarie
          con Health Diary.
        </p>

        <form
          onSubmit={handleSubmit(onSubmit)}
          noValidate
          className="mt-8 space-y-5"
        >
          <div>
            <label htmlFor="firstName" className="block text-sm font-medium">
              Nome
            </label>
            <input
              id="firstName"
              autoComplete="given-name"
              {...register('firstName')}
              className="mt-1 w-full rounded-lg border border-slate-300 p-3"
            />
            {errors.firstName && (
              <p className="mt-1 text-sm text-red-600">
                {errors.firstName.message}
              </p>
            )}
          </div>

          <div>
            <label htmlFor="lastName" className="block text-sm font-medium">
              Cognome
            </label>
            <input
              id="lastName"
              autoComplete="family-name"
              {...register('lastName')}
              className="mt-1 w-full rounded-lg border border-slate-300 p-3"
            />
            {errors.lastName && (
              <p className="mt-1 text-sm text-red-600">
                {errors.lastName.message}
              </p>
            )}
          </div>

          <div>
            <label htmlFor="email" className="block text-sm font-medium">
              Email
            </label>
            <input
              id="email"
              type="email"
              autoComplete="email"
              {...register('email')}
              className="mt-1 w-full rounded-lg border border-slate-300 p-3"
            />
            {errors.email && (
              <p className="mt-1 text-sm text-red-600">
                {errors.email.message}
              </p>
            )}
          </div>

          <div>
            <label htmlFor="password" className="block text-sm font-medium">
              Password
            </label>
            <input
              id="password"
              type="password"
              autoComplete="new-password"
              {...register('password')}
              className="mt-1 w-full rounded-lg border border-slate-300 p-3"
            />
            {errors.password && (
              <p className="mt-1 text-sm text-red-600">
                {errors.password.message}
              </p>
            )}
          </div>

          <div>
            <label htmlFor="dateOfBirth" className="block text-sm font-medium">
              Data di nascita
            </label>
            <input
              id="dateOfBirth"
              type="date"
              autoComplete="bday"
              {...register('dateOfBirth')}
              className="mt-1 w-full rounded-lg border border-slate-300 p-3"
            />
            {errors.dateOfBirth && (
              <p className="mt-1 text-sm text-red-600">
                {errors.dateOfBirth.message}
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
            className="w-full rounded-lg bg-blue-600 px-4 py-3 font-semibold text-white hover:bg-blue-700 disabled:opacity-50"
          >
            {isSubmitting ? 'Registrazione in corso...' : 'Registrati'}
          </button>
        </form>

        <p className="mt-6 text-center text-sm text-slate-600">
          Hai già un account?{' '}
          <Link to="/login" className="font-medium text-blue-600">
            Accedi
          </Link>
        </p>
      </div>
    </main>
  )
}
