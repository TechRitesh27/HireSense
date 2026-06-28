package com.p99softtraining.hiresense.repository;

import com.p99softtraining.hiresense.entity.SessionQuestionEval;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SessionQuestionEvalRepository extends JpaRepository<SessionQuestionEval, UUID> {

    Optional<SessionQuestionEval> findByInterviewSessionIdAndInterviewQuestionId(
            UUID sessionId, UUID questionId);

    List<SessionQuestionEval> findByInterviewSessionId(UUID sessionId);
}
