package com.interviewai.repository;

import com.interviewai.domain.InterviewAnswer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InterviewAnswerRepository extends JpaRepository<InterviewAnswer, Long> {
    List<InterviewAnswer> findByQuestionSessionIdOrderByAnsweredAtAsc(Long sessionId);
}
