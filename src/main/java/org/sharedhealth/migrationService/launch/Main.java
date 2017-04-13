package org.sharedhealth.migrationService.launch;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@ComponentScan(basePackages = {"org.sharedhealth.migrationService"})
@EnableScheduling
public class Main {
    private static final Logger logger = LogManager.getLogger(Main.class);

    public static void main(String[] args) {
        logger.info("starting shr migration service ");
        AnnotationConfigApplicationContext springContext = new AnnotationConfigApplicationContext();
        springContext.scan("org.sharedhealth.migrationService");
        springContext.refresh();


    }
}
