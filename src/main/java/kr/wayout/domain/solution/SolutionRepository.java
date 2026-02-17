package kr.wayout.domain.solution;

import kr.wayout.domain.problem.Problem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SolutionRepository extends JpaRepository<Solution, Long> {

    Optional<Solution> findTopByProblemOrderByVersionDesc(Problem problem);
}
