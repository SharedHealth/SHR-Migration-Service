package org.sharedhealth.migrationservice.converter;

import org.hl7.fhir.dstu2.model.StringType;
import org.hl7.fhir.dstu3.model.*;
import org.sharedhealth.migrationservice.config.SHRMigrationProperties;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.hl7.fhir.dstu3.model.ProcedureRequest.ProcedureRequestIntent.ORDER;
import static org.hl7.fhir.dstu3.model.ProcedureRequest.ProcedureRequestStatus.ACTIVE;
import static org.hl7.fhir.dstu3.model.ProcedureRequest.ProcedureRequestStatus.CANCELLED;
import static org.sharedhealth.migrationservice.converter.AllResourceConverter.*;
import static org.sharedhealth.migrationservice.converter.FhirBundleUtil.*;

public class ProcedureRequestConverter {
    public static void convertExistingProcedureRequests(Map<String, org.hl7.fhir.dstu2.model.ProcedureRequest> procedureRequestHashMap, Bundle bundle,
                                                        Composition composition, SHRMigrationProperties migrationProperties) {
        List<Map.Entry<String, org.hl7.fhir.dstu2.model.ProcedureRequest>> cancelledProcedures = procedureRequestHashMap.entrySet().stream().filter(
                procedureRequestEntry -> procedureRequestEntry.getValue().getStatus().equals(org.hl7.fhir.dstu2.model.ProcedureRequest.ProcedureRequestStatus.SUSPENDED)
        ).collect(Collectors.toList());

        List<Map.Entry<String, org.hl7.fhir.dstu2.model.ProcedureRequest>> requestedProcedures = procedureRequestHashMap.entrySet().stream().filter(
                procedureRequestEntry -> procedureRequestEntry.getValue().getStatus().equals(org.hl7.fhir.dstu2.model.ProcedureRequest.ProcedureRequestStatus.REQUESTED)
        ).collect(Collectors.toList());


        for (Map.Entry<String, org.hl7.fhir.dstu2.model.ProcedureRequest> entry : requestedProcedures) {
            String fullUrl = entry.getKey();
            String fullUrlForProvenance = buildProvenanceEntryURL(fullUrl);
            org.hl7.fhir.dstu2.model.ProcedureRequest source = entry.getValue();
            org.hl7.fhir.dstu3.model.ProcedureRequest target = convertProcedureRequest(source, migrationProperties);

            Provenance provenance = createProvenanceForProcedureRequest(fullUrl, fullUrlForProvenance, source);
            addResourceAndProvenanceToBundle(bundle, composition, fullUrl, fullUrlForProvenance, target, provenance);
        }

        for (Map.Entry<String, org.hl7.fhir.dstu2.model.ProcedureRequest> entry : cancelledProcedures) {
            String fullUrl = entry.getKey();
            String fullUrlForProvenance = buildProvenanceEntryURL(fullUrl);
            org.hl7.fhir.dstu2.model.ProcedureRequest source = entry.getValue();
            org.hl7.fhir.dstu3.model.ProcedureRequest target = convertProcedureRequest(source, migrationProperties);

            List<org.hl7.fhir.dstu2.model.Extension> extensions = getExtensionByUrl(source, PREVIOUS_PROCEDURE_ORDER_EXTN_URL);
            if (!CollectionUtils.isEmpty(extensions) && !extensions.get(0).getValue().isEmpty()) {
                Reference reference = target.addRelevantHistory();
                String previousOrderRef = ((StringType) extensions.get(0).getValue()).getValue();
                reference.setReference(buildProvenanceEntryURL(previousOrderRef));
            }

            Provenance provenance = createProvenanceForProcedureRequest(fullUrl, fullUrlForProvenance, source);
            addResourceAndProvenanceToBundle(bundle, composition, fullUrl, fullUrlForProvenance, target, provenance);
        }
    }

    private static org.hl7.fhir.dstu3.model.ProcedureRequest convertProcedureRequest(org.hl7.fhir.dstu2.model.ProcedureRequest source, SHRMigrationProperties migrationProperties) {
        org.hl7.fhir.dstu3.model.ProcedureRequest target = new org.hl7.fhir.dstu3.model.ProcedureRequest();

        List<org.hl7.fhir.dstu2.model.Identifier> identifierDSTU2 = source.getIdentifier();
        List<Identifier> identifiers = identifierDSTU2.stream().map(
                identifierDt -> new Identifier().setValue(identifierDt.getValue())
        ).collect(Collectors.toList());
        target.setIdentifier(identifiers);
        target.setId(source.getId());

        org.hl7.fhir.dstu2.model.Reference sourceSubject = source.getSubject();
        Reference subject = new Reference(sourceSubject.getReference());
        subject.setDisplay(sourceSubject.getDisplay());
        target.setSubject(subject);
        target.setContext(new Reference(source.getEncounter().getReference()));

        target.setCode(convertCode(source.getCode()));
        target.setIntent(ORDER);

        CodeableConcept codeableConcept = target.addCategory();
        Coding coding = codeableConcept.addCoding();
        coding.setSystem(getTrValuesetUrl(migrationProperties, TR_VALUESET_ORDER_TYPE_NAME));
        coding.setCode(TR_PROCEDURE_ORDER_TYPE_CODE);

        List<Annotation> annotations = source.getNotes().stream().map(
                annotationDt -> new Annotation().setText(annotationDt.getText())).collect(Collectors.toList());
        target.setNote(annotations);

        target.setAuthoredOn(source.getOrderedOn());
        org.hl7.fhir.dstu3.model.ProcedureRequest.ProcedureRequestRequesterComponent requester = new org.hl7.fhir.dstu3.model.ProcedureRequest.ProcedureRequestRequesterComponent();
        requester.setAgent(new Reference(source.getOrderer().getReference()));
        target.setRequester(requester);
        org.hl7.fhir.dstu3.model.ProcedureRequest.ProcedureRequestStatus status;

        if (org.hl7.fhir.dstu2.model.ProcedureRequest.ProcedureRequestStatus.REQUESTED.equals(source.getStatus())) {
            status = ACTIVE;
        } else if (org.hl7.fhir.dstu2.model.ProcedureRequest.ProcedureRequestStatus.SUSPENDED.equals(source.getStatus())) {
            status = CANCELLED;
        } else {
            status = ACTIVE;
        }
        target.setStatus(status);
        return target;
    }

    public static Provenance createProvenanceForProcedureRequest(String fullUrlForTarget, String fullUrlForProvenance, org.hl7.fhir.dstu2.model.ProcedureRequest source) {
        Provenance provenance = new Provenance();
        provenance.setId(fullUrlForProvenance);
        provenance.addTarget(new Reference(fullUrlForTarget));
        Provenance.ProvenanceAgentComponent agentComponent = new Provenance.ProvenanceAgentComponent();
        agentComponent.setWho(new Reference(source.getOrderer().getReference()));
        provenance.addAgent(agentComponent);
        provenance.setRecorded(source.getOrderedOn());
        return provenance;
    }

    public static void addResourceAndProvenanceToBundle(Bundle bundle, Composition composition, String fullUrlForResource,
                                                        String fullUrlForProvenance, Resource resourceToAdd, Provenance provenance) {
        Bundle.BundleEntryComponent bundleEntryComponent = bundle.addEntry();
        bundleEntryComponent.setFullUrl(fullUrlForResource);
        bundleEntryComponent.setResource(resourceToAdd);

        Reference reference = composition.addSection().addEntry();
        reference.setReference(fullUrlForResource);
        reference.setDisplay(PROCEDURE_REQUEST_RESOURCE_DISPLAY);

        Bundle.BundleEntryComponent bundleEntryProvenanceComponent = bundle.addEntry();
        bundleEntryProvenanceComponent.setFullUrl(fullUrlForProvenance);
        bundleEntryProvenanceComponent.setResource(provenance);

        Reference provenanceReference = composition.addSection().addEntry();
        provenanceReference.setReference(fullUrlForProvenance);
        provenanceReference.setDisplay(PROVENANCE_PROCEDURE_REQUEST_DISPLAY);
    }
}
