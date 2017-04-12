package org.sharedhealth.migrationService.converter;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.api.ExtensionDt;
import ca.uhn.fhir.model.dstu2.composite.*;
import ca.uhn.fhir.model.dstu2.resource.DiagnosticOrder;
import ca.uhn.fhir.model.dstu2.valueset.DiagnosticOrderStatusEnum;
import ca.uhn.fhir.model.primitive.StringDt;
import ca.uhn.fhir.parser.IParser;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.convertors.R2R3ConversionManager;
import org.hl7.fhir.dstu2.model.MedicationAdministration;
import org.hl7.fhir.dstu3.elementmodel.Manager;
import org.hl7.fhir.dstu3.model.*;
import org.hl7.fhir.exceptions.FHIRException;
import org.sharedhealth.migrationService.config.SHRMigrationProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static ca.uhn.fhir.model.dstu2.valueset.ProcedureRequestStatusEnum.REQUESTED;
import static ca.uhn.fhir.model.dstu2.valueset.ProcedureRequestStatusEnum.SUSPENDED;
import static java.util.Arrays.asList;
import static org.hl7.fhir.dstu3.model.ProcedureRequest.ProcedureRequestIntent.ORIGINALORDER;
import static org.hl7.fhir.dstu3.model.ProcedureRequest.ProcedureRequestStatus.ACTIVE;
import static org.hl7.fhir.dstu3.model.ProcedureRequest.ProcedureRequestStatus.CANCELLED;
import static org.sharedhealth.migrationService.converter.XMLParser.removeExistingDiagnosticOrderFromBundleContent;

@Component
public class AllResourceConverter {
    private final String TR_VALUESET_ORDER_TYPE_NAME = "Order-Type";

    private final String MEDICATION_ORDER_ENTRY_DISPLAY = "Medication Order";
    private final String MEDICATION_REQUEST_ENTRY_DISPLAY = "Medication Request";
    private final static String PROCEDURE_REQUEST_RESOURCE_DISPLAY = "Procedure Request";

    private final IParser stu3Parser;
    private final IParser dstu2Parser;
    private R2R3ConversionManager r2R3ConversionManager;
    private SHRMigrationProperties shrMigrationProperties;

    private HashMap<String, ca.uhn.fhir.model.dstu2.resource.DiagnosticOrder> diagnosticOrderHashMap;
    private HashMap<String, ca.uhn.fhir.model.dstu2.resource.ProcedureRequest> procedureRequestHashMap;

    private HashMap<String, ResourceReferenceDt> diagnosticReportPerformerMap;
    private HashMap<String, ResourceReferenceDt> procedureRequestOrdererMap = new HashMap<>();
    private HashMap<String, List<ResourceReferenceDt>> diagnosticReportRequestMap = new HashMap<>();

    private HashMap<String, String> diagnosisNotesMap = new HashMap<>();
    private HashMap<String, List<ca.uhn.fhir.model.dstu2.composite.AnnotationDt>> procedureNotesMap = new HashMap<>();
    private HashMap<String, String> procedureRequestNotesMap = new HashMap<>();
    private HashMap<String, String> fmhConditionNotesMap = new HashMap<>();

    @Autowired
    public AllResourceConverter(SHRMigrationProperties migrationProperties) throws IOException, FHIRException {
        this.r2R3ConversionManager = new R2R3ConversionManager();
        r2R3ConversionManager.setR2Definitions(this.getClass().getResourceAsStream("/fhir-definitions/validation-min.xml.zip"));
        r2R3ConversionManager.setR3Definitions(this.getClass().getResourceAsStream("/fhir-definitions/definitions.xml.zip"));
        r2R3ConversionManager.setMappingLibrary(this.getClass().getResourceAsStream("/fhir-definitions/r2r3maps.zip"));
        stu3Parser = FhirContext.forDstu3().newXmlParser();
        dstu2Parser = FhirContext.forDstu2().newXmlParser();
        shrMigrationProperties = migrationProperties;
    }

    public String convertBundleToStu3(String dstu2BundleContent) throws ParserConfigurationException, SAXException, IOException, TransformerException {
        diagnosticOrderHashMap = new HashMap<>();
        procedureRequestHashMap = new HashMap<>();

        dstu2BundleContent = makeChangesToExistingContent(dstu2BundleContent);

        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            InputStream inputStream = IOUtils.toInputStream(dstu2BundleContent, "UTF-8");
            r2R3ConversionManager.convert(inputStream, outputStream, true, Manager.FhirFormat.XML);
            Bundle bundle = stu3Parser.parseResource(Bundle.class, outputStream.toString());
            makeNecessaryChangesToBundleAfterConversion(bundle);
            return stu3Parser.encodeResourceToString(bundle);
        } catch (IOException | FHIRException e) {
            e.printStackTrace();
        }
        return null;
    }

    private String makeChangesToExistingContent(String dstu2BundleContent) throws IOException, ParserConfigurationException, SAXException, TransformerException {
        ca.uhn.fhir.model.dstu2.resource.Bundle existingBundle = dstu2Parser.parseResource(ca.uhn.fhir.model.dstu2.resource.Bundle.class, dstu2BundleContent);
        for (ca.uhn.fhir.model.dstu2.resource.Bundle.Entry entry : existingBundle.getEntry()) {
            if (entry.getResource() instanceof ca.uhn.fhir.model.dstu2.resource.DiagnosticOrder) {
                diagnosticOrderHashMap.put(entry.getFullUrl(), (ca.uhn.fhir.model.dstu2.resource.DiagnosticOrder) entry.getResource());
            }
            if (entry.getResource() instanceof ca.uhn.fhir.model.dstu2.resource.ProcedureRequest) {
                procedureRequestHashMap.put(entry.getFullUrl(), (ca.uhn.fhir.model.dstu2.resource.ProcedureRequest) entry.getResource());
            }
        }

        String content = removeExistingDiagnosticOrderFromBundleContent(dstu2BundleContent);
        ca.uhn.fhir.model.dstu2.resource.Bundle dstu2Bundle = dstu2Parser.parseResource(ca.uhn.fhir.model.dstu2.resource.Bundle.class, content);

        for (ca.uhn.fhir.model.dstu2.resource.Bundle.Entry entry : dstu2Bundle.getEntry()) {
            changeInvalidImmunizationStatus(entry);
        }

        return dstu2Parser.encodeResourceToString(dstu2Bundle);
    }

    private void extractForProcedureRequest(ca.uhn.fhir.model.dstu2.resource.Bundle.Entry entry) {
        if (entry.getResource() instanceof ca.uhn.fhir.model.dstu2.resource.ProcedureRequest) {
            ca.uhn.fhir.model.dstu2.resource.ProcedureRequest procedureRequest = (ca.uhn.fhir.model.dstu2.resource.ProcedureRequest) entry.getResource();
            procedureRequestOrdererMap.put(entry.getFullUrl(), procedureRequest.getOrderer());
            if (!procedureRequest.getNotes().isEmpty() && !procedureRequest.getNotesFirstRep().isEmpty()) {
                procedureRequestNotesMap.put(entry.getFullUrl(), procedureRequest.getNotesFirstRep().getText());
            }
        }
    }

    private void extractForProcedure(ca.uhn.fhir.model.dstu2.resource.Bundle.Entry entry) {
        if (entry.getResource() instanceof ca.uhn.fhir.model.dstu2.resource.Procedure) {
            ca.uhn.fhir.model.dstu2.resource.Procedure procedure = (ca.uhn.fhir.model.dstu2.resource.Procedure) entry.getResource();
            procedureNotesMap.put(entry.getFullUrl(), procedure.getNotes());
        }
    }

    private void changeInvalidImmunizationStatus(ca.uhn.fhir.model.dstu2.resource.Bundle.Entry entry) {
        if (entry.getResource() instanceof ca.uhn.fhir.model.dstu2.resource.Immunization) {
            ca.uhn.fhir.model.dstu2.resource.Immunization immunization = (ca.uhn.fhir.model.dstu2.resource.Immunization) entry.getResource();
            if ("completed".equals(immunization.getStatus()) || "in-progress".equals(immunization.getStatus())) {
                immunization.setStatus(MedicationAdministration.MedicationAdministrationStatus.COMPLETED.toCode());
            }
            if ("aborted".equals(immunization.getStatus()) || "entered-in-error".equals(immunization.getStatus())) {
                immunization.setStatus(MedicationAdministration.MedicationAdministrationStatus.ENTEREDINERROR.toCode());
            }
        }
    }

    private void extractForFamilyMemberHistory(ca.uhn.fhir.model.dstu2.resource.Bundle.Entry entry) {
        if (entry.getResource() instanceof ca.uhn.fhir.model.dstu2.resource.FamilyMemberHistory) {
            ca.uhn.fhir.model.dstu2.resource.FamilyMemberHistory familyMemberHistory = (ca.uhn.fhir.model.dstu2.resource.FamilyMemberHistory) entry.getResource();
            if (familyMemberHistory.getCondition().isEmpty()) return;
            for (ca.uhn.fhir.model.dstu2.resource.FamilyMemberHistory.Condition condition : familyMemberHistory.getCondition()) {
                String fullUrl = entry.getFullUrl();
                String key = String.format("%s%s", fullUrl, condition.getCode().getCodingFirstRep().getCode());
                fmhConditionNotesMap.put(key, condition.getNote().getText());
            }
        }
    }

    private void extractForConditions(ca.uhn.fhir.model.dstu2.resource.Bundle.Entry entry) {
        if (entry.getResource() instanceof ca.uhn.fhir.model.dstu2.resource.Condition) {
            ca.uhn.fhir.model.dstu2.resource.Condition condition = (ca.uhn.fhir.model.dstu2.resource.Condition) entry.getResource();
            if (StringUtils.isNotBlank(condition.getNotes())) {
                diagnosisNotesMap.put(entry.getFullUrl(), condition.getNotes());
            }
        }
    }

    private void extractForDiagnosticReport(ca.uhn.fhir.model.dstu2.resource.Bundle.Entry entry) {
        if (entry.getResource() instanceof ca.uhn.fhir.model.dstu2.resource.DiagnosticReport) {
            ca.uhn.fhir.model.dstu2.resource.DiagnosticReport resource = (ca.uhn.fhir.model.dstu2.resource.DiagnosticReport) entry.getResource();
            diagnosticReportPerformerMap.put(entry.getFullUrl(), resource.getPerformer());
            diagnosticReportRequestMap.put(entry.getFullUrl(), resource.getRequest());
        }
    }

    private void makeNecessaryChangesToBundleAfterConversion(Bundle bundle) {
        Composition composition = null;
        for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
            if (entry.getResource() instanceof Composition) {
                composition = (Composition) entry.getResource();
                for (Composition.SectionComponent sectionComponent : composition.getSection()) {
                    Reference sectionEntry = sectionComponent.getEntryFirstRep();
                    if (MEDICATION_ORDER_ENTRY_DISPLAY.equals(sectionEntry.getDisplay())) {
                        sectionEntry.setDisplay(MEDICATION_REQUEST_ENTRY_DISPLAY);
                    }
                }
            }

        }

        convertExistingDiagnosticOrdersToProcedureRequests(bundle, composition);
        for (Map.Entry<String, ca.uhn.fhir.model.dstu2.resource.ProcedureRequest> entry : procedureRequestHashMap.entrySet()) {
            String fullUrl = entry.getKey();
            ca.uhn.fhir.model.dstu2.resource.ProcedureRequest source = entry.getValue();
            ProcedureRequest target = new ProcedureRequest();

            List<Identifier> identifiers = source.getIdentifier().stream().map(
                    identifierDt -> new Identifier().setValue(identifierDt.getValue())
            ).collect(Collectors.toList());
            target.setIdentifier(identifiers);
            target.setId(source.getId());

            ResourceReferenceDt sourceSubject = source.getSubject();
            Reference subject = new Reference(sourceSubject.getReference().getValue());
            subject.setDisplay(sourceSubject.getDisplay().getValue());
            target.setSubject(subject);
            target.setContext(new Reference(source.getEncounter().getReference().getValue()));

            target.setCode(convertCode(source.getCode()));
            target.setIntent(ORIGINALORDER);

            List<Annotation> annotations = source.getNotes().stream().map(
                    annotationDt -> new Annotation().setText(annotationDt.getText())).collect(Collectors.toList());
            target.setNote(annotations);

            target.setAuthoredOn(source.getOrderedOn());
            target.setPerformer(new Reference(source.getPerformer().getReference().getValue()));
            ProcedureRequest.ProcedureRequestRequesterComponent requester = new ProcedureRequest.ProcedureRequestRequesterComponent();
            requester.setAgent(new Reference(source.getOrderer().getReference().getValue()));
            target.setRequester(requester);
            ProcedureRequest.ProcedureRequestStatus status;

            if (REQUESTED.getCode().equals(source.getStatus())) {
                status = ACTIVE;
            } else if (SUSPENDED.getCode().equals(source.getStatus())){
                status = CANCELLED;
            }else {
                status=  ACTIVE;
            }
            target.setStatus(status);

            Bundle.BundleEntryComponent bundleEntryComponent = bundle.addEntry();
            bundleEntryComponent.setFullUrl(fullUrl);
            bundleEntryComponent.setResource(target);

            Reference reference = composition.addSection().addEntry();
            reference.setReference(fullUrl);
            reference.setDisplay(PROCEDURE_REQUEST_RESOURCE_DISPLAY);
        }
    }

    private void convertExistingDiagnosticOrdersToProcedureRequests(Bundle bundle, Composition composition) {
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
                String codeForConceptCoding = getCodeForConceptCoding(item);
                codeForConceptCoding = codeForConceptCoding != null ? codeForConceptCoding : String.valueOf(count++);
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
                coding.setSystem(String.format("%s%s", shrMigrationProperties.getTrValuesetUri(), TR_VALUESET_ORDER_TYPE_NAME));
                coding.setCode(orderCategoryCode);

                procedureRequest.setAuthoredOn(item.getEventFirstRep().getDateTime());
                procedureRequest.setStatus(status);
                procedureRequest.setIntent(ORIGINALORDER);
                procedureRequest.setCode(convertCode(item.getCode()));
                ProcedureRequest.ProcedureRequestRequesterComponent component = new ProcedureRequest.ProcedureRequestRequesterComponent();
                component.setAgent(new Reference(diagnosticOrder.getOrderer().getReference().getValue()));
                procedureRequest.setRequester(component);

                Bundle.BundleEntryComponent bundleEntryComponent = bundle.addEntry();
                bundleEntryComponent.setFullUrl(fullUrl);
                bundleEntryComponent.setResource(procedureRequest);

                Reference reference = composition.addSection().addEntry();
                reference.setReference(fullUrl);
                reference.setDisplay(PROCEDURE_REQUEST_RESOURCE_DISPLAY);
            }
        }
    }

    private boolean hasCancelledEvent(ca.uhn.fhir.model.dstu2.resource.DiagnosticOrder.Item item) {
        return item.getEvent().stream().anyMatch(event -> event.getStatus().equals(DiagnosticOrderStatusEnum.CANCELLED.getCode()));
    }

    private String getCodeForConceptCoding(ca.uhn.fhir.model.dstu2.resource.DiagnosticOrder.Item item) {
        for (CodingDt codingDt : item.getCode().getCoding()) {
            if (StringUtils.isNotBlank(codingDt.getSystem()) && codingDt.getSystem().contains("/tr/concepts/")) {
                return codingDt.getCode();
            }
        }
        return null;
    }

    private CodeableConcept convertCode(CodeableConceptDt code) {
        CodeableConcept codeableConcept = new CodeableConcept();
        for (CodingDt codingDt : code.getCoding()) {
            codeableConcept.addCoding(new Coding(codingDt.getSystem(), codingDt.getCode(), codingDt.getDisplay()));
        }
        return codeableConcept;
    }

    private void setForProcedureRequest(Bundle.BundleEntryComponent entry) {
        if (entry.getResource() instanceof ProcedureRequest) {
            ProcedureRequest procedureRequest = (ProcedureRequest) entry.getResource();
            String requesterRef = procedureRequestOrdererMap.get(entry.getFullUrl()).getReference().getValue();
            ProcedureRequest.ProcedureRequestRequesterComponent requesterComponent = new ProcedureRequest.ProcedureRequestRequesterComponent();
            requesterComponent.setAgent(new Reference(requesterRef));
            procedureRequest.setRequester(requesterComponent);
            procedureRequest.setNote(asList(new Annotation().setText(procedureRequestNotesMap.get(entry.getFullUrl()))));
        }
    }

    private void setFotProcedure(Bundle.BundleEntryComponent entry) {
        if (entry.getResource() instanceof Procedure) {
            Procedure procedure = (Procedure) entry.getResource();
            ArrayList<Annotation> annotations = new ArrayList<>();
            for (ca.uhn.fhir.model.dstu2.composite.AnnotationDt annotationDt : procedureNotesMap.get(entry.getFullUrl())) {
                annotations.add(new Annotation().setText(annotationDt.getText()));
            }
            procedure.setNote(annotations);
        }
    }

    private void setForFamilyMemberHistory(Bundle.BundleEntryComponent entry) {
        if (entry.getResource() instanceof FamilyMemberHistory) {
            FamilyMemberHistory familyMemberHistory = (FamilyMemberHistory) entry.getResource();
            if (familyMemberHistory.hasCondition()) {
                for (FamilyMemberHistory.FamilyMemberHistoryConditionComponent condition : familyMemberHistory.getCondition()) {
                    Type onset = condition.getOnset();
                    if (!(onset instanceof Quantity)) continue;
                    Quantity onsetQuantity = (Quantity) onset;
                    Age onsetAge = new Age();
                    onsetAge.setUnit(onsetQuantity.getUnit());
                    onsetAge.setCode(onsetQuantity.getUnit());
                    onsetAge.setValue(onsetQuantity.getValue());
                    onsetAge.setSystem(onsetQuantity.getSystem());
                    condition.setOnset(onsetAge);

                    CodeableConcept code = condition.getCode();
                    if (!code.hasCoding()) continue;
                    String fullUrl = entry.getFullUrl();
                    String key = String.format("%s%s", fullUrl, code.getCodingFirstRep().getCode());
                    condition.setNote(asList(new Annotation().setText(fmhConditionNotesMap.get(key))));
                }
            }
        }
    }

    private void setForConditions(Bundle.BundleEntryComponent entry) {
        if (entry.getResource() instanceof Condition) {
            Condition condition = (Condition) entry.getResource();
            condition.setNote(asList(new Annotation().setText(diagnosisNotesMap.get(entry.getFullUrl()))));
        }
    }

    private void setForDiagnosticReport(Bundle.BundleEntryComponent entry) {
        if (entry.getResource() instanceof DiagnosticReport) {
            DiagnosticReport resource = (DiagnosticReport) entry.getResource();
            ResourceReferenceDt performerRef = diagnosticReportPerformerMap.get(entry.getFullUrl());
            DiagnosticReport.DiagnosticReportPerformerComponent performerComponent = new DiagnosticReport.DiagnosticReportPerformerComponent();
            performerComponent.setActor(new Reference(performerRef.getReference().getValue()));
            resource.setPerformer(asList(performerComponent));

            List<Reference> basedOnRefs = diagnosticReportRequestMap.get(entry.getFullUrl()).stream().map(resourceReferenceDt -> new Reference(resourceReferenceDt.getReference().getValue()))
                    .collect(Collectors.toList());
            resource.setBasedOn(basedOnRefs);
        }
    }
}
