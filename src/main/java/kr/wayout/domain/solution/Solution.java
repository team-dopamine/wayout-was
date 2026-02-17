package kr.wayout.domain.solution;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import kr.wayout.domain.member.Member;
import kr.wayout.domain.problem.Problem;
import kr.wayout.domain.submission.Language;
import kr.wayout.global.common.BaseEntity;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Getter
@SQLRestriction("deleted_at IS NULL")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Solution extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "problem_id")
    private Problem problem;

    @Column(name = "version", nullable = false)
    private Integer version;

    @Column(name = "source_code", columnDefinition = "TEXT", nullable = false)
    private String sourceCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "language", nullable = false, updatable = false)
    private Language language;

    @Builder
    public Solution(Member member, Problem problem, Integer version, String sourceCode, Language language) {
        this.member = member;
        this.problem = problem;
        this.version = version;
        this.sourceCode = sourceCode;
        this.language = language;
    }

    public static Solution create(Member member, Problem problem, Integer version, String sourceCode, Language language) {
        return Solution.builder()
                .member(member)
                .problem(problem)
                .version(version)
                .sourceCode(sourceCode)
                .language(language)
                .build();
    }

}
