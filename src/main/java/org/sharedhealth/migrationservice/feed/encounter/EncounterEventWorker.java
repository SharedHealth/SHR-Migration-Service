package org.sharedhealth.migrationservice.feed.encounter;

import org.sharedhealth.migrationservice.converter.AllResourceConverter;
import org.sharedhealth.migrationservice.persistent.EncounterRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class EncounterEventWorker {
    private AllResourceConverter allResourceConverter;
    private EncounterRepository encounterRepository;

    @Autowired
    public EncounterEventWorker(AllResourceConverter allResourceConverter, EncounterRepository encounterRepository) {
        this.allResourceConverter = allResourceConverter;
        this.encounterRepository = encounterRepository;
    }

    public void process(String dstu2Bundle, String encounterId) {
        String stu3Bundle = this.allResourceConverter.convertBundleToStu3(dstu2Bundle, encounterId);
        encounterRepository.save(stu3Bundle, encounterId);
    }

}
