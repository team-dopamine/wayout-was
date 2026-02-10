package kr.wayout.domain.submission;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import kr.wayout.domain.member.Member;
import kr.wayout.domain.problem.Problem;
import kr.wayout.global.common.BaseEntity;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

@Entity
@SQLRestriction("deleted_at IS NULL")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Submission extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "language", nullable = false, updatable = false)
    private Language language;

    @Column(name = "execution_time")
    private Double executionTime;

    @Column(name = "is_found", nullable = false)
    private Boolean isFound;

    @Column(name = "source_code", columnDefinition = "TEXT", nullable = false)
    private String sourceCode;

    @Column(name = "is_open", nullable = false)
    private Boolean isOpen = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "problem_id", nullable = false)
    private Problem problem;
}
