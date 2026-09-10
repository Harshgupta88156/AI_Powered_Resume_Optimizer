package com.ai_resume.resume_service.config;

import com.ai_resume.resume_service.entity.LearningResource;
import com.ai_resume.resume_service.repository.LearningResourceRepository;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.ApplicationArguments;
import org.springframework.context.annotation.Configuration;

/**
 * Seeds the curated learning-resource catalog on first boot.
 *
 * Runs per skill rather than "is the table empty", so adding a new skill to
 * this file ships on the next restart without disturbing rows an operator has
 * corrected by hand. Existing skills are never overwritten - the database is
 * the source of truth once seeded.
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class LearningResourceSeeder implements ApplicationRunner {

    private final LearningResourceRepository repository;

    @Override
    public void run(ApplicationArguments args) {
        List<LearningResource> pending = new ArrayList<>();

        for (Seed seed : CATALOG) {
            if (!repository.existsByUrl(seed.url)) {
                pending.add(seed.toEntity());
            }
        }

        if (pending.isEmpty()) {
            log.debug("Learning resource catalog already seeded");
            return;
        }

        repository.saveAll(pending);
        log.info("Seeded {} learning resources", pending.size());
    }

    // =====================================================================
    // Catalog
    // =====================================================================

    private record Seed(
            String skillKey, String skillLabel, String title, String description,
            String url, String provider, String type, String level, String category,
            Integer hours, boolean free, int order) {

        LearningResource toEntity() {
            return LearningResource.builder()
                    .skillKey(skillKey).skillLabel(skillLabel)
                    .title(title).description(description).url(url)
                    .provider(provider).resourceType(type).level(level)
                    .category(category).estimatedHours(hours).free(free)
                    .displayOrder(order).build();
        }
    }

    private static Seed s(String key, String label, String title, String desc, String url,
                          String provider, String type, String level, String cat,
                          Integer hours, boolean free, int order) {
        return new Seed(key, label, title, desc, url, provider, type, level, cat, hours, free, order);
    }

    private static final List<Seed> CATALOG = List.of(

        // ---------------- Java / Backend ----------------
        s("java", "Java", "Java Tutorial — Official Oracle Docs",
          "Oracle's own trail through the language, from syntax to generics and concurrency.",
          "https://dev.java/learn/", "Official Docs", "DOCS", "BEGINNER", "Programming", 25, true, 0),
        s("java", "Java", "Java Programming Masterclass Notes",
          "Baeldung's structured Java guides — short, example-first articles on every core topic.",
          "https://www.baeldung.com/java-tutorial", "Baeldung", "DOCS", "INTERMEDIATE", "Programming", 20, true, 1),

        s("spring boot", "Spring Boot", "Spring Boot Official Guides",
          "Short, self-contained builds: REST services, data access, security, testing.",
          "https://spring.io/guides", "Official Docs", "DOCS", "BEGINNER", "Backend", 15, true, 0),
        s("spring boot", "Spring Boot", "Spring Boot Reference Documentation",
          "The authoritative reference — auto-configuration, profiles, actuator, deployment.",
          "https://docs.spring.io/spring-boot/index.html", "Official Docs", "DOCS", "INTERMEDIATE", "Backend", 30, true, 1),

        s("rest api", "REST APIs", "REST API Tutorial",
          "Resource modelling, status codes, versioning and idempotency explained end to end.",
          "https://restfulapi.net/", "Reference", "DOCS", "BEGINNER", "Backend", 6, true, 0),
        s("rest api", "REST APIs", "Microsoft REST API Guidelines",
          "The design rules a large org actually ships against — naming, paging, errors.",
          "https://github.com/microsoft/api-guidelines", "GitHub", "DOCS", "INTERMEDIATE", "Backend", 4, true, 1),

        s("microservices", "Microservices", "Microservices Patterns Catalog",
          "Chris Richardson's pattern language: decomposition, saga, CQRS, API gateway.",
          "https://microservices.io/patterns/index.html", "Reference", "DOCS", "INTERMEDIATE", "Architecture", 12, true, 0),
        s("microservices", "Microservices", "Building Microservices with Spring Boot",
          "Spring's guide to service discovery, config and inter-service calls.",
          "https://spring.io/microservices", "Official Docs", "DOCS", "INTERMEDIATE", "Architecture", 10, true, 1),

        s("node js", "Node.js", "Introduction to Node.js",
          "The official learning track: modules, the event loop, streams, async patterns.",
          "https://nodejs.org/en/learn/getting-started/introduction-to-nodejs", "Official Docs", "DOCS", "BEGINNER", "Backend", 12, true, 0),
        s("express", "Express.js", "Express Getting Started",
          "Routing, middleware and error handling from the maintainers.",
          "https://expressjs.com/en/starter/installing.html", "Official Docs", "DOCS", "BEGINNER", "Backend", 5, true, 0),

        // ---------------- Frontend ----------------
        s("react", "React", "React Official Learn Guide",
          "The rewritten react.dev tutorial — components, state, effects, and thinking in React.",
          "https://react.dev/learn", "Official Docs", "DOCS", "BEGINNER", "Frontend", 18, true, 0),
        s("react", "React", "React Interactive Tic-Tac-Toe Tutorial",
          "Build a real app step by step; the fastest way to internalise state flow.",
          "https://react.dev/learn/tutorial-tic-tac-toe", "Official Docs", "PRACTICE", "BEGINNER", "Frontend", 3, true, 1),

        s("angular", "Angular", "Angular Interactive Tutorial",
          "In-browser lessons covering components, signals, routing and forms.",
          "https://angular.dev/tutorials/learn-angular", "Official Docs", "PRACTICE", "BEGINNER", "Frontend", 10, true, 0),
        s("angular", "Angular", "Angular Developer Guide",
          "The full reference: dependency injection, RxJS integration, change detection.",
          "https://angular.dev/overview", "Official Docs", "DOCS", "INTERMEDIATE", "Frontend", 25, true, 1),

        s("javascript", "JavaScript", "MDN JavaScript Guide",
          "The reference every working developer keeps open. Thorough and always current.",
          "https://developer.mozilla.org/en-US/docs/Web/JavaScript/Guide", "MDN", "DOCS", "BEGINNER", "Frontend", 20, true, 0),
        s("javascript", "JavaScript", "JavaScript.info — The Modern Tutorial",
          "Deep, well-sequenced explanations of closures, prototypes, promises and modules.",
          "https://javascript.info/", "Reference", "DOCS", "INTERMEDIATE", "Frontend", 30, true, 1),

        s("typescript", "TypeScript", "The TypeScript Handbook",
          "Types, generics, narrowing and declaration files, from the language team.",
          "https://www.typescriptlang.org/docs/handbook/intro.html", "Official Docs", "DOCS", "BEGINNER", "Frontend", 12, true, 0),
        s("typescript", "TypeScript", "Type Challenges",
          "Puzzles that build real fluency in the type system. Genuinely the fastest way up.",
          "https://github.com/type-challenges/type-challenges", "GitHub", "PRACTICE", "ADVANCED", "Frontend", 15, true, 1),

        s("html css", "HTML & CSS", "MDN Learn Web Development",
          "Structured curriculum from first HTML tag through responsive layout.",
          "https://developer.mozilla.org/en-US/docs/Learn_web_development", "MDN", "DOCS", "BEGINNER", "Frontend", 25, true, 0),
        s("html css", "HTML & CSS", "CSS Layout: Flexbox & Grid",
          "Josh Comeau's interactive guides — the clearest treatment of modern layout.",
          "https://css-tricks.com/snippets/css/a-guide-to-flexbox/", "CSS-Tricks", "DOCS", "BEGINNER", "Frontend", 4, true, 1),

        // ---------------- Python / Data ----------------
        s("python", "Python", "The Python Tutorial",
          "The official tour of the language and standard library.",
          "https://docs.python.org/3/tutorial/", "Official Docs", "DOCS", "BEGINNER", "Programming", 15, true, 0),
        s("python", "Python", "Real Python Tutorials",
          "Practical, well-edited articles on everything from decorators to packaging.",
          "https://realpython.com/", "Real Python", "DOCS", "INTERMEDIATE", "Programming", 20, true, 1),

        s("machine learning", "Machine Learning", "Machine Learning Crash Course",
          "Google's hands-on course with TensorFlow exercises and clear intuition.",
          "https://developers.google.com/machine-learning/crash-course", "Google", "COURSE", "BEGINNER", "AI/ML", 20, true, 0),
        s("machine learning", "Machine Learning", "Kaggle Learn: Intro to Machine Learning",
          "Short notebooks you run in the browser — model fitting, validation, overfitting.",
          "https://www.kaggle.com/learn/intro-to-machine-learning", "Kaggle", "PRACTICE", "BEGINNER", "AI/ML", 6, true, 1),

        s("data analysis", "Data Analysis", "pandas Getting Started Tutorials",
          "The official pandas onboarding — loading, reshaping and aggregating real data.",
          "https://pandas.pydata.org/docs/getting_started/intro_tutorials/index.html", "Official Docs", "DOCS", "BEGINNER", "Data", 10, true, 0),

        // ---------------- Databases ----------------
        s("sql", "SQL", "SQLBolt — Interactive SQL Lessons",
          "Write real queries in the browser from the first lesson. Best free SQL start.",
          "https://sqlbolt.com/", "SQLBolt", "PRACTICE", "BEGINNER", "Database", 5, true, 0),
        s("sql", "SQL", "Relational Database Certification",
          "freeCodeCamp's full course with hands-on labs and a certificate.",
          "https://www.freecodecamp.org/learn/relational-database/", "freeCodeCamp", "COURSE", "BEGINNER", "Database", 40, true, 1),

        s("postgresql", "PostgreSQL", "PostgreSQL Official Tutorial",
          "From first table to joins, transactions and inheritance.",
          "https://www.postgresql.org/docs/current/tutorial.html", "Official Docs", "DOCS", "BEGINNER", "Database", 8, true, 0),
        s("postgresql", "PostgreSQL", "Use the Index, Luke",
          "How SQL indexing actually works, and why your query is slow. Essential reading.",
          "https://use-the-index-luke.com/", "Reference", "DOCS", "INTERMEDIATE", "Database", 10, true, 1),

        s("mongodb", "MongoDB", "MongoDB University",
          "Free official courses with in-browser labs, from CRUD to aggregation pipelines.",
          "https://learn.mongodb.com/", "MongoDB", "COURSE", "BEGINNER", "Database", 15, true, 0),

        s("redis", "Redis", "Redis University",
          "Free official courses on caching patterns, data structures and persistence.",
          "https://university.redis.io/", "Redis", "COURSE", "BEGINNER", "Database", 10, true, 0),

        // ---------------- DevOps / Cloud ----------------
        s("docker", "Docker", "Docker Getting Started Guide",
          "Images, containers, volumes and Compose, straight from the docs.",
          "https://docs.docker.com/get-started/", "Official Docs", "DOCS", "BEGINNER", "DevOps", 8, true, 0),
        s("docker", "Docker", "Play with Docker",
          "A throwaway Docker host in the browser — practise without installing anything.",
          "https://labs.play-with-docker.com/", "Docker", "PRACTICE", "BEGINNER", "DevOps", 3, true, 1),

        s("kubernetes", "Kubernetes", "Kubernetes Basics Tutorial",
          "Interactive modules: deploy, scale, update and expose an app on a real cluster.",
          "https://kubernetes.io/docs/tutorials/kubernetes-basics/", "Official Docs", "PRACTICE", "BEGINNER", "DevOps", 8, true, 0),
        s("kubernetes", "Kubernetes", "Kubernetes the Hard Way",
          "Build a cluster component by component. The way to genuinely understand it.",
          "https://github.com/kelseyhightower/kubernetes-the-hard-way", "GitHub", "PRACTICE", "ADVANCED", "DevOps", 12, true, 1),

        s("aws", "AWS", "AWS Cloud Practitioner Essentials",
          "Amazon's own foundation course, and the syllabus for the entry certification.",
          "https://aws.amazon.com/training/digital/aws-cloud-practitioner-essentials/", "AWS", "COURSE", "BEGINNER", "Cloud", 12, true, 0),
        s("aws", "AWS", "AWS Skill Builder",
          "Free tier of role-based learning plans and hands-on labs.",
          "https://skillbuilder.aws/", "AWS", "COURSE", "INTERMEDIATE", "Cloud", 20, true, 1),

        s("azure", "Azure", "Azure Fundamentals Learning Path",
          "Microsoft Learn's AZ-900 path with sandboxes you can run for free.",
          "https://learn.microsoft.com/en-us/training/paths/microsoft-azure-fundamentals-describe-cloud-concepts/",
          "Microsoft Learn", "COURSE", "BEGINNER", "Cloud", 10, true, 0),

        s("google cloud", "Google Cloud", "Google Cloud Skills Boost",
          "Official labs and quests on a real GCP project.",
          "https://www.cloudskillsboost.google/", "Google Cloud", "COURSE", "BEGINNER", "Cloud", 15, true, 0),

        s("ci cd", "CI/CD", "GitHub Actions Documentation",
          "Workflows, matrix builds, caching and secrets — with copy-ready examples.",
          "https://docs.github.com/en/actions", "Official Docs", "DOCS", "BEGINNER", "DevOps", 8, true, 0),
        s("ci cd", "CI/CD", "Continuous Delivery Fundamentals",
          "Martin Fowler's articles on pipelines, trunk-based development and deployment.",
          "https://martinfowler.com/delivery.html", "Reference", "DOCS", "INTERMEDIATE", "DevOps", 6, true, 1),

        s("jenkins", "Jenkins", "Jenkins User Documentation",
          "Pipeline as code, agents and plugin configuration from the project itself.",
          "https://www.jenkins.io/doc/", "Official Docs", "DOCS", "BEGINNER", "DevOps", 8, true, 0),

        s("terraform", "Terraform", "HashiCorp Learn: Terraform",
          "Official guided tutorials, from first resource to modules and remote state.",
          "https://developer.hashicorp.com/terraform/tutorials", "HashiCorp", "PRACTICE", "BEGINNER", "DevOps", 10, true, 0),

        s("linux", "Linux", "Linux Journey",
          "Bite-sized lessons on the shell, permissions, processes and networking.",
          "https://linuxjourney.com/", "Reference", "DOCS", "BEGINNER", "DevOps", 10, true, 0),

        // ---------------- Testing ----------------
        s("testing", "Testing", "Testing Spring Boot Applications",
          "Slice tests, MockMvc, Testcontainers — the official testing reference.",
          "https://docs.spring.io/spring-boot/reference/testing/index.html", "Official Docs", "DOCS", "INTERMEDIATE", "Testing", 8, true, 0),
        s("testing", "Testing", "Testing Library Guiding Principles",
          "How to write tests that survive refactors, for any frontend framework.",
          "https://testing-library.com/docs/guiding-principles/", "Official Docs", "DOCS", "BEGINNER", "Testing", 4, true, 1),

        s("junit", "JUnit", "JUnit 5 User Guide",
          "Annotations, parameterised tests, extensions and assertions.",
          "https://docs.junit.org/current/user-guide/", "Official Docs", "DOCS", "BEGINNER", "Testing", 6, true, 0),

        s("selenium", "Selenium", "Selenium Documentation",
          "WebDriver setup, locators, waits and the page object pattern.",
          "https://www.selenium.dev/documentation/", "Official Docs", "DOCS", "BEGINNER", "Testing", 8, true, 0),

        // ---------------- CS fundamentals ----------------
        s("data structures", "Data Structures & Algorithms", "DSA Roadmap",
          "A sequenced path through the topics interviews actually test.",
          "https://roadmap.sh/datastructures-and-algorithms", "Roadmap.sh", "ROADMAP", "BEGINNER", "Computer Science", 40, true, 0),
        s("data structures", "Data Structures & Algorithms", "NeetCode 150",
          "The curated problem list, grouped by pattern, with worked explanations.",
          "https://neetcode.io/practice", "NeetCode", "PRACTICE", "INTERMEDIATE", "Computer Science", 60, true, 1),

        s("system design", "System Design", "System Design Primer",
          "The most complete free resource: caching, sharding, queues, with real case studies.",
          "https://github.com/donnemartin/system-design-primer", "GitHub", "DOCS", "INTERMEDIATE", "Architecture", 30, true, 0),
        s("system design", "System Design", "ByteByteGo System Design 101",
          "Diagram-led explanations of the patterns behind large systems.",
          "https://github.com/ByteByteGoHq/system-design-101", "GitHub", "DOCS", "INTERMEDIATE", "Architecture", 12, true, 1),

        s("git", "Git", "Pro Git Book",
          "The complete Git book, free online. Branching and internals explained properly.",
          "https://git-scm.com/book/en/v2", "Official Docs", "BOOK", "BEGINNER", "Tools", 12, true, 0),
        s("git", "Git", "Learn Git Branching",
          "A visual, interactive game for branching, rebasing and merging.",
          "https://learngitbranching.js.org/", "Reference", "PRACTICE", "BEGINNER", "Tools", 4, true, 1),

        s("security", "Security", "OWASP Top 10",
          "The ten risks every developer is expected to know and defend against.",
          "https://owasp.org/www-project-top-ten/", "OWASP", "DOCS", "BEGINNER", "Security", 5, true, 0),
        s("security", "Security", "OWASP Cheat Sheet Series",
          "Practical, per-topic defensive guidance: auth, sessions, injection, crypto.",
          "https://cheatsheetseries.owasp.org/", "OWASP", "DOCS", "INTERMEDIATE", "Security", 10, true, 1),

        s("graphql", "GraphQL", "Introduction to GraphQL",
          "Schemas, resolvers and queries from the specification maintainers.",
          "https://graphql.org/learn/", "Official Docs", "DOCS", "BEGINNER", "Backend", 6, true, 0),

        s("kafka", "Apache Kafka", "Apache Kafka Documentation",
          "Topics, partitions, consumer groups and delivery guarantees.",
          "https://kafka.apache.org/documentation/", "Official Docs", "DOCS", "INTERMEDIATE", "Backend", 12, true, 0),
        s("kafka", "Apache Kafka", "Confluent Developer: Kafka Courses",
          "Free courses with hands-on exercises on streaming fundamentals.",
          "https://developer.confluent.io/courses/", "Confluent", "COURSE", "BEGINNER", "Backend", 15, true, 1),

        // ============================================================
        // YouTube Playlists & Video Courses
        // Added to complement the docs/practice resources with video
        // learning paths. Each targets the same skill key so the
        // recommendation engine surfaces them alongside existing entries.
        // ============================================================

        // -- Java --
        s("java", "Java", "Java Full Course — Telusko (YouTube)",
          "Complete Java programming playlist covering basics to advanced OOP, collections, and streams.",
          "https://www.youtube.com/playlist?list=PLsyeobzWxl7pe_IiTfNyr55kwJPWbgxB5", "YouTube", "VIDEO", "BEGINNER", "Programming", 40, true, 2),

        // -- Spring Boot --
        s("spring boot", "Spring Boot", "Spring Boot Tutorial — Amigoscode (YouTube)",
          "Project-based Spring Boot course: REST APIs, Spring Security, JPA, and deployment.",
          "https://www.youtube.com/playlist?list=PLwvrYc43l1Mhuf4sByNeAfsDz45olnMqG", "YouTube", "VIDEO", "BEGINNER", "Backend", 12, true, 2),

        // -- React --
        s("react", "React", "React JS Full Course — freeCodeCamp (YouTube)",
          "Comprehensive React course covering hooks, state management, routing, and project builds.",
          "https://www.youtube.com/watch?v=bMknfKXIFA8", "YouTube", "VIDEO", "BEGINNER", "Frontend", 12, true, 2),

        // -- Angular --
        s("angular", "Angular", "Angular Full Course — Codevolution (YouTube)",
          "Step-by-step Angular tutorial: components, services, routing, forms, and HTTP client.",
          "https://www.youtube.com/playlist?list=PLC3y8-rFHvwhBRAgFinJR8KHIrCdTkZcZ", "YouTube", "VIDEO", "BEGINNER", "Frontend", 18, true, 2),

        // -- JavaScript --
        s("javascript", "JavaScript", "JavaScript Mastery — Namaste JavaScript (YouTube)",
          "Deep-dive into JS internals: execution context, closures, event loop, promises, and async.",
          "https://www.youtube.com/playlist?list=PLlasXeu85E9cQ32gLCvAvr9vNaUccPVNP", "YouTube", "VIDEO", "INTERMEDIATE", "Frontend", 15, true, 2),

        // -- TypeScript --
        s("typescript", "TypeScript", "TypeScript for Beginners — The Net Ninja (YouTube)",
          "Practical TypeScript playlist covering types, interfaces, generics, and integration with frameworks.",
          "https://www.youtube.com/playlist?list=PL4cUxeGkcC9gUgr39Q_yD6v-bSyMwKPUI", "YouTube", "VIDEO", "BEGINNER", "Frontend", 6, true, 2),

        // -- Python --
        s("python", "Python", "Python for Everybody — freeCodeCamp (YouTube)",
          "University-level Python course: data structures, web scraping, databases, and visualization.",
          "https://www.youtube.com/watch?v=8DvywoWv6fI", "YouTube", "VIDEO", "BEGINNER", "Programming", 14, true, 2),

        // -- Node.js --
        s("node js", "Node.js", "Node.js Full Course — freeCodeCamp (YouTube)",
          "Complete Node.js tutorial with Express, MongoDB, REST APIs, and authentication.",
          "https://www.youtube.com/watch?v=Oe421EPjeBE", "YouTube", "VIDEO", "BEGINNER", "Backend", 8, true, 2),

        // -- Docker --
        s("docker", "Docker", "Docker Tutorial — TechWorld with Nana (YouTube)",
          "Practical Docker course: images, containers, Compose, networking, and production patterns.",
          "https://www.youtube.com/watch?v=3c-iBn73dDE", "YouTube", "VIDEO", "BEGINNER", "DevOps", 4, true, 2),

        // -- Kubernetes --
        s("kubernetes", "Kubernetes", "Kubernetes Course — TechWorld with Nana (YouTube)",
          "Full Kubernetes tutorial: pods, deployments, services, volumes, Helm, and monitoring.",
          "https://www.youtube.com/watch?v=X48VuDVv0do", "YouTube", "VIDEO", "BEGINNER", "DevOps", 4, true, 2),

        // -- SQL --
        s("sql", "SQL", "SQL Full Course — Programming with Mosh (YouTube)",
          "Complete SQL tutorial: queries, joins, subqueries, stored procedures, and optimization.",
          "https://www.youtube.com/watch?v=7S_tz1z_5bA", "YouTube", "VIDEO", "BEGINNER", "Database", 3, true, 2),

        // -- MongoDB --
        s("mongodb", "MongoDB", "MongoDB Crash Course — Traversy Media (YouTube)",
          "Hands-on MongoDB tutorial covering CRUD, aggregation pipelines, indexing, and Atlas.",
          "https://www.youtube.com/watch?v=-56x56UppqQ", "YouTube", "VIDEO", "BEGINNER", "Database", 2, true, 2),

        // -- AWS --
        s("aws", "AWS", "AWS Certified Cloud Practitioner — freeCodeCamp (YouTube)",
          "Full certification prep course covering all AWS services, pricing, and architecture.",
          "https://www.youtube.com/watch?v=SOTamWNgDKc", "YouTube", "VIDEO", "BEGINNER", "Cloud", 14, true, 2),

        // -- Machine Learning --
        s("machine learning", "Machine Learning", "Machine Learning — Andrew Ng (Coursera/YouTube)",
          "Stanford's legendary ML course: regression, classification, neural networks, and best practices.",
          "https://www.youtube.com/playlist?list=PLkDaE6sCZn6FNC6YRfRQc_FbeQrF8BwGI", "YouTube", "VIDEO", "BEGINNER", "AI/ML", 40, true, 2),

        // -- Data Structures & Algorithms --
        s("data structures", "Data Structures & Algorithms", "DSA Full Course — Abdul Bari (YouTube)",
          "Complete DSA playlist: arrays, linked lists, trees, graphs, sorting, and dynamic programming.",
          "https://www.youtube.com/playlist?list=PLdo5W4Nhv31bbKJzrsKfMpo_grxuLl8LU", "YouTube", "VIDEO", "BEGINNER", "Computer Science", 50, true, 2),

        // -- System Design --
        s("system design", "System Design", "System Design — Gaurav Sen (YouTube)",
          "Popular system design playlist: load balancers, caching, databases, message queues, and real-world systems.",
          "https://www.youtube.com/playlist?list=PLMCXHnjXnTnvo6alSjVkgxV-VH6EPyvoX", "YouTube", "VIDEO", "INTERMEDIATE", "Architecture", 15, true, 2),

        // -- Git --
        s("git", "Git", "Git & GitHub Full Course — Kunal Kushwaha (YouTube)",
          "Beginner-friendly Git tutorial: branching, merging, rebasing, pull requests, and open source workflows.",
          "https://www.youtube.com/watch?v=apGV9Kg7ics", "YouTube", "VIDEO", "BEGINNER", "Tools", 2, true, 2),

        // -- REST API --
        s("rest api", "REST APIs", "REST API Design — JavaBrains (YouTube)",
          "Practical REST API design course: resources, methods, status codes, versioning, and HATEOAS.",
          "https://www.youtube.com/playlist?list=PLqq-6Pq4lTTZh5U8RbdXq0WaYsPBPFnG", "YouTube", "VIDEO", "BEGINNER", "Backend", 5, true, 2),

        // -- Microservices --
        s("microservices", "Microservices", "Microservices Full Course — in28minutes (YouTube)",
          "Spring Cloud microservices: service discovery, API gateway, circuit breakers, and distributed tracing.",
          "https://www.youtube.com/watch?v=BnknNTN8icw", "YouTube", "VIDEO", "INTERMEDIATE", "Architecture", 6, true, 2),

        // -- CI/CD --
        s("ci cd", "CI/CD", "GitHub Actions Tutorial — TechWorld with Nana (YouTube)",
          "Practical CI/CD with GitHub Actions: workflows, jobs, secrets, Docker builds, and deployments.",
          "https://www.youtube.com/watch?v=R8_veQiYBjI", "YouTube", "VIDEO", "BEGINNER", "DevOps", 2, true, 2),

        // -- HTML & CSS --
        s("html css", "HTML & CSS", "HTML & CSS Full Course — SuperSimpleDev (YouTube)",
          "Modern HTML & CSS course: flexbox, grid, responsive design, and real-world projects.",
          "https://www.youtube.com/watch?v=G3e-cpL7ofc", "YouTube", "VIDEO", "BEGINNER", "Frontend", 7, true, 2),

        // -- Security --
        s("security", "Security", "Web Security — PortSwigger Academy",
          "Free interactive labs covering SQL injection, XSS, CSRF, authentication flaws, and more.",
          "https://portswigger.net/web-security", "PortSwigger", "PRACTICE", "INTERMEDIATE", "Security", 20, true, 2),

        // -- Redis --
        s("redis", "Redis", "Redis Crash Course — Traversy Media (YouTube)",
          "Hands-on Redis tutorial: data types, pub/sub, caching patterns, and persistence.",
          "https://www.youtube.com/watch?v=jgpVdJB2sKQ", "YouTube", "VIDEO", "BEGINNER", "Database", 1, true, 2),

        // -- PostgreSQL --
        s("postgresql", "PostgreSQL", "PostgreSQL Full Course — freeCodeCamp (YouTube)",
          "Complete PostgreSQL tutorial: tables, queries, joins, functions, triggers, and administration.",
          "https://www.youtube.com/watch?v=qw--VYLpxG4", "YouTube", "VIDEO", "BEGINNER", "Database", 4, true, 2),

        // -- Terraform --
        s("terraform", "Terraform", "Terraform Course — freeCodeCamp (YouTube)",
          "Infrastructure as code with Terraform: providers, modules, state management, and AWS deployment.",
          "https://www.youtube.com/watch?v=SLB_c_ayRMo", "YouTube", "VIDEO", "BEGINNER", "DevOps", 3, true, 2),

        // -- Express --
        s("express", "Express.js", "Express.js Crash Course — Traversy Media (YouTube)",
          "Build REST APIs with Express: routing, middleware, error handling, and MongoDB integration.",
          "https://www.youtube.com/watch?v=L72fhGm1tfE", "YouTube", "VIDEO", "BEGINNER", "Backend", 2, true, 2)
    );
}
