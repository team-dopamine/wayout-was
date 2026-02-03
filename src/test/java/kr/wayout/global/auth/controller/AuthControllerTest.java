package kr.wayout.global.auth.controller;

import kr.wayout.global.auth.AuthService;
import kr.wayout.global.auth.jwt.JwtProvider;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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