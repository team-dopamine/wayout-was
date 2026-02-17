package kr.wayout.domain.solution.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.wayout.domain.solution.SolutionService;
import kr.wayout.domain.solution.dto.SolutionDto;
import kr.wayout.domain.submission.Language;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SolutionController.class)
@AutoConfigureMockMvc(addFilters = false)
class SolutionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private SolutionService solutionService;

    @MockitoBean
    private JwtProvider jwtProvider;

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
    @DisplayName("정답 코드 등록 API - 성공")
    void create_success() throws Exception {
        // given
        String email = "test@gmail.com";
        setAuthentication(email);

        SolutionDto.CreateRequest request = new SolutionDto.CreateRequest(1L, Language.JAVA, "public class Main {}");
        SolutionDto.CreateResponse response = SolutionDto.CreateResponse.of("정답 코드 등록에 성공했습니다.");

        given(solutionService.create(eq(email), any(SolutionDto.CreateRequest.class)))
                .willReturn(response);

        // when & then
        mockMvc.perform(post("/solutions")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("정답 코드 등록에 성공했습니다."));
    }

    @Test
    @DisplayName("정답 코드 등록 API - 인증되지 않은 사용자")
    void create_unauthorized() throws Exception {
        // given
        SolutionDto.CreateRequest request = new SolutionDto.CreateRequest(1L, Language.JAVA, "public class Main {}");

        // when & then
        mockMvc.perform(post("/solutions")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
}
