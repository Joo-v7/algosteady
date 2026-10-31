package com.algosteady.auth;

import com.algosteady.auth.service.OAuthLoginService;
import com.algosteady.judging.service.ExecutionWorker;
import com.algosteady.member.domain.Member;
import com.algosteady.member.repository.MemberRepository;
import com.algosteady.member.service.MemberCommandService;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** PostgreSQL 기반 OAuth 회원·세션·CSRF 검증 */
@SpringBootTest
@Transactional
@EnabledIfSystemProperty(named = "algosteady.postgres.tests", matches = "true")
class AuthPostgresTests {

    @MockitoBean
    private ExecutionWorker executionWorker;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private MemberCommandService memberCommandService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private OAuthLoginService oauthLoginService;

    @Autowired
    private JsonMapper jsonMapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    @DisplayName("최초 로그인은 회원을 생성하고 재로그인은 같은 회원의 프로필을 갱신합니다.")
    void login_success_reusesProviderIdentity() {
        // given
        Member first = memberCommandService.login("google", "subject-one", "same@example.com", "처음 이름");

        // when
        Member repeated = memberCommandService.login("google", "subject-one", "changed@example.com", "변경 이름");
        Member anotherProvider = memberCommandService.login("another-provider", "subject-one", "changed@example.com", "다른 제공자");
        Member anotherSubject = memberCommandService.login("google", "subject-two", "changed@example.com", "다른 사용자");

        // then
        Assertions.assertEquals(first.getId(), repeated.getId());
        Assertions.assertEquals("변경 이름", repeated.getName());
        Assertions.assertEquals("changed@example.com", repeated.getEmail());
        Assertions.assertNotEquals(repeated.getId(), anotherProvider.getId());
        Assertions.assertNotEquals(repeated.getId(), anotherSubject.getId());
    }

    @Test
    @DisplayName("OIDC 사용자를 회원과 연결하고 검증된 제공자 사용자 ID를 유지합니다.")
    void loadOidcUser_success_linksMember() {
        // given
        ClientRegistration registration = ClientRegistration.withRegistrationId("oidc-provider")
                .clientId("client")
                .clientSecret("secret")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("http://localhost/login/oauth2/code/oidc-provider")
                .scope("openid")
                .authorizationUri("http://localhost/authorize")
                .tokenUri("http://localhost/token")
                .jwkSetUri("http://localhost/jwks")
                .build();
        Instant now = Instant.now();
        OidcIdToken idToken = new OidcIdToken("id-token", now, now.plusSeconds(60), Map.of("sub", "provider-subject", "email", "member@example.com", "name", "OAuth 회원"));
        OAuth2AccessToken accessToken = new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, "access-token", now, now.plusSeconds(60), Set.of("openid"));

        // when
        OidcUser user = oauthLoginService.loadOidcUser(new OidcUserRequest(registration, accessToken, idToken));

        // then
        Member member = memberRepository.findByProviderAndProviderSubject("oidc-provider", "provider-subject").orElseThrow();
        Assertions.assertEquals(member.getId().toString(), user.getName());
        Assertions.assertEquals("provider-subject", user.getSubject());
        Assertions.assertEquals(idToken, user.getIdToken());
    }

    @Test
    @DisplayName("일반 OAuth2 제공자도 사용자 ID 설정으로 회원과 연결합니다.")
    void loadUser_success_withOAuth2Provider() throws Exception {
        // given
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/userinfo", exchange -> {
            byte[] body = "{\"uid\":\"oauth-subject\",\"email\":\"oauth@example.com\",\"name\":\"OAuth 회원\"}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json;charset=UTF-8");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        try {
            ClientRegistration registration = ClientRegistration.withRegistrationId("oauth-provider")
                    .clientId("client")
                    .clientSecret("secret")
                    .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                    .redirectUri("http://localhost/login/oauth2/code/oauth-provider")
                    .authorizationUri("http://localhost/authorize")
                    .tokenUri("http://localhost/token")
                    .userInfoUri("http://127.0.0.1:" + server.getAddress().getPort() + "/userinfo")
                    .userNameAttributeName("uid")
                    .build();
            OAuth2AccessToken token = new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, "access-token", Instant.now(), Instant.now().plusSeconds(60));

            // when
            OAuth2User user = oauthLoginService.loadUser(new OAuth2UserRequest(registration, token));

            // then
            Member member = memberRepository.findByProviderAndProviderSubject("oauth-provider", "oauth-subject").orElseThrow();
            Assertions.assertEquals(member.getId().toString(), user.getName());
            Assertions.assertEquals("OAuth 회원", member.getName());
        } finally {
            server.stop(0);
        }
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @DisplayName("같은 제공자 계정으로 동시에 처음 로그인해도 회원 하나만 생성합니다.")
    void login_success_withConcurrentRequests() throws Exception {
        // given
        String subject = UUID.randomUUID().toString();
        CountDownLatch start = new CountDownLatch(1);
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            Future<UUID> first = executor.submit(() -> {
                start.await();
                return memberCommandService.login("google", subject, "concurrent@example.com", "동시 로그인").getId();
            });
            Future<UUID> second = executor.submit(() -> {
                start.await();
                return memberCommandService.login("google", subject, "concurrent@example.com", "동시 로그인").getId();
            });

            // when
            start.countDown();

            // then
            Assertions.assertEquals(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS));
        } finally {
            memberRepository.findByProviderAndProviderSubject("google", subject)
                    .ifPresent(memberRepository::delete);
        }
    }

    @Test
    @DisplayName("공개 조회와 로그인 수단은 허용하고 비로그인 회원 조회는 JSON 401로 응답합니다.")
    void findMember_fail_withoutLogin() throws Exception {
        // when / then
        mockMvc.perform(get("/api/exercises/categories")).andExpect(status().isOk());
        mockMvc.perform(get("/api/auth/providers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("google"))
                .andExpect(jsonPath("$[0].authorizationUrl").value("/oauth2/authorization/google"));
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("로그인이 필요합니다."));
    }

    @Test
    @DisplayName("세션의 회원을 조회하고 CSRF 검증 후 로그아웃하면 세션을 폐기합니다.")
    void logout_success_invalidatesSession() throws Exception {
        // given
        Member member = memberCommandService.login("google", "session-subject", "session@example.com", "세션 회원");
        DefaultOAuth2User principal = new DefaultOAuth2User(List.of(new SimpleGrantedAuthority("OIDC_USER")), Map.of("member_id", member.getId().toString()), "member_id");
        MockHttpSession session = new MockHttpSession();
        mockMvc.perform(get("/api/auth/me").session(session).with(oauth2Login().oauth2User(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(member.getId().toString()));
        MvcResult csrfResult = mockMvc.perform(get("/api/auth/csrf").session(session))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode csrf = jsonMapper.readTree(csrfResult.getResponse().getContentAsString());

        // when
        mockMvc.perform(post("/api/auth/logout").session(session).header(csrf.path("headerName").asString(), csrf.path("token").asString()))
                .andExpect(status().isNoContent());

        // then
        Assertions.assertTrue(session.isInvalid());
        mockMvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("CSRF 토큰이 없는 변경 요청과 검증되지 않은 OAuth 콜백을 거부합니다.")
    void authenticate_fail_withInvalidRequests() throws Exception {
        // when / then
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").exists());
        mockMvc.perform(post("/api/runs").contentType("application/json").content("{}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/login/oauth2/code/google").param("code", "untrusted").param("state", UUID.randomUUID().toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost:5173/login?error=oauth"));
    }
}
