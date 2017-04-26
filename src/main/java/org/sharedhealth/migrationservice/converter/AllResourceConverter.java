package org.sharedhealth.migrationservice.converter;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.api.IDatatype;
import ca.uhn.fhir.model.dstu2.composite.CodingDt;
import ca.uhn.fhir.model.dstu2.composite.QuantityDt;
import ca.uhn.fhir.model.dstu2.composite.ResourceReferenceDt;
import ca.uhn.fhir.parser.IParser;
import org.apache.commons.io.IOUtils;
import org.hl7.fhir.convertors.R2R3ConversionManager;
import org.hl7.fhir.dstu2.model.MedicationAdministration;
import org.hl7.fhir.dstu3.elementmodel.Manager;
import org.hl7.fhir.dstu3.model.*;
import org.hl7.fhir.exceptions.FHIRException;
import org.sharedhealth.migrationservice.config.SHRMigrationProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import static java.lang.Boolean.FALSE;
import static org.sharedhealth.migrationservice.converter.DiagnosticOrderConverter.convertExistingDiagnosticOrders;
import static org.sharedhealth.migrationservice.converter.FhirBundleUtil.getConceptCodingDt;
import static org.sharedhealth.migrationservice.converter.FhirBundleUtil.getTrValuesetUrl;
import static org.sharedhealth.migrationservice.converter.MedicationRequestConverter.convertExistingMedicationOrders;
import static org.sharedhealth.migrationservice.converter.ProcedureRequestConverter.convertExistingProcedureRequests;
import static org.sharedhealth.migrationservice.converter.XMLParser.removeExistingDiagnosticOrderFromBundleContent;

@Component
public class AllResourceConverter {
    private static final Logger logger = LoggerFactory.getLogger(AllResourceConverter.class);

    public final static String TR_PROCEDURE_ORDER_TYPE_CODE = "PROCEDURE";
    public final static String TR_VALUESET_ORDER_TYPE_NAME = "order-type";
    public final static String TR_VALUESET_CONDITION_CATEGORY_NAME = "condition-category";
    private final static String FHIR_VALUESET_CONDITION_CATEGORY_URL = "http://hl7.org/fhir/condition-category";

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
    private Map<String, ca.uhn.fhir.model.dstu2.resource.FamilyMemberHistory> familyMemberHistoryMap;
    private Map<String, Boolean> immunizationReportedMap;

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

    public String convertBundleToStu3(String dstu2BundleContent, String encounterId) {
        diagnosticOrderMap = new HashMap<>();
        procedureRequestMap = new HashMap<>();
        medicationOrderMap = new HashMap<>();
        diagnosticReportPerformerMap = new HashMap<>();
        familyMemberHistoryMap = new HashMap<>();
        immunizationReportedMap = new HashMap<>();

        try {
            dstu2BundleContent = makeChangesToExistingContent(dstu2BundleContent);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            InputStream inputStream = IOUtils.toInputStream(dstu2BundleContent, "UTF-8");
            r2R3ConversionManager.convert(inputStream, outputStream, true, Manager.FhirFormat.XML);
            Bundle bundle = stu3Parser.parseResource(Bundle.class, outputStream.toString());
            makeNecessaryChangesToBundleAfterConversion(bundle);
            return stu3Parser.encodeResourceToString(bundle);
        } catch (Exception e) {
            String message = String.format("Failed while converting bundle with encounter-id %s ", encounterId);
            logger.error(message, e);
            throw new RuntimeException(message, e);
        }
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
            if (entry.getResource() instanceof ca.uhn.fhir.model.dstu2.resource.FamilyMemberHistory) {
                familyMemberHistoryMap.put(entry.getFullUrl(), (ca.uhn.fhir.model.dstu2.resource.FamilyMemberHistory) entry.getResource());
            }
            if (entry.getResource() instanceof ca.uhn.fhir.model.dstu2.resource.DiagnosticReport) {
                ca.uhn.fhir.model.dstu2.resource.DiagnosticReport diagnosticReport = (ca.uhn.fhir.model.dstu2.resource.DiagnosticReport) entry.getResource();
                diagnosticReportPerformerMap.put(entry.getFullUrl(), diagnosticReport.getPerformer());
            }
            if (entry.getResource() instanceof ca.uhn.fhir.model.dstu2.resource.Immunization) {
                ca.uhn.fhir.model.dstu2.resource.Immunization immunization = (ca.uhn.fhir.model.dstu2.resource.Immunization) entry.getResource();
                immunizationReportedMap.put(entry.getFullUrl(), immunization.getReported());
            }
        }

        String content = removeExistingDiagnosticOrderFromBundleContent(dstu2BundleContent);
        ca.uhn.fhir.model.dstu2.resource.Bundle dstu2Bundle = dstu2Parser.parseResource(ca.uhn.fhir.model.dstu2.resource.Bundle.class, content);

        for (ca.uhn.fhir.model.dstu2.resource.Bundle.Entry entry : dstu2Bundle.getEntry()) {
            changeInvalidImmunizationStatus(entry);
        }

        return dstu2Parser.encodeResourceToString(dstu2Bundle);
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
            if (entry.getResource() instanceof DiagnosticReport) {
                DiagnosticReport diagnosticReport = (DiagnosticReport) entry.getResource();
                DiagnosticReport.DiagnosticReportPerformerComponent performerComponent = diagnosticReport.addPerformer();
                String performerRef = diagnosticReportPerformerMap.get(entry.getFullUrl()).getReference().getValue();
                performerComponent.setActor(new Reference(performerRef));
            }
            if (entry.getResource() instanceof Immunization) {
                Immunization immunization = (Immunization) entry.getResource();
                Boolean value = immunizationReportedMap.get(entry.getFullUrl());
                value = (value != null) ? value : FALSE;
                immunization.setPrimarySource(!value);
            }
            makeChangesForCondition(entry);
            addOnsetToFamilyMemberCondition(entry);
        }

        convertExistingDiagnosticOrders(diagnosticOrderMap, bundle, composition, shrMigrationProperties);
        convertExistingProcedureRequests(procedureRequestMap, bundle, composition, shrMigrationProperties);
        convertExistingMedicationOrders(medicationOrderMap, bundle, composition);
    }

    private void makeChangesForCondition(Bundle.BundleEntryComponent entry) {
        if (entry.getResource() instanceof Condition) {
            Condition condition = (Condition) entry.getResource();
            condition.setClinicalStatus(Condition.ConditionClinicalStatus.ACTIVE);
            CodeableConcept category = condition.getCategoryFirstRep();
            if (category.isEmpty()) return;
            Coding coding = category.getCodingFirstRep();
            if (coding.isEmpty()) return;
            if (!coding.getSystem().equals(FHIR_VALUESET_CONDITION_CATEGORY_URL)) return;
            coding.setSystem(getTrValuesetUrl(shrMigrationProperties, TR_VALUESET_CONDITION_CATEGORY_NAME));
        }
    }

    private void addOnsetToFamilyMemberCondition(Bundle.BundleEntryComponent entry) {
        if (entry.getResource() instanceof FamilyMemberHistory) {
            ca.uhn.fhir.model.dstu2.resource.FamilyMemberHistory existingFamilyMemberHistory = familyMemberHistoryMap.get(entry.getFullUrl());
            if (null == existingFamilyMemberHistory) return;
            FamilyMemberHistory familyMemberHistory = (FamilyMemberHistory) entry.getResource();
            for (ca.uhn.fhir.model.dstu2.resource.FamilyMemberHistory.Condition existingCondition : existingFamilyMemberHistory.getCondition()) {
                FamilyMemberHistory.FamilyMemberHistoryConditionComponent condition = getMatchingCondition(familyMemberHistory, existingCondition);
                IDatatype onset = existingCondition.getOnset();
                if (null == onset || onset.isEmpty() || !(onset instanceof QuantityDt)) continue;
                QuantityDt onsetQuantity = (QuantityDt) onset;
                Age age = new Age();
                age.setCode(onsetQuantity.getUnit());
                age.setUnit(onsetQuantity.getUnit());
                age.setSystem(onsetQuantity.getSystem());
                age.setValue(onsetQuantity.getValue());
                condition.setOnset(age);
            }
        }
    }

    private FamilyMemberHistory.FamilyMemberHistoryConditionComponent getMatchingCondition(FamilyMemberHistory familyMemberHistory, ca.uhn.fhir.model.dstu2.resource.FamilyMemberHistory.Condition existingCondition) {
        for (FamilyMemberHistory.FamilyMemberHistoryConditionComponent newCondition : familyMemberHistory.getCondition()) {
            CodingDt existingConceptCoding = getConceptCodingDt(existingCondition.getCode().getCoding());
            Coding newConceptCoding = FhirBundleUtil.getConceptCoding(newCondition.getCode().getCoding());
            if (null == existingConceptCoding && null == newConceptCoding) {
                CodingDt existingCoding = existingCondition.getCode().getCodingFirstRep();
                Coding newCoding = newCondition.getCode().getCodingFirstRep();
                if (!existingCoding.isEmpty() && !newCoding.isEmpty() && existingCoding.getDisplay().equals(newCoding.getDisplay())) {
                    return newCondition;
                }
            }
            boolean existingTRConceptOnly = null != existingConceptCoding && null == newConceptCoding;
            boolean newTRConceptOnly = null == existingConceptCoding && null != newConceptCoding;
            if (existingTRConceptOnly || newTRConceptOnly) continue;
            if (newConceptCoding.getCode().equals(existingConceptCoding.getCode()) && newConceptCoding.getSystem().equals(existingConceptCoding.getSystem())) {
                return newCondition;
            }

        }
        return null;
    }
}