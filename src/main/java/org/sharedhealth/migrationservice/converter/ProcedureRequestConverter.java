package org.sharedhealth.migrationservice.converter;

import org.hl7.fhir.dstu3.model.*;

import static org.sharedhealth.migrationservice.converter.AllResourceConverter.PROCEDURE_REQUEST_RESOURCE_DISPLAY;
import static org.sharedhealth.migrationservice.converter.AllResourceConverter.PROVENANCE_PROCEDURE_REQUEST_DISPLAY;

public class ProcedureRequestConverter {
//    public static void convertExistingProcedureRequests(Map<String, ProcedureRequest> procedureRequestHashMap, Bundle bundle,
//                                                        Composition composition, SHRMigrationProperties migrationProperties) {
//        List<Map.Entry<String, ProcedureRequest>> cancelledProcedures = procedureRequestHashMap.entrySet().stream().filter(procedureRequestEntry -> {
//            ca.uhn.fhir.model.dstu2.resource.ProcedureRequest procedureRequest = procedureRequestEntry.getValue();
//            return ProcedureRequestStatusEnum.SUSPENDED.getCode().equals(procedureRequest.getStatus());
//        }).collect(Collectors.toList());
//
//        List<Map.Entry<String, ca.uhn.fhir.model.dstu2.resource.ProcedureRequest>> requestedProcedures = procedureRequestHashMap.entrySet().stream().filter(procedureRequestEntry -> {
//            ca.uhn.fhir.model.dstu2.resource.ProcedureRequest procedureRequest = procedureRequestEntry.getValue();
//            return ProcedureRequestStatusEnum.REQUESTED.getCode().equals(procedureRequest.getStatus());
//        }).collect(Collectors.toList());
//
//
//        for (Map.Entry<String, ca.uhn.fhir.model.dstu2.resource.ProcedureRequest> entry : requestedProcedures) {
//            String fullUrl = entry.getKey();
//            String fullUrlForProvenance = buildProvenanceEntryURL(fullUrl);
//            ca.uhn.fhir.model.dstu2.resource.ProcedureRequest source = entry.getValue();
//            org.hl7.fhir.dstu3.model.ProcedureRequest target = convertProcedureRequest(source, migrationProperties);
//
//            Provenance provenance = createProvenanceForProcedureRequest(fullUrl, fullUrlForProvenance, source);
//            addResourceAndProvenanceToBundle(bundle, composition, fullUrl, fullUrlForProvenance, target, provenance);
//        }
//
//        for (Map.Entry<String, ca.uhn.fhir.model.dstu2.resource.ProcedureRequest> entry : cancelledProcedures) {
//            String fullUrl = entry.getKey();
//            String fullUrlForProvenance = buildProvenanceEntryURL(fullUrl);
//            ca.uhn.fhir.model.dstu2.resource.ProcedureRequest source = entry.getValue();
//            org.hl7.fhir.dstu3.model.ProcedureRequest target = convertProcedureRequest(source, migrationProperties);
//
//            List<ExtensionDt> extensions = source.getUndeclaredExtensionsByUrl(PREVIOUS_PROCEDURE_ORDER_EXTN_URL);
//            if (!CollectionUtils.isEmpty(extensions) && !extensions.get(0).getValue().isEmpty()) {
//                Reference reference = target.addRelevantHistory();
//                String previousOrderRef = ((StringDt) extensions.get(0).getValue()).getValue();
//                reference.setReference(buildProvenanceEntryURL(previousOrderRef));
//            }
//
//            Provenance provenance = createProvenanceForProcedureRequest(fullUrl, fullUrlForProvenance, source);
//            addResourceAndProvenanceToBundle(bundle, composition, fullUrl, fullUrlForProvenance, target, provenance);
//        }
//    }
//
//    private static org.hl7.fhir.dstu3.model.ProcedureRequest convertProcedureRequest(ca.uhn.fhir.model.dstu2.resource.ProcedureRequest source, SHRMigrationProperties migrationProperties) {
//        org.hl7.fhir.dstu3.model.ProcedureRequest target = new org.hl7.fhir.dstu3.model.ProcedureRequest();
//
//        List<IdentifierDt> identifier = source.getIdentifier();
//        List<Identifier> identifiers = identifier.stream().map(
//                identifierDt -> new Identifier().setValue(identifierDt.getValue())
//        ).collect(Collectors.toList());
//        target.setIdentifier(identifiers);
//        target.setId(source.getId());
//
//        ResourceReferenceDt sourceSubject = source.getSubject();
//        Reference subject = new Reference(sourceSubject.getReference().getValue());
//        subject.setDisplay(sourceSubject.getDisplay().getValue());
//        target.setSubject(subject);
//        target.setContext(new Reference(source.getEncounter().getReference().getValue()));
//
//        target.setCode(convertCode(source.getCode()));
//        target.setIntent(ORDER);
//
//        CodeableConcept codeableConcept = target.addCategory();
//        Coding coding = codeableConcept.addCoding();
//        coding.setSystem(getTrValuesetUrl(migrationProperties, TR_VALUESET_ORDER_TYPE_NAME));
//        coding.setCode(TR_PROCEDURE_ORDER_TYPE_CODE);
//
//        List<Annotation> annotations = source.getNotes().stream().map(
//                annotationDt -> new Annotation().setText(annotationDt.getText())).collect(Collectors.toList());
//        target.setNote(annotations);
//
//        target.setAuthoredOn(source.getOrderedOn());
//        org.hl7.fhir.dstu3.model.ProcedureRequest.ProcedureRequestRequesterComponent requester = new org.hl7.fhir.dstu3.model.ProcedureRequest.ProcedureRequestRequesterComponent();
//        requester.setAgent(new Reference(source.getOrderer().getReference().getValue()));
//        target.setRequester(requester);
//        org.hl7.fhir.dstu3.model.ProcedureRequest.ProcedureRequestStatus status;
//
//        if (REQUESTED.getCode().equals(source.getStatus())) {
//            status = ACTIVE;
//        } else if (SUSPENDED.getCode().equals(source.getStatus())) {
//            status = CANCELLED;
//        } else {
//            status = ACTIVE;
//        }
//        target.setStatus(status);
//        return target;
//    }
//
//    public static Provenance createProvenanceForProcedureRequest(String fullUrlForTarget, String fullUrlForProvenance, ca.uhn.fhir.model.dstu2.resource.ProcedureRequest source) {
//        Provenance provenance = new Provenance();
//        provenance.setId(fullUrlForProvenance);
//        provenance.addTarget(new Reference(fullUrlForTarget));
//        Provenance.ProvenanceAgentComponent agentComponent = new Provenance.ProvenanceAgentComponent();
//        agentComponent.setWho(new Reference(source.getOrderer().getReference().getValue()));
//        provenance.addAgent(agentComponent);
//        provenance.setRecorded(source.getOrderedOn());
//        return provenance;
//    }

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
