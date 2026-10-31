import type { Member } from '../api/auth'
import { useOutletContext } from 'react-router'

function MyPage() {
    const member = useOutletContext<Member>()

    return (
        <main className="workspace account-workspace">
            <section className="panel account-panel">
                <h1>마이페이지</h1>
                <p>{member.name ?? '회원'}님</p>
                {member.email && <p>{member.email}</p>}
            </section>
        </main>
    )
}

export default MyPage
