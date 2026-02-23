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
}
