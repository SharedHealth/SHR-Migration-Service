package org.sharedhealth.migrationService.launch;

import org.sharedhealth.migrationService.config.ShrProperties;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Main {

    private static ShrProperties shrProperties ;

    private static void createTaskScheduler() {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> {
        }, 1000, 1000, TimeUnit.MILLISECONDS);
    }

    public static void main(String[] args) {
        shrProperties = ShrProperties.getInstance();
        System.out.println("started: " + shrProperties.getShrUrl());
        createTaskScheduler();
    }
}
