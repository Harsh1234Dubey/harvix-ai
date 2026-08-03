package com.interviewai.repository;

import com.interviewai.common.enums.Difficulty;
import com.interviewai.domain.Question;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface QuestionRepository extends JpaRepository<Question, Long> {

    Page<Question> findByTopic(String topic, Pageable pageable);

    Page<Question> findByTopicAndDifficulty(String topic, Difficulty difficulty, Pageable pageable);

    long countByTopic(String topic);

    @Query("select q from Question q where lower(q.question) like lower(concat('%', :q, '%')) " +
            "or lower(q.topic) like lower(concat('%', :q, '%'))")
    Page<Question> search(@Param("q") String q, Pageable pageable);
}
