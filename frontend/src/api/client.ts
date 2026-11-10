export interface CsrfToken {
    headerName: string
    parameterName: string
    token: string
}

export async function getCsrfToken(): Promise<CsrfToken> {
    const response = await fetch('/api/auth/csrf', {
        credentials: 'include',
    })

    if (!response.ok) {
        throw new Error('Impossibile ottenere il token CSRF')
    }

    return response.json() as Promise<CsrfToken>
}


export interface RegisterRequest {
    firstName: string
    lastName: string
    email: string
    password: string
    dateOfBirth: string
}

export interface RegisterResponse {
    id: number
    firstName: string
    lastName: string
    email: string
}

export interface LoginRequest {
    email: string;
    password: string;
}

export interface LoginResponse {
    id: number;
    firstName: string;
    lastName: string;
    email: string;
}

export async function registerUser(
    data: RegisterRequest,
): Promise<RegisterResponse> {
    const csrf = await getCsrfToken()

    const response = await fetch('/api/auth/register', {
        method: 'POST',
        credentials: 'include',
        headers: {
            'Content-Type': 'application/json',
            [csrf.headerName]: csrf.token,
        },
        body: JSON.stringify(data),
    })

    if (response.status === 409) {
        throw new Error('Esiste già un account con questa email.')
    }

    if (response.status === 400) {
        throw new Error('Controlla i dati inseriti e riprova.')
    }

    if (!response.ok) {
        throw new Error('Registrazione non riuscita. Riprova più tardi.')
    }

    return response.json() as Promise<RegisterResponse>
}

export async function loginUser(
    data: LoginRequest
): Promise<LoginResponse> {
    const csrf = await getCsrfToken();

    const response = await fetch('/api/auth/login', {
        method: 'POST',
        credentials: 'include',
        headers: {
            'Content-Type': 'application/json',
            [csrf.headerName]: csrf.token,
        },
        body: JSON.stringify(data),
    });

    if (response.status === 401) {
        throw new Error('Email o password non corretti.');
    }

    if (response.status === 400) {
        throw new Error('Controlla i dati inseriti.');
    }

    if (!response.ok) {
        throw new Error('Accesso non riuscito. Riprova più tardi.');
    }

    return response.json() as Promise<LoginResponse>;
}

export async function getCurrentUser(): Promise<LoginResponse | null> {
    const response = await fetch('/api/auth/me', {
        credentials: 'include',
    });

    if (response.status === 401) {
        return null;
    }

    if (!response.ok) {
        throw new Error('Impossibile recuperare l’utente autenticato.');
    }

    return response.json() as Promise<LoginResponse>;
}

export async function logoutUser(): Promise<void> {
    const csrf = await getCsrfToken();

    const response = await fetch('/api/auth/logout', {
        method: 'POST',
        credentials: 'include',
        headers: {
            [csrf.headerName]: csrf.token,
        },
    });

    if (!response.ok) {
        throw new Error('Impossibile effettuare il logout. Riprova.');
    }
}
