package com.interviewai.config.seed;

import com.interviewai.common.enums.Difficulty;
import com.interviewai.common.enums.EmploymentType;
import com.interviewai.common.enums.JobStatus;
import com.interviewai.common.enums.QuestionType;
import com.interviewai.common.enums.UserRole;
import com.interviewai.common.enums.UserStatus;
import com.interviewai.common.enums.WorkMode;
import com.interviewai.domain.Achievement;
import com.interviewai.domain.CodingTest;
import com.interviewai.domain.Company;
import com.interviewai.domain.CompanyMember;
import com.interviewai.domain.Job;
import com.interviewai.domain.JobSkill;
import com.interviewai.domain.Permission;
import com.interviewai.domain.Question;
import com.interviewai.domain.Role;
import com.interviewai.domain.Skill;
import com.interviewai.domain.TestCase;
import com.interviewai.domain.User;
import com.interviewai.repository.AchievementRepository;
import com.interviewai.repository.CodingTestRepository;
import com.interviewai.repository.CompanyMemberRepository;
import com.interviewai.repository.CompanyRepository;
import com.interviewai.repository.JobRepository;
import com.interviewai.repository.JobSkillRepository;
import com.interviewai.repository.PermissionRepository;
import com.interviewai.repository.QuestionRepository;
import com.interviewai.repository.RoleRepository;
import com.interviewai.repository.SkillRepository;
import com.interviewai.repository.TestCaseRepository;
import com.interviewai.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final UserRepository userRepository;
    private final SkillRepository skillRepository;
    private final AchievementRepository achievementRepository;
    private final QuestionRepository questionRepository;
    private final CodingTestRepository codingTestRepository;
    private final TestCaseRepository testCaseRepository;
    private final CompanyRepository companyRepository;
    private final CompanyMemberRepository companyMemberRepository;
    private final JobRepository jobRepository;
    private final JobSkillRepository jobSkillRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seed.enabled:true}")
    private boolean enabled;

    @Override
    @Transactional
    public void run(String... args) {
        if (!enabled) {
            log.info("DataSeeder disabled by configuration.");
            return;
        }
        if (roleRepository.count() > 0) {
            log.info("Database already seeded, skipping.");
            return;
        }
        log.info("Seeding initial data...");

        Set<Role> roles = seedRoles();
        seedPermissions();
        User admin = seedUser("System", "Admin", "admin@interviewai.com", "Admin@123", UserRole.ADMIN, roles);
        User recruiter = seedUser("Harsh", "Dubey", "recruiter@interviewai.com", "Recruiter@123", UserRole.RECRUITER, roles);
        seedUser("Priya", "Verma", "candidate@interviewai.com", "Candidate@123", UserRole.CANDIDATE, roles);

        Map<String, Skill> skills = seedSkills();
        seedAchievements();
        seedQuestions();
        CodingTest twoSum = seedCodingTest(admin);
        seedCompanies(recruiter, skills);
        log.info("DataSeeder finished. Admin login: admin@interviewai.com / Admin@123");
    }

    private Set<Role> seedRoles() {
        Role admin = role("ADMIN", "Administrator", "Full platform access");
        Role recruiter = role("RECRUITER", "Recruiter", "Company and jobs management");
        Role candidate = role("CANDIDATE", "Candidate", "Job seeking and interviews");
        return Set.of(admin, recruiter, candidate);
    }

    private Role role(String code, String name, String description) {
        Role role = new Role();
        role.setCode(code);
        role.setName(name);
        role.setDescription(description);
        roleRepository.save(role);
        return role;
    }

    private void seedPermissions() {
        List<String[]> perms = List.of(
                new String[]{"user:read", "Read user", "users", "read"},
                new String[]{"user:write", "Write user", "users", "write"},
                new String[]{"job:manage", "Manage jobs", "jobs", "manage"},
                new String[]{"company:manage", "Manage companies", "companies", "manage"},
                new String[]{"interview:manage", "Manage interviews", "interviews", "manage"},
                new String[]{"report:read", "Read reports", "reports", "read"},
                new String[]{"admin:all", "Admin access", "admin", "all"});
        for (String[] p : perms) {
            Permission permission = new Permission();
            permission.setCode(p[0]);
            permission.setName(p[1]);
            permission.setResource(p[2]);
            permission.setAction(p[3]);
            permissionRepository.save(permission);
        }
    }

    private User seedUser(String first, String last, String email, String password,
                          UserRole role, Set<Role> allRoles) {
        User user = new User();
        user.setFirstName(first);
        user.setLastName(last);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setStatus(UserStatus.ACTIVE);
        user.setEmailVerified(true);
        Role roleEntity = allRoles.stream()
                .filter(r -> r.getCode().equals(role.name()))
                .findFirst().orElseThrow();
        user.setRoles(Set.of(roleEntity));
        userRepository.save(user);
        return user;
    }

    private Map<String, Skill> seedSkills() {
        List<String[]> skills = List.of(
                new String[]{"Java", "Backend"},
                new String[]{"Spring Boot", "Backend"},
                new String[]{"Python", "Backend"},
                new String[]{"JavaScript", "Frontend"},
                new String[]{"TypeScript", "Frontend"},
                new String[]{"React", "Frontend"},
                new String[]{"SQL", "Database"},
                new String[]{"PostgreSQL", "Database"},
                new String[]{"Docker", "DevOps"},
                new String[]{"Kubernetes", "DevOps"},
                new String[]{"System Design", "Architecture"},
                new String[]{"Machine Learning", "AI/ML"});
        for (String[] s : skills) {
            Skill skill = new Skill();
            skill.setName(s[0]);
            skill.setCategory(s[1]);
            skillRepository.save(skill);
        }
        return Map.ofEntries(
                Map.entry("Java", skillRepository.findByNameIgnoreCase("Java").orElseThrow()),
                Map.entry("Spring Boot", skillRepository.findByNameIgnoreCase("Spring Boot").orElseThrow()),
                Map.entry("Python", skillRepository.findByNameIgnoreCase("Python").orElseThrow()),
                Map.entry("JavaScript", skillRepository.findByNameIgnoreCase("JavaScript").orElseThrow()),
                Map.entry("TypeScript", skillRepository.findByNameIgnoreCase("TypeScript").orElseThrow()),
                Map.entry("React", skillRepository.findByNameIgnoreCase("React").orElseThrow()),
                Map.entry("SQL", skillRepository.findByNameIgnoreCase("SQL").orElseThrow()),
                Map.entry("PostgreSQL", skillRepository.findByNameIgnoreCase("PostgreSQL").orElseThrow()),
                Map.entry("Docker", skillRepository.findByNameIgnoreCase("Docker").orElseThrow()),
                Map.entry("Kubernetes", skillRepository.findByNameIgnoreCase("Kubernetes").orElseThrow()),
                Map.entry("System Design", skillRepository.findByNameIgnoreCase("System Design").orElseThrow()),
                Map.entry("Machine Learning", skillRepository.findByNameIgnoreCase("Machine Learning").orElseThrow()));
    }

    private void seedAchievements() {
        List<Object[]> achievements = List.of(
                new Object[]{"first_signin", "First Sign In", "Complete your first login", 10},
                new Object[]{"profile_complete", "Profile Pro", "Fill in 100% of your profile", 25},
                new Object[]{"first_interview", "First Interview", "Complete your first AI interview", 50},
                new Object[]{"five_interviews", "Interview Veteran", "Complete 5 interviews", 150},
                new Object[]{"first_code_accepted", "Clean Compile", "Get a coding submission accepted", 40},
                new Object[]{"seven_day_streak", "Consistency King", "Maintain a 7-day practice streak", 200});
        for (Object[] a : achievements) {
            Achievement achievement = new Achievement();
            achievement.setCode((String) a[0]);
            achievement.setName((String) a[1]);
            achievement.setDescription((String) a[2]);
            achievement.setXpReward((Integer) a[3]);
            achievementRepository.save(achievement);
        }
    }

    private void seedQuestions() {
        List<Object[]> questions = List.of(
                new Object[]{"Algorithms", "Arrays", "Explain how two-pointer technique works and give an example.", "Two pointers move toward each other or in tandem to reduce time complexity, e.g. finding a pair summing to target in a sorted array.", Difficulty.EASY, QuestionType.TEXT},
                new Object[]{"Algorithms", "Sorting", "What is the worst-case time complexity of quicksort and how can it be avoided?", "O(n^2) worst case; avoided with randomized pivot or median-of-three.", Difficulty.MEDIUM, QuestionType.TEXT},
                new Object[]{"Algorithms", "Dynamic Programming", "Explain the difference between memoization and tabulation.", "Memoization is top-down recursion with caching; tabulation is bottom-up iterative filling of a table.", Difficulty.MEDIUM, QuestionType.TEXT},
                new Object[]{"Data Structures", "Hash Tables", "How is a hash collision resolved in separate chaining?", "Each bucket holds a linked list or tree of entries sharing the same hash.", Difficulty.EASY, QuestionType.TEXT},
                new Object[]{"Data Structures", "Trees", "When would you prefer an AVL tree over a binary search tree?", "When guaranteed O(log n) lookups are required, since AVL rebalances on insert/delete.", Difficulty.MEDIUM, QuestionType.TEXT},
                new Object[]{"System Design", "Design", "Design a URL shortener. Mention key components and storage choices.", "Hash generation, write-through to DB, cache with TTL, redirect 302, analytics pipeline.", Difficulty.HARD, QuestionType.SYSTEM_DESIGN},
                new Object[]{"System Design", "Design", "How would you design a rate limiter for a public API?", "Sliding window counter in Redis; return 429 with Retry-After when exhausted.", Difficulty.HARD, QuestionType.SYSTEM_DESIGN},
                new Object[]{"Databases", "SQL", "Explain the difference between an index scan and a sequential scan.", "Index scan uses an index to locate rows efficiently; sequential scan reads the whole table.", Difficulty.MEDIUM, QuestionType.TEXT},
                new Object[]{"Databases", "SQL", "When does a database query use a full table scan despite an index existing?", "Low selectivity, small tables, or functions on indexed columns prevent index usage.", Difficulty.MEDIUM, QuestionType.TEXT},
                new Object[]{"Concurrency", "Threads", "What is a deadlock and what are the four necessary conditions?", "Mutual exclusion, hold-and-wait, no preemption, circular wait.", Difficulty.MEDIUM, QuestionType.TEXT},
                new Object[]{"Java", "Core", "What is the difference between an abstract class and an interface in modern Java?", "Interfaces support default/static methods and multiple inheritance of type; abstract classes can hold state and constructors.", Difficulty.EASY, QuestionType.TEXT},
                new Object[]{"Java", "Core", "What is a race condition and how do synchronized blocks prevent it?", "Two threads mutate shared state unpredictably; synchronization serializes access via monitor locks.", Difficulty.EASY, QuestionType.TEXT},
                new Object[]{"Java", "Core", "What is the purpose of the volatile keyword?", "Ensures visibility of a variable across threads without locking; prevents compiler caching of the value.", Difficulty.EASY, QuestionType.TEXT},
                new Object[]{"Networking", "HTTP", "What is idempotency and which HTTP methods are idempotent?", "Repeated identical requests have the same effect as a single request; GET, PUT, DELETE are idempotent.", Difficulty.EASY, QuestionType.TEXT},
                new Object[]{"Networking", "HTTP", "Explain the differences between HTTP/1.1 and HTTP/2.", "HTTP/2 adds multiplexing, header compression, server push, and binary framing.", Difficulty.MEDIUM, QuestionType.TEXT});
        for (Object[] q : questions) {
            Question question = new Question();
            question.setTopic((String) q[0]);
            question.setSubTopic((String) q[1]);
            question.setQuestion((String) q[2]);
            question.setAnswer((String) q[3]);
            question.setDifficulty((Difficulty) q[4]);
            question.setType((QuestionType) q[5]);
            question.setSource("SEED");
            questionRepository.save(question);
        }
    }

    private CodingTest seedCodingTest(User admin) {
        CodingTest test = new CodingTest();
        test.setTitle("Two Sum");
        test.setDescription("Given an array of integers nums and an integer target, return indices of the two numbers that add up to target.");
        test.setLanguage("java");
        test.setDifficulty(Difficulty.EASY);
        test.setStarterCode("class Solution {\n    public int[] twoSum(int[] nums, int target) {\n        // TODO\n    }\n}\n");
        test.setSolutionCode("class Solution {\n    public int[] twoSum(int[] nums, int target) {\n        java.util.Map<Integer,Integer> map = new java.util.HashMap<>();\n        for (int i = 0; i < nums.length; i++) {\n            int complement = target - nums[i];\n            if (map.containsKey(complement)) {\n                return new int[]{map.get(complement), i};\n            }\n            map.put(nums[i], i);\n        }\n        return new int[0];\n    }\n}\n");
        test.setPublicTest(true);
        test.setCreatedBy(admin);
        codingTestRepository.save(test);

        List<Object[]> cases = List.of(
                new Object[]{"[2,7,11,15] target=9", "[0,1]", false, 0},
                new Object[]{"[3,2,4] target=6", "[1,2]", false, 1},
                new Object[]{"[3,3] target=6", "[0,1]", false, 2},
                new Object[]{"[1,5,9,2] target=7", "[0,3]", true, 3},
                new Object[]{"[0,4,3,0] target=0", "[0,3]", true, 4});
        for (Object[] c : cases) {
            TestCase testCase = new TestCase();
            testCase.setCodingTest(test);
            testCase.setInputData((String) c[0]);
            testCase.setExpectedOutput((String) c[1]);
            testCase.setHidden((Boolean) c[2]);
            testCase.setOrderIndex((Integer) c[3]);
            testCaseRepository.save(testCase);
        }
        return test;
    }

    private void seedCompanies(User recruiter, Map<String, Skill> skills) {
        Company innotech = seedCompany(recruiter, "InnoTech Labs", "innotech-labs",
                "A product engineering company building developer tools.",
                "Technology", "Bengaluru, India", "50-200", 2018);

        seedJob(recruiter, innotech, "Senior Backend Engineer", "senior-backend-engineer",
                "Design and build scalable backend services for our SaaS platform.",
                "5+ years in Java/Spring Boot; strong SQL and distributed systems knowledge.",
                "Bengaluru, India", WorkMode.HYBRID, 4, 8, 3000000, 5000000,
                List.of("Java", "Spring Boot", "SQL", "System Design"), skills);

        seedJob(recruiter, innotech, "Backend Engineer II", "backend-engineer-ii",
                "Own backend features end-to-end across our payments and identity services.",
                "2+ years in Java/Spring Boot; solid PostgreSQL and REST API design.",
                "Bengaluru, India", WorkMode.HYBRID, 2, 5, 1800000, 3200000,
                List.of("Java", "Spring Boot", "PostgreSQL", "Docker"), skills);

        seedJob(recruiter, innotech, "Platform Engineer", "platform-engineer",
                "Build and operate the Kubernetes platform that runs all of our services.",
                "3+ years with Docker/Kubernetes; experience with observability and CI/CD.",
                "Bengaluru, India", WorkMode.ONSITE, 3, 6, 2500000, 4200000,
                List.of("Docker", "Kubernetes", "System Design", "PostgreSQL"), skills);

        Company nimbus = seedCompany(recruiter, "NimbusWorks", "nimbusworks",
                "Cloud-native SaaS helping teams ship software faster.",
                "Cloud Computing", "Pune, India", "201-500", 2015);

        seedJob(recruiter, nimbus, "Senior Frontend Engineer", "senior-frontend-engineer",
                "Craft the developer-facing dashboard used by thousands of teams daily.",
                "3+ years in React/TypeScript; strong design systems and performance skills.",
                "Pune, India", WorkMode.REMOTE, 3, 6, 2200000, 3800000,
                List.of("React", "TypeScript", "JavaScript", "SQL"), skills);

        seedJob(recruiter, nimbus, "Cloud DevOps Engineer", "cloud-devops-engineer",
                "Automate infrastructure, CI/CD pipelines, and multi-cloud networking.",
                "2+ years in Terraform/Kubernetes; Python scripting and Linux expertise.",
                "Pune, India", WorkMode.HYBRID, 2, 5, 2000000, 3500000,
                List.of("Docker", "Kubernetes", "Python", "SQL"), skills);

        Company datacraft = seedCompany(recruiter, "DataCraft AI", "datacraft-ai",
                "Applied AI studio building production machine learning systems.",
                "Artificial Intelligence", "Hyderabad, India", "50-200", 2020);

        seedJob(recruiter, datacraft, "Machine Learning Engineer", "machine-learning-engineer",
                "Build and deploy ML models that power our recommendation platform.",
                "3+ years in Python and ML frameworks; strong SQL and distributed systems.",
                "Hyderabad, India", WorkMode.HYBRID, 3, 7, 2800000, 4600000,
                List.of("Python", "Machine Learning", "SQL", "System Design"), skills);
    }

    private Company seedCompany(User recruiter, String name, String slug, String description,
                                String industry, String location, String sizeRange, int foundedYear) {
        Company company = new Company();
        company.setName(name);
        company.setSlug(slug);
        company.setDescription(description);
        company.setIndustry(industry);
        company.setLocation(location);
        company.setSizeRange(sizeRange);
        company.setFoundedYear(foundedYear);
        company.setVerified(true);
        company.setCreatedBy(recruiter);
        companyRepository.save(company);

        CompanyMember member = new CompanyMember();
        member.setCompany(company);
        member.setUser(recruiter);
        member.setRoleInCompany("Hiring Lead");
        member.setOwner(true);
        companyMemberRepository.save(member);
        return company;
    }

    private void seedJob(User recruiter, Company company, String title, String slug, String description,
                         String requirements, String location, WorkMode workMode,
                         int experienceMin, int experienceMax, int salaryMin, int salaryMax,
                         List<String> skillNames, Map<String, Skill> skills) {
        Job job = new Job();
        job.setCompany(company);
        job.setPostedBy(recruiter);
        job.setTitle(title);
        job.setSlug(slug);
        job.setDescription(description);
        job.setRequirements(requirements);
        job.setLocation(location);
        job.setWorkMode(workMode);
        job.setEmploymentType(EmploymentType.FULL_TIME);
        job.setExperienceMin(experienceMin);
        job.setExperienceMax(experienceMax);
        job.setSalaryMin(new BigDecimal(salaryMin));
        job.setSalaryMax(new BigDecimal(salaryMax));
        job.setCurrency("INR");
        job.setStatus(JobStatus.PUBLISHED);
        job.setPublishedAt(Instant.now());
        jobRepository.save(job);

        for (String name : skillNames) {
            JobSkill jobSkill = new JobSkill();
            jobSkill.setJob(job);
            jobSkill.setSkill(skills.get(name));
            jobSkill.setRequired(true);
            jobSkillRepository.save(jobSkill);
        }
    }
}
