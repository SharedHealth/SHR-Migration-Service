package org.sharedhealth.migrationservice.feed.transaction;

import org.apache.commons.io.FileUtils;
import org.ict4h.atomfeed.client.domain.Event;
import org.ict4h.atomfeed.client.domain.FailedEvent;
import org.ict4h.atomfeed.client.repository.jdbc.AllFailedEventsJdbcImpl;
import org.ict4h.atomfeed.jdbc.JdbcConnectionProvider;
import org.sharedhealth.migrationservice.config.SHRMigrationProperties;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public class SHRFailedEventsJdbcImpl extends AllFailedEventsJdbcImpl {
    private SHRMigrationProperties migrationProperties;

    public SHRFailedEventsJdbcImpl(JdbcConnectionProvider connectionProvider,
                                   SHRMigrationProperties migrationProperties) {
        super(connectionProvider);
        this.migrationProperties = migrationProperties;
    }

    @Override
    public FailedEvent get(String feedUri, String eventId) {
        FailedEvent failedEvent = super.get(feedUri, eventId);
        return replaceContentWithFileBundle(failedEvent);
    }

    @Override
    public void addOrUpdate(FailedEvent failedEvent) {
        Event event = failedEvent.getEvent();
        String content = event.getContent();
        String storageDirPath = migrationProperties.getFailedBundleStorageDirPath();
        File bundleStorageDirPath = new File(storageDirPath);
        if (!bundleStorageDirPath.exists()) {
            super.addOrUpdate(failedEvent);
            return;
        }
        File bundleFile = new File(bundleStorageDirPath, event.getTitle());
        try {
            FileUtils.writeStringToFile(bundleFile, content, "UTF-8");
        } catch (IOException e) {
            throw new RuntimeException("Error while writing failed bundle contents to file");
        }
        Event newEvent = new Event(event.getId(), bundleFile.getAbsolutePath(), event.getTitle(),
                event.getFeedUri(), event.getUpdatedDate());
        newEvent.getCategories().addAll(event.getCategories());
        FailedEvent newFailedEvent = new FailedEvent(failedEvent.getFeedUri(), newEvent,
                failedEvent.getErrorMessage(), failedEvent.getFailedAt(), failedEvent.getRetries());
        super.addOrUpdate(newFailedEvent);
    }

    @Override
    public List<FailedEvent> getOldestNFailedEvents(String feedUri, int numberOfFailedEvents, int numberOfRetries) {
        return super.getOldestNFailedEvents(feedUri, numberOfFailedEvents, numberOfRetries).stream()
                .map(this::replaceContentWithFileBundle).collect(Collectors.toList());
    }

    private FailedEvent replaceContentWithFileBundle(FailedEvent failedEvent) {
        if (failedEvent == null) return null;
        Event event = failedEvent.getEvent();
        File failedBundleFilePath = new File(event.getContent());
        if (!failedBundleFilePath.exists()) {
            return failedEvent;
        }
        String content;
        try {
            content = FileUtils.readFileToString(failedBundleFilePath, "UTF-8");
        } catch (IOException e) {
            throw new RuntimeException("Error while writing failed bundle contents to file");
        }
        Event newEvent = new Event(event.getId(), content, event.getTitle(), event.getFeedUri(), event.getUpdatedDate());
        newEvent.getCategories().addAll(event.getCategories());
        return new FailedEvent(failedEvent.getFeedUri(), newEvent,
                failedEvent.getErrorMessage(), failedEvent.getFailedAt(), failedEvent.getRetries());
    }
}
