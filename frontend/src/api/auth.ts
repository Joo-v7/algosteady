import { ApiError, requestApi } from './exercises'

export interface Member {
    id: string
    name: string | null
    email: string | null
}

export interface OAuthProvider {
    id: string
    name: string
    authorizationUrl: string
}

export async function getCurrentMember(signal: AbortSignal): Promise<Member | null> {
    try {
        return await requestApi<Member>('/api/auth/me', { signal })
    } catch (error) {
        if (error instanceof ApiError && error.status === 401) {
            return null
        }
        throw error
    }
}

export function getOAuthProviders(signal: AbortSignal) {
    return requestApi<OAuthProvider[]>('/api/auth/providers', { signal })
}

export function logout() {
    return requestApi<void>('/api/auth/logout', { method: 'POST' })
}
