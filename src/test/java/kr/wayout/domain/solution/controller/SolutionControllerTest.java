package kr.wayout.domain.solution.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.wayout.domain.problem.Platform;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

        SolutionDto.CreateRequest request = new SolutionDto.CreateRequest(1L, Language.JAVA, true, "public class Main {}");
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
        SolutionDto.CreateRequest request = new SolutionDto.CreateRequest(1L, Language.JAVA, true, "public class Main {}");

        // when & then
        mockMvc.perform(post("/solutions")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("내 정답 코드 기여 조회 API - 성공")
    void listContributions_success() throws Exception {
        // given
        String email = "test@gmail.com";
        setAuthentication(email);

        SolutionDto.ContributionResponse item = SolutionDto.ContributionResponse.builder()
                .problemId(1L)
                .problemNo(1000)
                .platform(Platform.SWEA)
                .problemTitle("A+B")
                .language(Language.JAVA)
                .submissionDate(LocalDateTime.of(2026, 3, 17, 12, 0))
                .build();

        PageRequest pageable = PageRequest.of(0, 8);
        given(solutionService.listContributions(any(), any()))
                .willReturn(new PageImpl<>(List.of(item), pageable, 1));

        // when & then
        mockMvc.perform(get("/solutions/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].problemId").value(1))
                .andExpect(jsonPath("$.content[0].problemNo").value(1000))
                .andExpect(jsonPath("$.content[0].platform").value("SWEA"))
                .andExpect(jsonPath("$.content[0].problemTitle").value("A+B"))
                .andExpect(jsonPath("$.content[0].language").value("JAVA"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("내 정답 코드 기여 조회 API - 인증되지 않은 사용자")
    void listContributions_unauthorized() throws Exception {
        // when & then
        mockMvc.perform(get("/solutions/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("내 정답 코드 기여 상세 조회 API - 성공")
    void detailMyContribution_success() throws Exception {
        // given
        String email = "test@gmail.com";
        Long solutionId = 11L;
        setAuthentication(email);

        SolutionDto.MyContributionDetailResponse response = SolutionDto.MyContributionDetailResponse.builder()
                .id(solutionId)
                .problemId(1L)
                .problemNo(1000)
                .title("A+B")
                .sourceCode("public class Main {}")
                .language(Language.JAVA)
                .contributionDate(LocalDateTime.of(2026, 4, 26, 1, 15))
                .isOpen(true)
                .build();

        given(solutionService.detailMyContribution(email, solutionId)).willReturn(response);

        // when & then
        mockMvc.perform(get("/solutions/me/{solutionId}", solutionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(11))
                .andExpect(jsonPath("$.problemId").value(1))
                .andExpect(jsonPath("$.problemNo").value(1000))
                .andExpect(jsonPath("$.title").value("A+B"))
                .andExpect(jsonPath("$.sourceCode").value("public class Main {}"))
                .andExpect(jsonPath("$.language").value("JAVA"))
                .andExpect(jsonPath("$.isOpen").value(true));
    }

    @Test
    @DisplayName("내 정답 코드 기여 상세 조회 API - 인증되지 않은 사용자")
    void detailMyContribution_unauthorized() throws Exception {
        // when & then
        mockMvc.perform(get("/solutions/me/{solutionId}", 11L))
                .andExpect(status().isUnauthorized());
    }
}
