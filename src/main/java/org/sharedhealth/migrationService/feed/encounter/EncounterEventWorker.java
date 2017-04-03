package org.sharedhealth.migrationService.feed.encounter;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

@Component
public class EncounterEventWorker {

    private Logger logger = LogManager.getLogger(EncounterEventWorker.class);


    public void process(String dstu2Bundle) {
        logger.info("migrating from dstu2");
//        logger.info(dstu2Bundle);

        //do the convertion

    }

}
