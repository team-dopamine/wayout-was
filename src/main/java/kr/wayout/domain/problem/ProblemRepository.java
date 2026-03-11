package kr.wayout.domain.problem;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProblemRepository extends JpaRepository<Problem, Long> {

    Page<Problem> findAll(Pageable pageable);

    @Query("""
            SELECT p
            FROM Problem p
            WHERE (:problemNo IS NULL OR p.problemNo = :problemNo)
              AND (:titleKeyword IS NULL OR LOWER(p.title) LIKE LOWER(CONCAT('%', :titleKeyword, '%')))
            """)
    List<Problem> search(@Param("problemNo") Integer problemNo,
                         @Param("titleKeyword") String titleKeyword,
                         Pageable pageable);

    Problem findProblemById(Long id);

}
