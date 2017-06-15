package org.sharedhealth.migrationservice.converter;

import org.hl7.fhir.dstu2.model.DiagnosticOrder;
import org.hl7.fhir.dstu3.model.*;
import org.sharedhealth.migrationservice.config.SHRMigrationProperties;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.hl7.fhir.dstu3.model.ProcedureRequest.ProcedureRequestIntent.ORDER;
import static org.hl7.fhir.dstu3.model.ProcedureRequest.ProcedureRequestStatus.ACTIVE;
import static org.hl7.fhir.dstu3.model.ProcedureRequest.ProcedureRequestStatus.CANCELLED;
import static org.sharedhealth.migrationservice.converter.AllResourceConverter.PROCEDURE_REQUEST_RESOURCE_DISPLAY;
import static org.sharedhealth.migrationservice.converter.AllResourceConverter.TR_VALUESET_ORDER_TYPE_NAME;
import static org.sharedhealth.migrationservice.converter.FhirBundleUtil.*;

public class DiagnosticOrderConverter {
    private static final String DIAGNOSTIC_ORDER_R2_EXTENSION = "http://hl7.org/fhir/diagnosticorder-r2-marker";

    public static void convertExistingDiagnosticOrders(Map<String, org.hl7.fhir.dstu2.model.DiagnosticOrder> diagnosticOrderHashMap,
                                                       Bundle bundle, Composition composition, SHRMigrationProperties migrationProperties) {
        for (Map.Entry<String, org.hl7.fhir.dstu2.model.DiagnosticOrder> entry : diagnosticOrderHashMap.entrySet()) {
            org.hl7.fhir.dstu2.model.DiagnosticOrder diagnosticOrder = entry.getValue();
            List<org.hl7.fhir.dstu2.model.Extension> orderCategoryExtensions = getExtensionByUrl(diagnosticOrder, "https://sharedhealth.atlassian.net/wiki/display/docs/fhir-extensions#DiagnosticOrderCategory");
            String orderCategoryCode = !CollectionUtils.isEmpty(orderCategoryExtensions) ?
                    ((org.hl7.fhir.dstu2.model.StringType) orderCategoryExtensions.get(0).getValue()).getValue() : "LAB";

            if (!"LAB".equals(orderCategoryCode) && !"RAD".equals(orderCategoryCode)) {
                throw new RuntimeException(String.format("Unknown Diagnostic Order Category %s", orderCategoryCode));
            }

            ProcedureRequest.ProcedureRequestStatus status = org.hl7.fhir.dstu2.model.DiagnosticOrder.DiagnosticOrderStatus.REQUESTED.equals(diagnosticOrder.getStatus()) ? ACTIVE : CANCELLED;
            int count = 1;
            for (org.hl7.fhir.dstu2.model.DiagnosticOrder.DiagnosticOrderItemComponent item : diagnosticOrder.getItem()) {
                if (item.getEvent().size() > 1 && hasCancelledEvent(item)) continue;

                ProcedureRequest procedureRequest = new ProcedureRequest();
                Extension extension = procedureRequest.addExtension();
                extension.setUrl(DIAGNOSTIC_ORDER_R2_EXTENSION).setValue(new BooleanType(true));

                List<org.hl7.fhir.dstu2.model.Identifier> identifier = diagnosticOrder.getIdentifier();

                org.hl7.fhir.dstu2.model.Coding conceptCoding = getConceptCodingForDSTU2(item.getCode().getCoding());
                String identifierValue = identifier.get(0).getValue();
                String fullUrl = conceptCoding != null
                        ? String.format("%s#TR:%s", identifierValue, conceptCoding.getCode())
                        : String.format("%s#LOCAL:%s", identifierValue, count++);


                procedureRequest.addIdentifier().setValue(fullUrl);
                procedureRequest.setId(fullUrl);

                org.hl7.fhir.dstu2.model.Reference diagnosticOrderSubject = diagnosticOrder.getSubject();
                Reference subject = new Reference(diagnosticOrderSubject.getReference());
                subject.setDisplay(diagnosticOrderSubject.getDisplay());
                procedureRequest.setSubject(subject);
                procedureRequest.setContext(new Reference(diagnosticOrder.getEncounter().getReference()));

                if (null != item.getStatus()) {
                    status = org.hl7.fhir.dstu2.model.DiagnosticOrder.DiagnosticOrderStatus.REQUESTED.equals(item.getStatus()) ? ACTIVE : CANCELLED;
                }

                CodeableConcept codeableConcept = procedureRequest.addCategory();
                Coding coding = codeableConcept.addCoding();
                coding.setSystem(String.format("%s%s", migrationProperties.getTrValuesetUri(), TR_VALUESET_ORDER_TYPE_NAME));
                coding.setCode(orderCategoryCode);

                List<DiagnosticOrder.DiagnosticOrderEventComponent> events = item.getEvent();
                if (!events.isEmpty()) {
                    procedureRequest.setAuthoredOn(events.get(0).getDateTime());
                }
                procedureRequest.setStatus(status);
                procedureRequest.setIntent(ORDER);
                procedureRequest.setCode(convertCode(item.getCode()));
                org.hl7.fhir.dstu3.model.ProcedureRequest.ProcedureRequestRequesterComponent component = new org.hl7.fhir.dstu3.model.ProcedureRequest.ProcedureRequestRequesterComponent();
                component.setAgent(new Reference(diagnosticOrder.getOrderer().getReference()));
                procedureRequest.setRequester(component);

                List<Reference> references = diagnosticOrder.getSpecimen().stream().map(
                        resourceReferenceDt -> new Reference(resourceReferenceDt.getReference())
                ).collect(Collectors.toList());
                procedureRequest.setSpecimen(references);

                org.hl7.fhir.dstu3.model.Bundle.BundleEntryComponent bundleEntryComponent = bundle.addEntry();
                bundleEntryComponent.setFullUrl(fullUrl);
                bundleEntryComponent.setResource(procedureRequest);

                Reference reference = composition.addSection().addEntry();
                reference.setReference(fullUrl);
                reference.setDisplay(PROCEDURE_REQUEST_RESOURCE_DISPLAY);
            }
        }
    }

    private static boolean hasCancelledEvent(org.hl7.fhir.dstu2.model.DiagnosticOrder.DiagnosticOrderItemComponent item) {
        return item.getEvent().stream().anyMatch(
                event -> DiagnosticOrder.DiagnosticOrderStatus.CANCELLED.equals(event.getStatus())
        );
    }
}
