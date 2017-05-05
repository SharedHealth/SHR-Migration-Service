package org.sharedhealth.migrationservice.converter;

import org.hl7.fhir.dstu2.model.MedicationOrder;
import org.hl7.fhir.dstu3.model.*;
import org.springframework.util.CollectionUtils;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.sharedhealth.migrationservice.converter.AllResourceConverter.PROVENANCE_MEDICATION_REQUEST_DISPLAY;
import static org.sharedhealth.migrationservice.converter.FhirBundleUtil.buildProvenanceEntryURL;

public class MedicationRequestConverter {
    public static final String MEDICATION_ORDER_SCHEDULED_DATE_EXTENSION_URL = "https://sharedhealth.atlassian.net/wiki/display/docs/fhir-extensions#TimingScheduledDate";
    public static final String MEDICATION_ORDER_ACTION_EXTENSION_URL = "https://sharedhealth.atlassian.net/wiki/display/docs/fhir-extensions#MedicationOrderAction";
    private static final String MEDICATION_ORDER_ACTION_NEW = "NEW";
    private static final String MEDICATION_ORDER_ACTION_REVISE = "REVISE";
    private static final String MEDICATION_ORDER_ACTION_DISCONTINUE = "DISCONTINUE";

    public static void convertExistingMedicationOrders(Map<String, org.hl7.fhir.dstu2.model.MedicationOrder> medicationOrderMap, Bundle bundle, Composition composition) {
        for (Map.Entry<String, org.hl7.fhir.dstu2.model.MedicationOrder> entry : medicationOrderMap.entrySet()) {
            org.hl7.fhir.dstu2.model.MedicationOrder medicationOrder = entry.getValue();
            Optional<Bundle.BundleEntryComponent> componentOptional = bundle.getEntry().stream().filter(
                    entryComponent -> entryComponent.getFullUrl().equals(entry.getKey())
            ).findFirst();
            if (!componentOptional.isPresent()) continue;
            Bundle.BundleEntryComponent entryComponent = componentOptional.get();
            MedicationRequest medicationRequest = (MedicationRequest) entryComponent.getResource();

            Provenance provenance = new Provenance();
            String provenanceEntryURL = buildProvenanceEntryURL(entry.getKey());
            provenance.setId(provenanceEntryURL);
            provenance.addTarget(new Reference(entry.getKey()));
            Provenance.ProvenanceAgentComponent agentComponent = new Provenance.ProvenanceAgentComponent();
            agentComponent.setWho(new Reference(medicationRequest.getRequester().getAgent().getReference()));
            provenance.addAgent(agentComponent);
            provenance.setRecorded(medicationRequest.getAuthoredOn());

            List<Extension> scheduledDateExtensions = medicationRequest.getDosageInstructionFirstRep().getTiming()
                    .getExtensionsByUrl(MEDICATION_ORDER_SCHEDULED_DATE_EXTENSION_URL);
            if (!CollectionUtils.isEmpty(scheduledDateExtensions)) {
                DateTimeType dateTimeType = (DateTimeType) scheduledDateExtensions.get(0).getValue();
                provenance.getPeriod().setStart(dateTimeType.getValue());
            }
            Date dateEnded = medicationOrder.getDateEnded();
            if (null != dateEnded) {
                provenance.getPeriod().setEnd(dateEnded);
            }

            List<Extension> actionExtensions = medicationRequest.getExtensionsByUrl(MEDICATION_ORDER_ACTION_EXTENSION_URL);
            if (!CollectionUtils.isEmpty(actionExtensions)) {
                StringType stringType = (StringType) actionExtensions.get(0).getValue();
                String action = stringType.getValue();
                if (MEDICATION_ORDER_ACTION_NEW.equals(action)) {
                    Coding coding = new Coding("http://hl7.org/fhir/v3/DataOperation", "CREATE", "create");
                    provenance.setActivity(coding);
                } else if (MEDICATION_ORDER_ACTION_REVISE.equals(action)) {
                    Coding coding = new Coding("http://hl7.org/fhir/v3/DataOperation", "UPDATE", "revise");
                    provenance.setActivity(coding);
                } else if (MEDICATION_ORDER_ACTION_DISCONTINUE.equals(action)) {
                    Coding coding = new Coding("http://hl7.org/fhir/v3/DataOperation", "ABORT", "abort");
                    provenance.setActivity(coding);
                }
            }

            addDosageRepeatBounds(medicationOrderMap, entryComponent);
            addProvenanceToBundle(provenanceEntryURL, provenance, bundle, composition);
        }
    }

    private static void addProvenanceToBundle(String provenanceEntryURL, Provenance provenance,
                                              Bundle bundle, Composition composition) {
        Bundle.BundleEntryComponent bundleEntryProvenanceComponent = bundle.addEntry();
        bundleEntryProvenanceComponent.setFullUrl(provenanceEntryURL);
        bundleEntryProvenanceComponent.setResource(provenance);

        Reference provenanceReference = composition.addSection().addEntry();
        provenanceReference.setReference(provenanceEntryURL);
        provenanceReference.setDisplay(PROVENANCE_MEDICATION_REQUEST_DISPLAY);

    }

    private static MedicationOrder getOrderForRequest(Map<String, MedicationOrder> medicationOrderMap, Bundle.BundleEntryComponent entryComponent) {
        Optional<Map.Entry<String, MedicationOrder>> entryOptional = medicationOrderMap.entrySet().stream().filter(
                entry -> entry.getKey().equals(entryComponent.getFullUrl())
        ).findFirst();
        return entryOptional.map(Map.Entry::getValue).orElse(null);
    }

    private static void addDosageRepeatBounds(Map<String, org.hl7.fhir.dstu2.model.MedicationOrder> medicationOrderMap,
                                              Bundle.BundleEntryComponent entryComponent) {
        org.hl7.fhir.dstu2.model.MedicationOrder medicationOrder = medicationOrderMap.get(entryComponent.getFullUrl());
        org.hl7.fhir.dstu2.model.Type bounds = medicationOrder.getDosageInstruction().get(0).getTiming().getRepeat().getBounds();
        if (!(bounds instanceof org.hl7.fhir.dstu2.model.Quantity) || bounds.isEmpty()) return;
        org.hl7.fhir.dstu2.model.Quantity quantityDt = (org.hl7.fhir.dstu2.model.Quantity) bounds;
        MedicationRequest medicationRequest = (MedicationRequest) entryComponent.getResource();
        if (medicationRequest.getDosageInstructionFirstRep().isEmpty()) return;
        Timing timing = medicationRequest.getDosageInstructionFirstRep().getTiming();
        Timing.TimingRepeatComponent repeat = timing.getRepeat();
        if ((repeat.getBounds() != null && !repeat.getBounds().isEmpty())) return;
        Duration duration = new Duration();
        duration.setCode(quantityDt.getCode());
        duration.setUnit(quantityDt.getUnit());
        duration.setSystem(quantityDt.getSystem());
        duration.setValue(quantityDt.getValue());
        timing.getRepeat().setBounds(duration);
    }
}
