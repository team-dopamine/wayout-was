package kr.wayout.domain.submission;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import kr.wayout.domain.problem.Problem;
import kr.wayout.domain.user.User;
import kr.wayout.global.common.BaseEntity;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Submission extends BaseEntity {

    @Column(name = "language", nullable = false, updatable = false)
    private Language language;

    @Column(name = "execution_time")
    private Double executionTime;

    @Column(name = "is_found", nullable = false)
    private Boolean isFound;

    @Column(name = "source_code", columnDefinition = "TEXT", nullable = false)
    private String sourceCode;

    @Column(name = "is_open", columnDefinition = "BOOLEAN DEFAULT TRUE")
    private Boolean isOpen;

    @ManyToOne(fetch = FetchType.LAZY)
    @Column(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @Column(name = "problem_id", nullable = false)
    private Problem problem;
}
