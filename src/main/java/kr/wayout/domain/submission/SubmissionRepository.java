package kr.wayout.domain.submission;

import kr.wayout.domain.problem.Problem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubmissionRepository extends JpaRepository<Submission, Long> {

    public interface SubmissionCountRow {
        Long getProblemId();

        long getTotalSubmissions();

        long getFoundSubmissions();
    }

    @Query("""
                select
                    s.problem.id as problemId,
                    count(s.id) as totalSubmissions,
                    sum(case when s.isFound = true then 1 else 0 end) as foundSubmissions
                from Submission s
                where s.problem.id in (:problemIds)
                group by s.problem.id
            """)
    List<SubmissionCountRow> countByProblemIds(@Param("problemIds") List<Long> problemIds);

    Page<Submission> findAll(Pageable pageable);

    Page<Submission> findAllByProblem(Pageable pageable, Problem problem);
}
