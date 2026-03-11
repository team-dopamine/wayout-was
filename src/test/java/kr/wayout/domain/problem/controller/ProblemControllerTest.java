package kr.wayout.domain.problem.controller;

import kr.wayout.domain.problem.Platform;
import kr.wayout.domain.problem.ProblemService;
import kr.wayout.domain.problem.dto.ProblemDto;
import kr.wayout.global.auth.jwt.JwtProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProblemController.class)
@AutoConfigureMockMvc(addFilters = false)
class ProblemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtProvider jwtProvider;

    @MockitoBean
    private ProblemService problemService;

    @Test
    @DisplayName("문제 목록 조회 API - 페이지 형태로 문제 목록을 반환한다")
    void list_success() throws Exception {
        // given
        ProblemDto.List item = new ProblemDto.List(
                1L, 1000, "A+B", Platform.SWEA, 12L, 3L
        );
        PageRequest pageable = PageRequest.of(0, 8);
        given(problemService.list(org.mockito.ArgumentMatchers.any()))
                .willReturn(new PageImpl<>(List.of(item), pageable, 1));

        // when & then
        mockMvc.perform(get("/problems"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(jsonPath("$.content[0].problemId").value(1))
                .andExpect(jsonPath("$.content[0].problemNo").value(1000))
                .andExpect(jsonPath("$.content[0].title").value("A+B"))
                .andExpect(jsonPath("$.content[0].platform").value("SWEA"))
                .andExpect(jsonPath("$.content[0].totalSubmissions").value(12))
                .andExpect(jsonPath("$.content[0].foundSubmissions").value(3))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("문제 제목 검색 API - 키워드와 limit으로 목록을 반환한다")
    void search_success() throws Exception {
        // given
        ProblemDto.Search item = new ProblemDto.Search(2L, 1001, "A-B", Platform.SWEA);
        given(problemService.search(eq("AB"), eq(10))).willReturn(List.of(item));

        // when & then
        mockMvc.perform(get("/problems/search")
                        .param("keyword", "AB")
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(jsonPath("$[0].problemId").value(2))
                .andExpect(jsonPath("$[0].problemNo").value(1001))
                .andExpect(jsonPath("$[0].title").value("A-B"))
                .andExpect(jsonPath("$[0].platform").value("SWEA"));
    }

    @Test
    @DisplayName("문제 검색 API - 문제 번호 키워드로도 목록을 반환한다")
    void search_by_problem_no_success() throws Exception {
        // given
        ProblemDto.Search item = new ProblemDto.Search(10L, 1000, "A+B", Platform.SWEA);
        given(problemService.search(eq("1000"), eq(10))).willReturn(List.of(item));

        // when & then
        mockMvc.perform(get("/problems/search")
                        .param("keyword", "1000"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(jsonPath("$[0].problemId").value(10))
                .andExpect(jsonPath("$[0].problemNo").value(1000))
                .andExpect(jsonPath("$[0].title").value("A+B"));
    }

    @Test
    @DisplayName("문제 검색 API - 서비스에서 limit 오류를 던지면 400을 반환한다")
    void search_bad_request_when_limit_invalid() throws Exception {
        // given
        given(problemService.search(eq("AB"), eq(0)))
                .willThrow(new IllegalArgumentException("limit은 1 이상이어야 합니다."));

        // when & then
        mockMvc.perform(get("/problems/search")
                        .param("keyword", "AB")
                        .param("limit", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("V001"))
                .andExpect(jsonPath("$.message").value("limit은 1 이상이어야 합니다."));
    }
}
