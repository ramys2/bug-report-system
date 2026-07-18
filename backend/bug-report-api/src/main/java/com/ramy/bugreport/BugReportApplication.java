package com.ramy.bugreport;

import java.nio.file.Path;
import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import com.ramy.bugreport.domain.Attachment;
import com.ramy.bugreport.domain.BugReport;
import com.ramy.bugreport.domain.Comment;
import com.ramy.bugreport.domain.Component;
import com.ramy.bugreport.domain.EBugSeverity;
import com.ramy.bugreport.domain.EBugStatus;
import com.ramy.bugreport.domain.EUserRole;
import com.ramy.bugreport.domain.Resolution;
import com.ramy.bugreport.domain.SoftwareProject;
import com.ramy.bugreport.domain.UserAccount;
import com.ramy.bugreport.repository.IAttachementRepository;
import com.ramy.bugreport.repository.IBugReportRepository;
import com.ramy.bugreport.repository.ICommentRepository;
import com.ramy.bugreport.repository.IComponentRepository;
import com.ramy.bugreport.repository.IResolutionRepository;
import com.ramy.bugreport.repository.ISoftwareProjectRepository;
import com.ramy.bugreport.repository.IUserAccountRepository;

@SpringBootApplication
public class BugReportApplication {

    private static final Logger logger = LoggerFactory.getLogger(BugReportApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(BugReportApplication.class, args);
    }

    @Bean
    public CommandLineRunner seedData(
            IUserAccountRepository userRepository,
            ISoftwareProjectRepository projectRepository,
            IComponentRepository componentRepository,
            IBugReportRepository bugReportRepository,
            ICommentRepository commentRepository,
            IAttachementRepository attachmentRepository,
            IResolutionRepository resolutionRepository) {
        return args -> {
            if (userRepository.count() > 0) {
                logger.info("Demo data already exists; skipping initialization.");
                return;
            }

            LocalDateTime now = LocalDateTime.now();

            UserAccount admin = userRepository.save(new UserAccount(
                    "Alice Admin", "admin@bugreport.local", "Admin123!", EUserRole.ADMIN));
            UserAccount backendDeveloper = userRepository.save(new UserAccount(
                    "Daniel Developer", "developer@bugreport.local", "Developer123!", EUserRole.DEVELOPER));
            UserAccount frontendDeveloper = userRepository.save(new UserAccount(
                    "Fiona Frontend", "frontend@bugreport.local", "Frontend123!", EUserRole.DEVELOPER));
            UserAccount reporter = userRepository.save(new UserAccount(
                    "Rachel Reporter", "reporter@bugreport.local", "Reporter123!", EUserRole.REPORTER));

            SoftwareProject bugTracker = projectRepository.save(new SoftwareProject(
                    "Bug Report System", "Application for reporting and resolving software defects."));

            Component api = componentRepository.save(new Component(
                    "Backend API", "REST API and persistence layer.", backendDeveloper.getId()));
            Component web = componentRepository.save(new Component(
                    "Web Client", "Browser user interface.", frontendDeveloper.getId()));

            BugReport loginBug = BugReport.builder(
                            reporter.getId(), bugTracker.getId(), api.getId(),
                            "Valid users cannot sign in", EBugSeverity.CRITICAL)
                    .assigneeId(backendDeveloper.getId())
                    .description("The sign-in endpoint returns HTTP 500 for valid credentials.")
                    .stepsToReproduce("Open sign in, enter valid credentials, and submit the form.")
                    .expectedBehavior("The user is authenticated and redirected to the dashboard.")
                    .actualBehavior("The server returns HTTP 500 and the user remains signed out.")
                    .createdAt(now.minusDays(3))
                    .updatedAt(now.minusHours(2))
                    .build();
            loginBug.setStatus(EBugStatus.IN_PROGRESS);
            loginBug = bugReportRepository.save(loginBug);

            Resolution resolution = resolutionRepository.save(new Resolution(
                    "Handle an absent optional avatar before constructing the user response.",
                    now.minusDays(1), "0.1.1", "https://example.invalid/commits/7a21c9d"));

            BugReport profileBug = BugReport.builder(
                            reporter.getId(), bugTracker.getId(), web.getId(),
                            "Profile page is blank without an avatar", EBugSeverity.MEDIUM)
                    .assigneeId(frontendDeveloper.getId())
                    .description("Accounts without an avatar see a blank profile page.")
                    .stepsToReproduce("Create an account without an avatar and open its profile.")
                    .expectedBehavior("The profile displays a default avatar.")
                    .actualBehavior("The profile content is not rendered.")
                    .createdAt(now.minusDays(6))
                    .updatedAt(now.minusDays(1))
                    .build();
            profileBug.setResolution(resolution);
            profileBug.setStatus(EBugStatus.CLOSED);
            profileBug = bugReportRepository.save(profileBug);

            BugReport mobileBug = BugReport.builder(
                            admin.getId(), bugTracker.getId(), web.getId(),
                            "Severity selector overflows on mobile", EBugSeverity.LOW)
                    .description("The severity selector extends beyond screens narrower than 360px.")
                    .stepsToReproduce("Open the new report form at a 320px viewport width.")
                    .expectedBehavior("All form controls remain inside the viewport.")
                    .actualBehavior("The severity selector causes horizontal scrolling.")
                    .createdAt(now.minusHours(8))
                    .updatedAt(now.minusHours(8))
                    .build();
            mobileBug = bugReportRepository.save(mobileBug);

            commentRepository.save(new Comment(
                    loginBug.getId(), reporter.getId(),
                    "This also happens in a private browser window.", now.minusDays(2)));
            commentRepository.save(new Comment(
                    loginBug.getId(), backendDeveloper.getId(),
                    "The failure is in user response mapping; a fix is in progress.", now.minusHours(3)));
            commentRepository.save(new Comment(
                    profileBug.getId(), frontendDeveloper.getId(),
                    "Verified the default-avatar fix in version 0.1.1.", now.minusDays(1)));

            attachmentRepository.save(new Attachment(
                    loginBug.getId(), reporter.getId(), "login-error.png", "image/png",
                    Path.of("demo-uploads/login-error.png"), now.minusDays(2)));
            attachmentRepository.save(new Attachment(
                    mobileBug.getId(), admin.getId(), "mobile-overflow.png", "image/png",
                    Path.of("demo-uploads/mobile-overflow.png"), now.minusHours(7)));

            logger.info(
                    "Initialized demo database: {} users, {} projects, {} components, {} bug reports, "
                            + "{} comments, {} attachments and {} resolutions.",
                    userRepository.count(), projectRepository.count(), componentRepository.count(),
                    bugReportRepository.count(), commentRepository.count(), attachmentRepository.count(),
                    resolutionRepository.count());
        };
    }
}
