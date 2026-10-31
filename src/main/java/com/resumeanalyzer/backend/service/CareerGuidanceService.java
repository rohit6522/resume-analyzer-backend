package com.resumeanalyzer.backend.service;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.resumeanalyzer.backend.dto.InterviewPrepRequest;
import com.resumeanalyzer.backend.dto.InterviewPrepResponse;
import com.resumeanalyzer.backend.dto.SkillGapRequest;
import com.resumeanalyzer.backend.dto.SkillGapResponse;
import com.resumeanalyzer.backend.entity.ParsedResumeData;
import com.resumeanalyzer.backend.entity.Resume;
import com.resumeanalyzer.backend.exception.ApiException;
import com.resumeanalyzer.backend.repository.ParsedResumeDataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CareerGuidanceService {

    private final GeminiService geminiService;
    private final ParsedResumeDataRepository parsedResumeDataRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final Map<String, List<String>> TRACK_SKILLS = Map.of(
            "FRONTEND", List.of("HTML5/Semantic HTML", "Modern CSS/Tailwind", "JavaScript (ES6+)", "TypeScript", "React 18+", "Next.js/SSR", "State Management (Redux/Zustand)", "REST & GraphQL APIs", "Web Performance & Core Web Vitals", "Unit Testing (Jest/Vitest)"),
            "BACKEND", List.of("Java or Node.js or Python", "Spring Boot or Express or FastAPI", "Relational DB (PostgreSQL/MySQL)", "ORM/Hibernate", "Redis Caching", "RESTful API Design", "Microservices Architecture", "Docker Containerization", "Message Queues (Kafka/RabbitMQ)", "CI/CD & Cloud Deployment"),
            "FULLSTACK", List.of("React/TypeScript", "Node.js or Spring Boot", "PostgreSQL", "REST/GraphQL", "State Management", "JWT Authentication & Security", "Docker", "Git/GitHub Workflows", "API Integration", "Cloud Deployment (AWS/Render/Vercel)"),
            "JAVA", List.of("Java 17/21 Core", "Spring Boot 3+", "Spring Security & JWT", "Hibernate/Spring Data JPA", "PostgreSQL/MySQL", "Multithreading & Concurrency", "Maven/Gradle", "Unit Testing (JUnit 5 & Mockito)", "Microservices Architecture", "Docker"),
            "PYTHON", List.of("Python 3.10+", "FastAPI or Django", "SQLAlchemy ORM", "AsyncIO & Concurrency", "PostgreSQL", "Pytest Unit Testing", "REST API Development", "Docker", "Data Structures & Algorithms", "Git"),
            "DATA_ANALYST", List.of("Advanced SQL", "Python (Pandas, NumPy)", "Tableau or Power BI", "Excel & Financial Modeling", "Exploratory Data Analysis", "Statistics & Hypothesis Testing", "Data Cleaning & Wrangling", "Dashboard Design", "Business Metrics/KPIs", "Storytelling with Data"),
            "DATA_SCIENTIST", List.of("Python (Pandas, NumPy, Scikit-Learn)", "Machine Learning Algorithms", "Statistical Modeling", "SQL", "Feature Engineering", "Data Visualization (Matplotlib, Seaborn)", "Model Evaluation Metrics", "Deep Learning Fundamentals", "Git & Jupyter", "Experimentation/A/B Testing"),
            "AIML", List.of("Python", "PyTorch or TensorFlow", "Large Language Models & Prompt Engineering", "RAG & Vector Databases (Pinecone/Chroma)", "Hugging Face Ecosystem", "Model Fine-Tuning (LoRA/QLoRA)", "API Deployment (FastAPI/Triton)", "Docker", "MLOps Fundamentals", "Math (Linear Algebra, Calculus, Probabilities)"),
            "DEVOPS", List.of("Linux Administration & Shell Scripting", "Docker & Containerization", "Kubernetes & Helm", "Terraform (Infrastructure as Code)", "CI/CD Pipelines (GitHub Actions/GitLab)", "Cloud Platforms (AWS or GCP or Azure)", "Monitoring (Prometheus & Grafana)", "Networking & DNS & SSL", "GitOps (ArgoCD)", "Security & Secret Management")
    );

    public SkillGapResponse analyzeSkillGap(Resume resume, SkillGapRequest request) {
        ParsedResumeData data = parsedResumeDataRepository.findByResumeId(resume.getId())
                .orElseThrow(() -> new ApiException("Resume must be parsed first", HttpStatus.BAD_REQUEST));

        String trackKey = request.getCareerTrack().toUpperCase();
        List<String> benchmarkSkills = TRACK_SKILLS.getOrDefault(trackKey, TRACK_SKILLS.get("FULLSTACK"));

        String prompt = buildSkillGapPrompt(data.getRawText(), trackKey, benchmarkSkills, request.getTargetCompanyOrLevel());
        String aiResponse = geminiService.generateContent(prompt);
        String cleanJson = cleanJsonResponse(aiResponse);

        return parseSkillGapResponse(cleanJson, trackKey);
    }

    public InterviewPrepResponse generateInterviewQuestions(Resume resume, InterviewPrepRequest request) {
        ParsedResumeData data = parsedResumeDataRepository.findByResumeId(resume.getId())
                .orElseThrow(() -> new ApiException("Resume must be parsed first", HttpStatus.BAD_REQUEST));

        String prompt = buildInterviewPrompt(data.getRawText(), request);
        String aiResponse = geminiService.generateContent(prompt);
        String cleanJson = cleanJsonResponse(aiResponse);

        return parseInterviewResponse(cleanJson, request.getTargetJobTitle());
    }

    private String buildSkillGapPrompt(String resumeText, String track, List<String> benchmarks, String level) {
        return """
                You are a senior engineering career mentor.
                Analyze the candidate's resume strictly against the benchmark requirements for the '%s' career track (%s level).
                Benchmark core skill list for this track: %s

                STRICT GROUNDING RULE: Only mark skills as 'skillsOwned' if there is clear evidence in the candidate's resume text. Everything else required is 'skillsNeeded'.

                Return ONLY valid JSON (no markdown, no code fences):
                {
                  "readinessScore": <integer 0-100 indicating track readiness>,
                  "skillsOwned": ["<skill candidate demonstrably has>"],
                  "skillsNeeded": ["<skill missing from benchmark>"],
                  "criticalMissing": ["<top 2-3 most damaging missing skills for this role>"],
                  "learningPriorities": [
                    {
                      "skillName": "<Skill>",
                      "priorityLevel": "CRITICAL or HIGH or MEDIUM",
                      "reason": "<Why recruiters require this>",
                      "estimatedTimeToLearn": "<e.g. 2-3 weeks>"
                    }
                  ],
                  "recommendedProjects": [
                    {
                      "title": "<Concrete production-grade project title>",
                      "description": "<What this project builds and how it solves real problems>",
                      "techStack": "<Specific modern tools and libraries to use>",
                      "portfolioImpact": "<Why this impresses recruiters>"
                    }
                  ],
                  "roadmap": [
                    {
                      "stepNumber": 1,
                      "phaseTitle": "<e.g. Phase 1: Core Fundamentals & Framework Mastery>",
                      "duration": "<e.g. Weeks 1-4>",
                      "keyMilestones": ["<Milestone 1>", "<Milestone 2>"]
                    }
                  ]
                }

                Candidate Resume Text:
                %s
                """.formatted(
                track,
                level != null ? level : "Standard",
                String.join(", ", benchmarks),
                resumeText.length() > 6500 ? resumeText.substring(0, 6500) : resumeText
        );
    }

    private String buildInterviewPrompt(String resumeText, InterviewPrepRequest req) {
        String role = req.getTargetJobTitle() != null ? req.getTargetJobTitle() : "Target Software Engineer Role";
        String jd = req.getJobDescription() != null ? "Target Job Posting:\n" + req.getJobDescription() + "\n" : "";

        return """
                You are a senior tech lead conducting an interview for: %s.
                %s
                Generate 6-8 tailored, realistic interview questions grounded in the candidate's actual resume projects, listed technologies, and experience.
                Include:
                1. Technical core questions (verifying depth in their listed languages/frameworks)
                2. Project deep dive (probing specific architectures, tradeoffs, or challenges in their listed projects)
                3. Coding or System Design scenario
                4. Behavioral question (STAR format: Situation, Task, Action, Result)
                5. HR/Culture fit question
                6. Resume claim verification question

                Return ONLY valid JSON (no markdown, no code fences):
                {
                  "roleTitle": "%s",
                  "totalQuestions": 6,
                  "questions": [
                    {
                      "id": "q1",
                      "category": "TECHNICAL or PROJECT_DEEP_DIVE or CODING_SYSTEM or BEHAVIORAL or HR or RESUME_VERIFICATION",
                      "question": "<The exact interview question>",
                      "contextFromResume": "<Which specific project, company or skill on their resume triggered this question>",
                      "suggestedAnswer": "<A high-scoring, professional model answer>",
                      "keyPointsToHit": ["<Crucial point 1 to mention>", "<Point 2>"],
                      "commonPitfalls": "<Mistakes candidates often make answering this>"
                    }
                  ]
                }

                Candidate Resume Text:
                %s
                """.formatted(
                role,
                jd,
                role,
                resumeText.length() > 6500 ? resumeText.substring(0, 6500) : resumeText
        );
    }

    private String cleanJsonResponse(String raw) {
        String cleaned = raw.trim();
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.replaceAll("```json", "").replaceAll("```", "").trim();
        }
        return cleaned;
    }

    private SkillGapResponse parseSkillGapResponse(String json, String track) {
        try {
            JsonNode node = objectMapper.readTree(json);

            List<String> owned = new ArrayList<>();
            node.path("skillsOwned").forEach(item -> owned.add(item.asText()));

            List<String> needed = new ArrayList<>();
            node.path("skillsNeeded").forEach(item -> needed.add(item.asText()));

            List<String> critical = new ArrayList<>();
            node.path("criticalMissing").forEach(item -> critical.add(item.asText()));

            List<SkillGapResponse.LearningPriorityItem> priorities = new ArrayList<>();
            node.path("learningPriorities").forEach(p -> {
                priorities.add(SkillGapResponse.LearningPriorityItem.builder()
                        .skillName(p.path("skillName").asText())
                        .priorityLevel(p.path("priorityLevel").asText("HIGH"))
                        .reason(p.path("reason").asText())
                        .estimatedTimeToLearn(p.path("estimatedTimeToLearn").asText("2 weeks"))
                        .build());
            });

            List<SkillGapResponse.ProjectRecommendation> projects = new ArrayList<>();
            node.path("recommendedProjects").forEach(pr -> {
                projects.add(SkillGapResponse.ProjectRecommendation.builder()
                        .title(pr.path("title").asText())
                        .description(pr.path("description").asText())
                        .techStack(pr.path("techStack").asText())
                        .portfolioImpact(pr.path("portfolioImpact").asText())
                        .build());
            });

            List<SkillGapResponse.RoadmapStep> roadmap = new ArrayList<>();
            node.path("roadmap").forEach(r -> {
                List<String> milestones = new ArrayList<>();
                r.path("keyMilestones").forEach(m -> milestones.add(m.asText()));

                roadmap.add(SkillGapResponse.RoadmapStep.builder()
                        .stepNumber(r.path("stepNumber").asInt(roadmap.size() + 1))
                        .phaseTitle(r.path("phaseTitle").asText())
                        .duration(r.path("duration").asText())
                        .keyMilestones(milestones)
                        .build());
            });

            return SkillGapResponse.builder()
                    .careerTrack(track)
                    .readinessScore(node.path("readinessScore").asInt(60))
                    .skillsOwned(owned)
                    .skillsNeeded(needed)
                    .criticalMissing(critical)
                    .learningPriorities(priorities)
                    .recommendedProjects(projects)
                    .roadmap(roadmap)
                    .build();

        } catch (Exception e) {
            log.error("Failed to parse SkillGap response: {}", json, e);
            throw new ApiException("Failed to parse skill gap analysis: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private InterviewPrepResponse parseInterviewResponse(String json, String defaultTitle) {
        try {
            JsonNode node = objectMapper.readTree(json);

            List<InterviewPrepResponse.InterviewQuestion> list = new ArrayList<>();
            node.path("questions").forEach(q -> {
                List<String> points = new ArrayList<>();
                q.path("keyPointsToHit").forEach(pt -> points.add(pt.asText()));

                list.add(InterviewPrepResponse.InterviewQuestion.builder()
                        .id(q.path("id").asText("q" + (list.size() + 1)))
                        .category(q.path("category").asText("TECHNICAL"))
                        .question(q.path("question").asText())
                        .contextFromResume(q.path("contextFromResume").asText())
                        .suggestedAnswer(q.path("suggestedAnswer").asText())
                        .keyPointsToHit(points)
                        .commonPitfalls(q.path("commonPitfalls").asText())
                        .build());
            });

            return InterviewPrepResponse.builder()
                    .roleTitle(node.path("roleTitle").asText(defaultTitle != null ? defaultTitle : "Software Engineer"))
                    .totalQuestions(list.size())
                    .questions(list)
                    .build();

        } catch (Exception e) {
            log.error("Failed to parse InterviewPrep response: {}", json, e);
            throw new ApiException("Failed to generate interview questions: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}

