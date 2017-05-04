package org.sharedhealth.migrationservice.converter;

public class DiagnosticOrderConverter {
    private static final String DIAGNOSTIC_ORDER_R2_EXTENSION = "http://hl7.org/fhir/diagnosticorder-r2-marker";

//    public static void convertExistingDiagnosticOrders(Map<String, DiagnosticOrder> diagnosticOrderHashMap,
//                                                       Bundle bundle, Composition composition, SHRMigrationProperties migrationProperties) {
//        for (Map.Entry<String, DiagnosticOrder> entry : diagnosticOrderHashMap.entrySet()) {
//            ca.uhn.fhir.model.dstu2.resource.DiagnosticOrder diagnosticOrder = entry.getValue();
//            List<ExtensionDt> orderCategoryExtensions = diagnosticOrder.getUndeclaredExtensionsByUrl("https://sharedhealth.atlassian.net/wiki/display/docs/fhir-extensions#DiagnosticOrderCategory");
//            String orderCategoryCode = !CollectionUtils.isEmpty(orderCategoryExtensions) ?
//                    ((StringDt) orderCategoryExtensions.get(0).getValue()).getValue() : "LAB";
//            if (!"LAB".equals(orderCategoryCode) && !"RAD".equals(orderCategoryCode)) {
//                throw new RuntimeException(String.format("Unknown Diagnostic Order Category %s", orderCategoryCode));
//            }
//            ProcedureRequest.ProcedureRequestStatus status = DiagnosticOrderStatusEnum.REQUESTED.getCode().equals(diagnosticOrder.getStatus()) ? ACTIVE : CANCELLED;
//            int count = 1;
//            for (ca.uhn.fhir.model.dstu2.resource.DiagnosticOrder.Item item : diagnosticOrder.getItem()) {
//                if (item.getEvent().size() > 1 && hasCancelledEvent(item)) continue;
//
//                ProcedureRequest procedureRequest = new ProcedureRequest();
//                Extension extension = procedureRequest.addExtension();
//                extension.setUrl(DIAGNOSTIC_ORDER_R2_EXTENSION).setValue(new BooleanType(true));
//
//                List<IdentifierDt> identifier = diagnosticOrder.getIdentifier();
//                CodingDt conceptCoding = getConceptCodingForDSTU2(item.getCode().getCoding());
//                String codeForConceptCoding = conceptCoding != null ? conceptCoding.getCode() : String.valueOf(count++);
//                String fullUrl = String.format("%s#%s", identifier.get(0).getValue(), codeForConceptCoding);
//
//                procedureRequest.addIdentifier().setValue(fullUrl);
//                procedureRequest.setId(fullUrl);
//
//                ResourceReferenceDt diagnosticOrderSubject = diagnosticOrder.getSubject();
//                Reference subject = new Reference(diagnosticOrderSubject.getReference().getValue());
//                subject.setDisplay(diagnosticOrderSubject.getDisplay().getValue());
//                procedureRequest.setSubject(subject);
//                procedureRequest.setContext(new Reference(diagnosticOrder.getEncounter().getReference().getValue()));
//
//                if (!item.getStatus().isEmpty()) {
//                    status = DiagnosticOrderStatusEnum.REQUESTED.getCode().equals(item.getStatus()) ? ACTIVE : CANCELLED;
//                }
//
//                CodeableConcept codeableConcept = procedureRequest.addCategory();
//                Coding coding = codeableConcept.addCoding();
//                coding.setSystem(String.format("%s%s", migrationProperties.getTrValuesetUri(), TR_VALUESET_ORDER_TYPE_NAME));
//                coding.setCode(orderCategoryCode);
//
//                procedureRequest.setAuthoredOn(item.getEventFirstRep().getDateTime());
//                procedureRequest.setStatus(status);
//                procedureRequest.setIntent(ORDER);
//                procedureRequest.setCode(convertCode(item.getCode()));
//                org.hl7.fhir.dstu3.model.ProcedureRequest.ProcedureRequestRequesterComponent component = new org.hl7.fhir.dstu3.model.ProcedureRequest.ProcedureRequestRequesterComponent();
//                component.setAgent(new Reference(diagnosticOrder.getOrderer().getReference().getValue()));
//                procedureRequest.setRequester(component);
//
//                List<Reference> references = diagnosticOrder.getSpecimen().stream().map(
//                        resourceReferenceDt -> new Reference(resourceReferenceDt.getReference().getValue())
//                ).collect(Collectors.toList());
//                procedureRequest.setSpecimen(references);
//
//                org.hl7.fhir.dstu3.model.Bundle.BundleEntryComponent bundleEntryComponent = bundle.addEntry();
//                bundleEntryComponent.setFullUrl(fullUrl);
//                bundleEntryComponent.setResource(procedureRequest);
//
//                Reference reference = composition.addSection().addEntry();
//                reference.setReference(fullUrl);
//                reference.setDisplay(PROCEDURE_REQUEST_RESOURCE_DISPLAY);
//            }
//        }
//    }
//
//    private static boolean hasCancelledEvent(ca.uhn.fhir.model.dstu2.resource.DiagnosticOrder.Item item) {
//        return item.getEvent().stream().anyMatch(event -> event.getStatus().equals(DiagnosticOrderStatusEnum.CANCELLED.getCode()));
//    }
}
