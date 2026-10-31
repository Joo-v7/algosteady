package com.algosteady.member.repository;

import com.algosteady.member.domain.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

/** OAuth 회원 등록 및 조회 */
public interface MemberRepository extends JpaRepository<Member, UUID> {

    /**
     * OAuth 제공자와 제공자 사용자 ID로 회원 조회
     * @param provider OAuth 등록 ID
     * @param providerSubject 제공자의 고유 사용자 ID
     * @return 연결된 회원
     */
    Optional<Member> findByProviderAndProviderSubject(String provider, String providerSubject);

    /**
     * 최초 로그인 회원 생성 또는 기존 회원의 프로필·로그인 시각 갱신
     * @param id 최초 등록에 사용할 회원 ID
     * @param provider OAuth 등록 ID
     * @param providerSubject 제공자의 고유 사용자 ID
     * @param email 제공자에서 조회한 이메일
     * @param name 제공자에서 조회한 이름
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            INSERT INTO member_account (id, provider, provider_subject, email, name, created_at, last_login_at)
            VALUES (:id, :provider, :providerSubject, :email, :name, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            ON CONFLICT (provider, provider_subject)
            DO UPDATE SET email = EXCLUDED.email
                        , name = EXCLUDED.name
                        , last_login_at = CURRENT_TIMESTAMP
            """, nativeQuery = true)
    void saveLogin(@Param("id") UUID id, @Param("provider") String provider, @Param("providerSubject") String providerSubject, @Param("email") String email, @Param("name") String name);
}
