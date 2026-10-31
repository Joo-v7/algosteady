package com.algosteady.member.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/** OAuth 제공자 계정과 연결된 회원 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "member_account")
public class Member {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String provider;

    @Column(nullable = false)
    private String providerSubject;

    private String email;

    private String name;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant lastLoginAt;
}
