package org.sharedhealth.migrationService.converter;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.dstu2.composite.ResourceReferenceDt;
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
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static org.sharedhealth.migrationService.converter.DiagnosticOrderConverter.convertExistingDiagnosticOrders;
import static org.sharedhealth.migrationService.converter.MedicationRequestConverter.convertExistingMedicationOrders;
import static org.sharedhealth.migrationService.converter.ProcedureRequestConverter.convertExistingProcedureRequests;
import static org.sharedhealth.migrationService.converter.XMLParser.removeExistingDiagnosticOrderFromBundleContent;

@Component
public class AllResourceConverter {
    public final static String TR_PROCEDURE_ORDER_TYPE_CODE = "PROC";
    public final static String TR_VALUESET_ORDER_TYPE_NAME = "Order-Type";

    private final static String MEDICATION_ORDER_ENTRY_DISPLAY = "Medication Order";

    public final static String MEDICATION_REQUEST_ENTRY_DISPLAY = "Medication Request";
    public final static String PROCEDURE_REQUEST_RESOURCE_DISPLAY = "Procedure Request";
    public final static String PROVENANCE_PROCEDURE_REQUEST_DISPLAY = "Provenance Procedure Request";
    public final static String PROVENANCE_MEDICATION_REQUEST_DISPLAY = "Provenance Medication Request";
    public final static String PREVIOUS_PROCEDURE_ORDER_EXTN_URL = "https://sharedhealth.atlassian.net/wiki/display/docs/fhir-extensions#PreviousProcedureRequest";

    private final IParser stu3Parser;
    private final IParser dstu2Parser;
    private R2R3ConversionManager r2R3ConversionManager;
    private SHRMigrationProperties shrMigrationProperties;

    private Map<String, ca.uhn.fhir.model.dstu2.resource.DiagnosticOrder> diagnosticOrderMap;
    private Map<String, ca.uhn.fhir.model.dstu2.resource.ProcedureRequest> procedureRequestMap;
    private Map<String, ca.uhn.fhir.model.dstu2.resource.MedicationOrder> medicationOrderMap;

    private Map<String, ResourceReferenceDt> diagnosticReportPerformerMap;
    private Map<String, ResourceReferenceDt> procedureRequestOrdererMap;
    private Map<String, List<ResourceReferenceDt>> diagnosticReportRequestMap;

    private Map<String, String> diagnosisNotesMap;
    private Map<String, List<ca.uhn.fhir.model.dstu2.composite.AnnotationDt>> procedureNotesMap;
    private Map<String, String> procedureRequestNotesMap;
    private Map<String, String> fmhConditionNotesMap;

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
        diagnosticOrderMap = new HashMap<>();
        procedureRequestMap = new HashMap<>();
        medicationOrderMap = new HashMap<>();

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
                diagnosticOrderMap.put(entry.getFullUrl(), (ca.uhn.fhir.model.dstu2.resource.DiagnosticOrder) entry.getResource());
            }
            if (entry.getResource() instanceof ca.uhn.fhir.model.dstu2.resource.ProcedureRequest) {
                procedureRequestMap.put(entry.getFullUrl(), (ca.uhn.fhir.model.dstu2.resource.ProcedureRequest) entry.getResource());
            }
            if (entry.getResource() instanceof ca.uhn.fhir.model.dstu2.resource.MedicationOrder) {
                medicationOrderMap.put(entry.getFullUrl(), (ca.uhn.fhir.model.dstu2.resource.MedicationOrder) entry.getResource());
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

        convertExistingDiagnosticOrders(diagnosticOrderMap, bundle, composition, shrMigrationProperties);
        convertExistingProcedureRequests(procedureRequestMap, bundle, composition, shrMigrationProperties);
        convertExistingMedicationOrders(medicationOrderMap, bundle, composition);
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
