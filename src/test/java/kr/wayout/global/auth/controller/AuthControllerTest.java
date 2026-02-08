package kr.wayout.global.auth.controller;

import kr.wayout.global.auth.AuthService;
import kr.wayout.global.auth.dto.SignInDto;
import kr.wayout.global.auth.jwt.JwtProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean  // 변경
    private JwtProvider jwtProvider;

    private static final String TEST_EMAIL = "test@gmail.com";

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void setAuthentication(String email) {
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(email, null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @Test
    @DisplayName("로그인 확인 API - 로그인 상태")
    void check_success() throws Exception {
        // given
        String nickname = "testUser";
        setAuthentication(TEST_EMAIL);

        given(authService.check(TEST_EMAIL))
                .willReturn(SignInDto.CheckResponse.from(nickname));

        // when & then
        mockMvc.perform(get("/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nickname").value(nickname));
    }

    @Test
    @DisplayName("로그인 확인 API - 인증되지 않은 사용자")
    void check_unauthorized() throws Exception {
        // given - X

        // when & then
        mockMvc.perform(get("/auth/me"))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("회원 탈퇴 API - 성공")
    void withdraw_success() throws Exception {
        // given
        String email = "test@gmail.com";
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(email, null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        given(authService.withdraw(email))
                .willReturn("탈퇴 처리가 완료되었습니다.");

        // when & then
        mockMvc.perform(delete("/auth/withdraw"))
                .andExpect(status().isOk())
                .andExpect(content().string("탈퇴 처리가 완료되었습니다."));
    }
}