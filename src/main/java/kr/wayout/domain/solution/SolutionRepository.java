package kr.wayout.domain.solution;

import kr.wayout.domain.problem.Problem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SolutionRepository extends JpaRepository<Solution, Long> {

    Optional<Solution> findTopByProblemOrderByVersionDesc(Problem problem);

    @Query(value = """
            select s
            from Solution s
            join fetch s.problem
            where s.member.id = :memberId
            """,
            countQuery = """
                    select count(s)
                    from Solution s
                    where s.member.id = :memberId
                    """)
    Page<Solution> findAllByMemberIdWithProblem(@Param("memberId") Long memberId, Pageable pageable);
}
