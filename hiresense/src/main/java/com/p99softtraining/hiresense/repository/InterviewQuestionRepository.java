package com.p99softtraining.hiresense.repository;

import com.p99softtraining.hiresense.entity.InterviewQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface InterviewQuestionRepository extends JpaRepository<InterviewQuestion, UUID> {

    List<InterviewQuestion> findByInterviewSessionIdOrderByDifficultyLevel(UUID interviewSessionId);

    List<InterviewQuestion> findByInterviewSessionId(UUID interviewSessionId);
}
