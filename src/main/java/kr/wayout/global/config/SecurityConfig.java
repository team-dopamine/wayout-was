package kr.wayout.global.config;

import jakarta.servlet.http.Cookie;
import kr.wayout.global.auth.CustomOAuth2UserService;
import kr.wayout.global.auth.handler.CustomLogoutSuccessHandler;
import kr.wayout.global.auth.handler.OAuth2SuccessHandler;
import kr.wayout.global.auth.jwt.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final OAuth2SuccessHandler oAuth2SuccessHandler;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final CustomLogoutSuccessHandler customLogoutSuccessHandler;

    @Value("${app.cookie-domain}")
    private String cookieDomain;

    @Value("${cors.allowed-origins}")
    private String allowedOrigins;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity httpSecurity) throws Exception {
        httpSecurity
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .authorizeHttpRequests(auth -> auth
                        // TODO: 추후 회원 기능이 구현 될 경우, 해당 경로에 대해서 인증 설정
                        .requestMatchers("/api/auth/me", "/api/auth/sign-out", "/api/auth/withdraw").authenticated()
                        .requestMatchers("/api/members/me", "/api/members/me/nickname").authenticated()
                        .requestMatchers("/api/solutions/me").authenticated()
                        .requestMatchers("/api/testcases").authenticated()
                        // 우선적으로는 모두 허용
                        .anyRequest().permitAll()
                )
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(customOAuth2UserService)
                        )
                        // TODO: SuccessUrl은 추후 온보딩 페이지 경로로 설정
                        .successHandler(oAuth2SuccessHandler)
                        .failureUrl("/auth/error")
                )
                .logout(logout -> logout
                                .logoutUrl("/auth/sign-out")
                                .addLogoutHandler((request, response, authentication) -> {

                                    Cookie cookie = new Cookie("JSESSIONID", null);
                                    cookie.setMaxAge(0);
                                    cookie.setDomain(cookieDomain);
                                    cookie.setPath("/api");
                                    cookie.setHttpOnly(true);
//                            cookie.setSecure(true);

                                    response.addCookie(cookie);
                                })
                                .logoutSuccessHandler(customLogoutSuccessHandler)
                )

                .addFilterBefore(jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class);

        return httpSecurity.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(Arrays.asList(allowedOrigins.split(",")));
        configuration.addAllowedMethod("*");
        configuration.addAllowedHeader("*");
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

}
