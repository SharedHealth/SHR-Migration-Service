package org.sharedhealth.migrationService.converter;

import ca.uhn.fhir.model.api.ExtensionDt;
import ca.uhn.fhir.model.dstu2.composite.CodingDt;
import ca.uhn.fhir.model.dstu2.composite.IdentifierDt;
import ca.uhn.fhir.model.dstu2.composite.ResourceReferenceDt;
import ca.uhn.fhir.model.dstu2.resource.DiagnosticOrder;
import ca.uhn.fhir.model.dstu2.valueset.DiagnosticOrderStatusEnum;
import ca.uhn.fhir.model.primitive.StringDt;
import org.hl7.fhir.dstu3.model.*;
import org.sharedhealth.migrationService.config.SHRMigrationProperties;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.hl7.fhir.dstu3.model.ProcedureRequest.ProcedureRequestIntent.ORIGINALORDER;
import static org.hl7.fhir.dstu3.model.ProcedureRequest.ProcedureRequestStatus.ACTIVE;
import static org.hl7.fhir.dstu3.model.ProcedureRequest.ProcedureRequestStatus.CANCELLED;
import static org.sharedhealth.migrationService.converter.AllResourceConverter.PROCEDURE_REQUEST_RESOURCE_DISPLAY;
import static org.sharedhealth.migrationService.converter.AllResourceConverter.TR_VALUESET_ORDER_TYPE_NAME;
import static org.sharedhealth.migrationService.converter.FhirBundleUtil.convertCode;
import static org.sharedhealth.migrationService.converter.FhirBundleUtil.getConceptCodingDt;

public class DiagnosticOrderConverter {

    public static void convertExistingDiagnosticOrders(Map<String, DiagnosticOrder> diagnosticOrderHashMap, Bundle bundle, Composition composition, SHRMigrationProperties migrationProperties) {
        for (Map.Entry<String, DiagnosticOrder> entry : diagnosticOrderHashMap.entrySet()) {
            ca.uhn.fhir.model.dstu2.resource.DiagnosticOrder diagnosticOrder = entry.getValue();
            List<ExtensionDt> orderCategoryExtensions = diagnosticOrder.getUndeclaredExtensionsByUrl("https://sharedhealth.atlassian.net/wiki/display/docs/fhir-extensions#DiagnosticOrderCategory");
            String orderCategoryCode = !CollectionUtils.isEmpty(orderCategoryExtensions) ?
                    ((StringDt) orderCategoryExtensions.get(0).getValue()).getValue() : "LAB";
            ProcedureRequest.ProcedureRequestStatus status = DiagnosticOrderStatusEnum.REQUESTED.getCode().equals(diagnosticOrder.getStatus()) ? ACTIVE : CANCELLED;
            int count = 1;
            for (ca.uhn.fhir.model.dstu2.resource.DiagnosticOrder.Item item : diagnosticOrder.getItem()) {
                if (item.getEvent().size() > 1 && hasCancelledEvent(item)) continue;

                ProcedureRequest procedureRequest = new ProcedureRequest();
                Extension extension = procedureRequest.addExtension();
                extension.setUrl("http://hl7.org/fhir/diagnosticorder-r2-marker").setValue(new BooleanType(true));

                List<IdentifierDt> identifier = diagnosticOrder.getIdentifier();
                CodingDt conceptCoding = getConceptCodingDt(item.getCode().getCoding());
                String codeForConceptCoding = conceptCoding != null ? conceptCoding.getCode() : String.valueOf(count++);
                String fullUrl = String.format("%s#%s", identifier.get(0).getValue(), codeForConceptCoding);

                procedureRequest.addIdentifier().setValue(fullUrl);
                procedureRequest.setId(fullUrl);

                ResourceReferenceDt diagnosticOrderSubject = diagnosticOrder.getSubject();
                Reference subject = new Reference(diagnosticOrderSubject.getReference().getValue());
                subject.setDisplay(diagnosticOrderSubject.getDisplay().getValue());
                procedureRequest.setSubject(subject);
                procedureRequest.setContext(new Reference(diagnosticOrder.getEncounter().getReference().getValue()));

                if (!item.getStatus().isEmpty()) {
                    status = DiagnosticOrderStatusEnum.REQUESTED.getCode().equals(item.getStatus()) ? ACTIVE : CANCELLED;
                }

                CodeableConcept codeableConcept = procedureRequest.addCategory();
                Coding coding = codeableConcept.addCoding();
                coding.setSystem(String.format("%s%s", migrationProperties.getTrValuesetUri(), TR_VALUESET_ORDER_TYPE_NAME));
                coding.setCode(orderCategoryCode);

                procedureRequest.setAuthoredOn(item.getEventFirstRep().getDateTime());
                procedureRequest.setStatus(status);
                procedureRequest.setIntent(ORIGINALORDER);
                procedureRequest.setCode(convertCode(item.getCode()));
                org.hl7.fhir.dstu3.model.ProcedureRequest.ProcedureRequestRequesterComponent component = new org.hl7.fhir.dstu3.model.ProcedureRequest.ProcedureRequestRequesterComponent();
                component.setAgent(new Reference(diagnosticOrder.getOrderer().getReference().getValue()));
                procedureRequest.setRequester(component);

                List<Reference> references = diagnosticOrder.getSpecimen().stream().map(
                        resourceReferenceDt -> new Reference(resourceReferenceDt.getReference().getValue())
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

    private static boolean hasCancelledEvent(ca.uhn.fhir.model.dstu2.resource.DiagnosticOrder.Item item) {
        return item.getEvent().stream().anyMatch(event -> event.getStatus().equals(DiagnosticOrderStatusEnum.CANCELLED.getCode()));
    }
}