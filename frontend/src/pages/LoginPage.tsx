import { useEffect, useState } from 'react'
import { useSearchParams } from 'react-router'
import { getOAuthProviders } from '../api/auth'
import type { OAuthProvider } from '../api/auth'
import googleLogo from '../assets/google-g.png'

function LoginPage() {
    const [searchParams] = useSearchParams()
    const [providers, setProviders] = useState<OAuthProvider[] | null>(null)
    const [error, setError] = useState('')

    useEffect(() => {
        const controller = new AbortController()

        getOAuthProviders(controller.signal)
            .then(result => {
                if (!controller.signal.aborted) {
                    setProviders(result)
                }
            })
            .catch(error => {
                if (!controller.signal.aborted) {
                    setError(error.message)
                }
            })

        return () => controller.abort()
    }, [])

    return (
        <main className="workspace account-workspace">
            <section className="panel account-panel login-panel">
                <h1>로그인</h1>
                {searchParams.has('error') && <p role="alert">로그인하지 못했습니다. 다시 시도해 주세요.</p>}
                {error && <p role="alert">{error}</p>}
                {!error && providers === null && <p role="status">로그인 수단을 불러오는 중입니다.</p>}
                {providers?.length === 0 && <p>사용 가능한 로그인 수단이 없습니다.</p>}
                <div className="oauth-providers">
                    {providers?.map(provider => (
                        <a className={provider.id === 'google' ? 'oauth-login google-login' : 'secondary-button oauth-login'} href={provider.authorizationUrl} key={provider.id}>
                            {provider.id === 'google' && <img src={googleLogo} alt="" width="20" height="20" />}
                            <span>{provider.id === 'google' ? 'Google 계정으로 로그인' : `${provider.name}로 계속하기`}</span>
                        </a>
                    ))}
                </div>
            </section>
        </main>
    )
}

export default LoginPage
