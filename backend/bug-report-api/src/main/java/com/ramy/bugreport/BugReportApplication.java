package com.ramy.bugreport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import com.ramy.bugreport.domain.EUserRole;
import com.ramy.bugreport.domain.UserAccount;
import com.ramy.bugreport.repository.IUserAccountRepository;

@SpringBootApplication
public class BugReportApplication {

    private static final Logger logger = LoggerFactory.getLogger(BugReportApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(BugReportApplication.class, args);
    }

    @Bean
    public CommandLineRunner demo(IUserAccountRepository userRepository) {
        return args -> {
            userRepository.save(new UserAccount("John Doe", "john.doe@email.com", "Hello123", EUserRole.ADMIN));
            userRepository.save(new UserAccount("John Dev", "john.dev@email.com", "Hello123", EUserRole.DEVELOPER));
            userRepository.save(new UserAccount("John Rep", "john.rep@email.com", "Hello123", EUserRole.REPORTER));

            logger.info("All saved users in memory.");
            logger.info("--------------------------");
            userRepository.findAll().forEach(user -> logger.info(user.toString()));

            logger.info("");
        };
    }
}
