package org.sharedhealth.migrationService.launch;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sharedhealth.migrationService.config.ShrProperties;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Main {

    private static ShrProperties shrProperties ;

    private static final Logger logger = LogManager.getLogger(Main.class);


    private static void createTaskScheduler() {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> {
        }, 1000, 1000, TimeUnit.MILLISECONDS);
    }

    public static void main(String[] args) {
        shrProperties = ShrProperties.getInstance();
        logger.info("started: shr server base url is " + shrProperties.getShrServerBaseUrl());
        createTaskScheduler();
    }
}
