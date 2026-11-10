import { z } from 'zod'

export const registerSchema = z.object({
  firstName: z
    .string()
    .trim()
    .min(1, 'Inserisci il nome')
    .max(100, 'Il nome non può superare 100 caratteri'),

  lastName: z
    .string()
    .trim()
    .min(1, 'Inserisci il cognome')
    .max(100, 'Il cognome non può superare 100 caratteri'),

  email: z
    .email('Inserisci un indirizzo email valido')
    .max(255, 'L’email non può superare 255 caratteri'),

  password: z
    .string()
    .min(12, 'La password deve contenere almeno 12 caratteri')
    .max(128, 'La password non può superare 128 caratteri'),

  dateOfBirth: z
    .iso.date('Inserisci una data valida')
    .refine(
      (value) => value < new Date().toISOString().slice(0, 10),
      'La data di nascita deve essere nel passato',
    ),
})

export type RegisterFormValues = z.infer<typeof registerSchema>
