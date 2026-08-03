package com.interviewai.service;

import com.interviewai.ai.AiCodeReviewService;
import com.interviewai.ai.CodeReviewResult;
import com.interviewai.common.enums.Difficulty;
import com.interviewai.common.enums.SubmissionStatus;
import com.interviewai.common.response.PageResponse;
import com.interviewai.common.util.PageableUtils;
import com.interviewai.domain.CodingSubmission;
import com.interviewai.domain.CodingTest;
import com.interviewai.domain.TestCase;
import com.interviewai.domain.User;
import com.interviewai.dto.request.CreateCodingTestRequest;
import com.interviewai.dto.request.SubmitCodeRequest;
import com.interviewai.dto.response.CodingTestResponse;
import com.interviewai.dto.response.MessageResponse;
import com.interviewai.dto.response.SubmissionResponse;
import com.interviewai.exception.ResourceNotFoundException;
import com.interviewai.repository.CodingSubmissionRepository;
import com.interviewai.repository.CodingTestRepository;
import com.interviewai.repository.TestCaseRepository;
import com.interviewai.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CodingService {

    private final CodingTestRepository codingTestRepository;
    private final TestCaseRepository testCaseRepository;
    private final CodingSubmissionRepository submissionRepository;
    private final UserRepository userRepository;
    private final GamificationService gamificationService;
    private final AiCodeReviewService aiCodeReviewService;

    @Transactional
    public CodingTestResponse createTest(CreateCodingTestRequest request, User creator) {
        CodingTest test = new CodingTest();
        test.setTitle(request.title());
        test.setDescription(request.description());
        test.setLanguage(request.language());
        test.setDifficulty(request.difficulty() != null ? request.difficulty() : Difficulty.MEDIUM);
        test.setTimeLimitSec(request.timeLimitSec() != null ? request.timeLimitSec() : 10);
        test.setMemoryLimitMb(request.memoryLimitMb() != null ? request.memoryLimitMb() : 256);
        test.setStarterCode(request.starterCode());
        test.setCreatedBy(creator);
        CodingTest saved = codingTestRepository.save(test);

        if (request.hiddenTestCases() != null) {
            int index = 0;
            for (String expected : request.hiddenTestCases()) {
                TestCase testCase = new TestCase();
                testCase.setCodingTest(saved);
                testCase.setExpectedOutput(expected);
                testCase.setHidden(index >= 0);
                testCase.setOrderIndex(index++);
                testCaseRepository.save(testCase);
            }
        }
        return CodingTestResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public PageResponse<CodingTestResponse> listTests(String language, String difficulty, int page, int size) {
        Pageable pageable = PageableUtils.build(page, size, "createdAt:desc");
        Page<CodingTest> tests;
        if (difficulty != null && !difficulty.isBlank()) {
            tests = codingTestRepository.findByDifficulty(Difficulty.valueOf(difficulty.toUpperCase()), pageable);
        } else if (language != null && !language.isBlank()) {
            tests = codingTestRepository.findByLanguage(language, pageable);
        } else {
            tests = codingTestRepository.findAll(pageable);
        }
        return PageResponse.from(tests, tests.stream().map(CodingTestResponse::from).toList());
    }

    @Transactional(readOnly = true)
    public CodingTestResponse getTest(Long id) {
        CodingTest test = codingTestRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("CodingTest", id));
        return CodingTestResponse.from(test);
    }

    @Transactional(readOnly = true)
    public List<TestCase> publicCases(Long id) {
        return testCaseRepository.findByCodingTestIdOrderByOrderIndexAsc(id).stream()
                .filter(c -> !c.isHidden())
                .toList();
    }

    @Transactional
    public SubmissionResponse submit(SubmitCodeRequest request, User candidate) {
        CodingTest test = codingTestRepository.findById(request.codingTestId())
                .orElseThrow(() -> ResourceNotFoundException.of("CodingTest", request.codingTestId()));

        List<TestCase> allCases = testCaseRepository.findByCodingTestIdOrderByOrderIndexAsc(test.getId());
        List<TestCase> publicCases = allCases.stream().filter(c -> !c.isHidden()).toList();

        Instant start = Instant.now();
        CodeReviewResult result = aiCodeReviewService.review(test, request.sourceCode(), publicCases);
        long latencyMs = Duration.between(start, Instant.now()).toMillis();

        CodingSubmission submission = new CodingSubmission();
        submission.setCandidate(candidate);
        submission.setCodingTest(test);
        submission.setLanguage(request.language());
        submission.setSourceCode(request.sourceCode());
        submission.setStatus(result.status());
        submission.setPassedCases(result.passed());
        submission.setTotalCases(allCases.size());
        submission.setExecutionTimeMs(latencyMs);
        submission.setMemoryUsedKb(8_192L);
        submission.setStdout(result.stdout());
        submission.setStderr(result.stderr());
        submission.setErrorMessage(result.error());
        submission.setCodeScore(result.codeScore());
        submission.setComplexityTime(result.complexityTime());
        submission.setComplexitySpace(result.complexitySpace());
        CodingSubmission saved = submissionRepository.save(submission);

        if (result.status() == SubmissionStatus.ACCEPTED) {
            gamificationService.awardXp(candidate.getId(), 50, "SOLVED_PROBLEM", saved.getId());
            gamificationService.updateStreak(candidate.getId());
        }
        return SubmissionResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<SubmissionResponse> history(Long candidateId, Long codingTestId) {
        if (codingTestId != null && codingTestId > 0) {
            return submissionRepository.findByCandidateIdAndCodingTestIdOrderBySubmittedAtDesc(candidateId, codingTestId)
                    .stream().map(SubmissionResponse::from).toList();
        }
        return submissionRepository.findByCandidateId(candidateId,
                        PageableUtils.build(0, 100, "submittedAt:desc"))
                .stream().map(SubmissionResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public SubmissionResponse getSubmission(UUID uuid) {
        CodingSubmission submission = submissionRepository.findByUuid(uuid)
                .orElseThrow(() -> ResourceNotFoundException.of("CodingSubmission", uuid));
        return SubmissionResponse.from(submission);
    }
}
