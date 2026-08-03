package com.interviewai.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "test_cases")
public class TestCase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "coding_test_id", nullable = false)
    private CodingTest codingTest;

    @Column(name = "input_data", columnDefinition = "text")
    private String inputData;

    @Column(name = "expected_output", nullable = false, columnDefinition = "text")
    private String expectedOutput;

    @Column(name = "is_hidden", nullable = false)
    private boolean hidden = false;

    @Column(name = "order_index", nullable = false)
    private int orderIndex;
}
