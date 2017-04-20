package org.sharedhealth.migrationService.converter;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import org.apache.commons.io.FileUtils;
import org.hl7.fhir.dstu3.model.*;
import org.hl7.fhir.dstu3.model.MedicationRequest.MedicationRequestStatus;
import org.hl7.fhir.exceptions.FHIRException;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sharedhealth.migrationService.config.SHRMigrationProperties;

import java.io.File;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.hl7.fhir.dstu3.model.Condition.ConditionClinicalStatus.ACTIVE;
import static org.hl7.fhir.dstu3.model.Condition.ConditionVerificationStatus.CONFIRMED;
import static org.hl7.fhir.dstu3.model.Condition.ConditionVerificationStatus.PROVISIONAL;
import static org.hl7.fhir.dstu3.model.Encounter.EncounterStatus.FINISHED;
import static org.hl7.fhir.dstu3.model.Observation.ObservationStatus.PRELIMINARY;
import static org.hl7.fhir.dstu3.model.ProcedureRequest.ProcedureRequestIntent.ORDER;
import static org.hl7.fhir.dstu3.model.Timing.UnitsOfTime.D;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sharedhealth.migrationService.converter.FhirBundleUtil.buildProvenanceEntryURL;

public class AllResourceConverterTest {
    private final String trSystem = "http://tr.com";
    private final String trCode = "answerCode";
    private final String trDisplay = "answerDisplay";
    private static AllResourceConverter allResourceConverter;

    private static SHRMigrationProperties shrMigrationProperties;

    private IParser xmlParser = FhirContext.forDstu3().newXmlParser();

    @BeforeClass
    public static void setUp() throws Exception {
        shrMigrationProperties = mock(SHRMigrationProperties.class);
        allResourceConverter = new AllResourceConverter(shrMigrationProperties);
    }

    @Test
    public void shouldConvertABundleWithJustEncounter() throws Exception {
        URL resource = this.getClass().getResource("/bundles/dstu2/bundle_with_encounter.xml");
        String content = FileUtils.readFileToString(new File(resource.getFile()), "UTF-8");

        String s = allResourceConverter.convertBundleToStu3(content);
        Bundle stu3Buble = (Bundle) xmlParser.parseResource(s);

        Bundle.BundleEntryComponent compositionEntry = getFirstEntryOfType(stu3Buble, ResourceType.Composition);
        Composition composition = (Composition) compositionEntry.getResource();
        assertEquals(composition.getConfidentiality().toCode(), "N");
        Coding compositionType = composition.getType().getCodingFirstRep();
        assertEquals("51899-3", compositionType.getCode());
        assertEquals("http://hl7.org/fhir/vs/doc-typecodes", compositionType.getSystem());
        assertEquals("http://www.mci.com/patients/98104750156", composition.getSubject().getReference());
        assertEquals("http://www.fr.com/facilities/10019841.json", composition.getAuthorFirstRep().getReference());
        assertEquals("urn:uuid:de711fc8-3b1b-4089-813e-1ae8b3936ea8", composition.getEncounter().getReference());
        assertEquals(1, composition.getSection().size());

        Bundle.BundleEntryComponent encounterEntry = getFirstEntryOfType(stu3Buble, ResourceType.Encounter);
        Encounter encounter = (Encounter) encounterEntry.getResource();
        assertTrue(isPresentInCompositionSection(composition, encounterEntry));
        assertEquals(FINISHED, encounter.getStatus());
        assertEquals("AMB", encounter.getClass_().getCode());
        assertEquals("http://hl7.org/fhir/v3/ActCode", encounter.getClass_().getSystem());
        assertEquals("http://www.mci.com/patients/98104750156", encounter.getSubject().getReference());
        assertEquals("http://www.pr.com/providers/812.json", encounter.getParticipantFirstRep().getIndividual().getReference());
        assertEquals("http://www.fr.com/facilities/10019841.json", encounter.getServiceProvider().getReference());
        assertNotNull(encounter.getPeriod());
    }

    @Test
    public void shouldConvertABundleWithComplaint() throws Exception {
        URL resource = this.getClass().getResource("/bundles/dstu2/bundle_with_complaint.xml");
        String content = FileUtils.readFileToString(new File(resource.getFile()), "UTF-8");

        String s = allResourceConverter.convertBundleToStu3(content);
        Bundle stu3Buble = (Bundle) xmlParser.parseResource(s);

        Bundle.BundleEntryComponent compositionEntry = getFirstEntryOfType(stu3Buble, ResourceType.Composition);
        Composition composition = (Composition) compositionEntry.getResource();
        assertEquals(2, composition.getSection().size());

        Bundle.BundleEntryComponent chiefComplaintEntry = getFirstEntryOfType(stu3Buble, ResourceType.Condition);
        Condition chiefComplaint = (Condition) chiefComplaintEntry.getResource();
        assertTrue(isPresentInCompositionSection(composition, chiefComplaintEntry));
        assertEquals("http://www.mci.com/patients/98104750156", chiefComplaint.getSubject().getReference());
        assertEquals("urn:uuid:de711fc8-3b1b-4089-813e-1ae8b3936ea8", chiefComplaint.getContext().getReference());
        assertEquals("http://www.pr.com/providers/812.json", chiefComplaint.getAsserter().getReference());

        Coding category = chiefComplaint.getCategoryFirstRep().getCodingFirstRep();
        assertEquals("http://hl7.org/fhir/condition-category", category.getSystem());
        assertEquals("complaint", category.getCode());

        assertEquals(ACTIVE, chiefComplaint.getClinicalStatus());
        assertEquals(PROVISIONAL, chiefComplaint.getVerificationStatus());
        assertTrue(chiefComplaint.getOnset() instanceof Period);

        Coding code = chiefComplaint.getCode().getCodingFirstRep();
        assertEquals(trDisplay, code.getDisplay());
        assertEquals(trCode, code.getCode());
        assertEquals(trSystem, code.getSystem());
    }

    @Test
    public void shouldConvertAnBundleWithDiagnosis() throws Exception {
        URL resource = this.getClass().getResource("/bundles/dstu2/bundle_with_diagnosis.xml");
        String content = FileUtils.readFileToString(new File(resource.getFile()), "UTF-8");

        String s = allResourceConverter.convertBundleToStu3(content);
        Bundle stu3Buble = (Bundle) xmlParser.parseResource(s);

        Bundle.BundleEntryComponent compositionEntry = getFirstEntryOfType(stu3Buble, ResourceType.Composition);
        Composition composition = (Composition) compositionEntry.getResource();
        assertEquals(2, composition.getSection().size());

        Bundle.BundleEntryComponent diagnosisEntry = getFirstEntryOfType(stu3Buble, ResourceType.Condition);
        Condition diagnosis = (Condition) diagnosisEntry.getResource();
        assertTrue(isPresentInCompositionSection(composition, diagnosisEntry));
        assertEquals("http://www.mci.com/patients/98104750156", diagnosis.getSubject().getReference());
        assertEquals("urn:uuid:de711fc8-3b1b-4089-813e-1ae8b3936ea8", diagnosis.getContext().getReference());
        assertEquals("http://www.pr.com/providers/812.json", diagnosis.getAsserter().getReference());

        Coding category = diagnosis.getCategoryFirstRep().getCodingFirstRep();
        assertEquals("http://hl7.org/fhir/condition-category", category.getSystem());
        assertEquals("diagnosis", category.getCode());
        assertEquals(CONFIRMED, diagnosis.getVerificationStatus());

        Coding code = diagnosis.getCode().getCodingFirstRep();
        assertEquals(trDisplay, code.getDisplay());
        assertEquals(trCode, code.getCode());
        assertEquals(trSystem, code.getSystem());

        assertEquals("Sample Notes", diagnosis.getNoteFirstRep().getText());

        List<Extension> extensionsByUrl = diagnosis.getExtensionsByUrl("https://sharedhealth.atlassian.net/wiki/display/docs/fhir-extensions#PreviousCondition");
        assertEquals(1, extensionsByUrl.size());
    }

    @Test
    public void shouldConvertABundleWithDiagnosticReport() throws Exception {
        URL resource = this.getClass().getResource("/bundles/dstu2/bundle_with_diagnostic_report.xml");
        String content = FileUtils.readFileToString(new File(resource.getFile()), "UTF-8");

        String s = allResourceConverter.convertBundleToStu3(content);
        Bundle stu3Buble = (Bundle) xmlParser.parseResource(s);

        Bundle.BundleEntryComponent compositionEntry = getFirstEntryOfType(stu3Buble, ResourceType.Composition);
        Composition composition = (Composition) compositionEntry.getResource();
        assertEquals(3, composition.getSection().size());

        Bundle.BundleEntryComponent diagnosticReportEntry = getFirstEntryOfType(stu3Buble, ResourceType.DiagnosticReport);
        DiagnosticReport diagnosticReport = (DiagnosticReport) diagnosticReportEntry.getResource();
        assertTrue(isPresentInCompositionSection(composition, diagnosticReportEntry));
        assertEquals("http://172.18.46.56:8081/api/v1/patients/98101039678", diagnosticReport.getSubject().getReference());
        assertEquals("urn:uuid:85c310fd-1f9d-4c1e-a16b-4e53a636c46d", diagnosticReport.getContext().getReference());

        Coding category = diagnosticReport.getCategory().getCodingFirstRep();
        assertEquals("http://hl7.org/fhir/v2/0074", category.getSystem());
        assertEquals("LAB", category.getCode());

        Coding code = diagnosticReport.getCode().getCodingFirstRep();
        assertEquals("HEM", code.getCode());
        assertEquals(trSystem, code.getSystem());

        assertEquals(DiagnosticReport.DiagnosticReportStatus.FINAL, diagnosticReport.getStatus());
        assertNotNull(diagnosticReport.getEffective());
        assertNotNull(diagnosticReport.getIssued());

        Bundle.BundleEntryComponent observationEntry = getFirstEntryOfType(stu3Buble, ResourceType.Observation);
        Observation observation = (Observation) observationEntry.getResource();
        assertEquals(diagnosticReport.getResultFirstRep().getReference(), observationEntry.getFullUrl());

        Coding observationCode = observation.getCode().getCodingFirstRep();
        assertEquals("HEM", observationCode.getCode());
        assertEquals(trSystem, observationCode.getSystem());
        assertEquals(Observation.ObservationStatus.FINAL, observation.getStatus());
        assertEquals("changed", observation.getComment());
        assertEquals("http://172.18.46.56:8081/api/v1/patients/98101039678", observation.getSubject().getReference());
        assertEquals("urn:uuid:85c310fd-1f9d-4c1e-a16b-4e53a636c46d", observation.getContext().getReference());
        assertEquals("http://shr.com/patients/hid/encounters/shrEncounterId3", diagnosticReport.getBasedOnFirstRep().getReference());

        assertEquals("http://pr.com/api/1.0/providers/812.json", diagnosticReport.getPerformerFirstRep().getActor().getReference());
        assertEquals("http://pr.com/api/1.0/providers/812.json", observation.getPerformerFirstRep().getReference());
    }

    @Test
    public void shouldConvertABundleWithFamilyMemberHistory() throws Exception {
        URL resource = this.getClass().getResource("/bundles/dstu2/bundle_with_family_member_history.xml");
        String content = FileUtils.readFileToString(new File(resource.getFile()), "UTF-8");

        String s = allResourceConverter.convertBundleToStu3(content);
        Bundle stu3Buble = (Bundle) xmlParser.parseResource(s);

        Bundle.BundleEntryComponent compositionEntry = getFirstEntryOfType(stu3Buble, ResourceType.Composition);
        Composition composition = (Composition) compositionEntry.getResource();
        assertEquals(2, composition.getSection().size());

        Bundle.BundleEntryComponent fmhEntry = getFirstEntryOfType(stu3Buble, ResourceType.FamilyMemberHistory);
        FamilyMemberHistory familyMemberHistory = (FamilyMemberHistory) fmhEntry.getResource();
        assertTrue(isPresentInCompositionSection(composition, fmhEntry));
        assertEquals("http://mci.com/patients/98104750156", familyMemberHistory.getPatient().getReference());
        assertEquals(FamilyMemberHistory.FamilyHistoryStatus.PARTIAL, familyMemberHistory.getStatus());
        assertTrue(familyMemberHistory.getBorn() instanceof DateType);

        Coding relation = familyMemberHistory.getRelationship().getCodingFirstRep();
        assertEquals(trSystem, relation.getSystem());
        assertEquals("FTH", relation.getCode());

        FamilyMemberHistory.FamilyMemberHistoryConditionComponent conditionComponent = familyMemberHistory.getConditionFirstRep();
        Coding code = conditionComponent.getCode().getCodingFirstRep();
        assertEquals(trSystem, code.getSystem());
        assertEquals("3", code.getCode());
        assertEquals("Some notes", conditionComponent.getNoteFirstRep().getText());
        Age onsetAge = conditionComponent.getOnsetAge();
        assertFalse(onsetAge.isEmpty());
        assertEquals(5, onsetAge.getValue().intValue());
        assertEquals("http://unitsofmeasure.org", onsetAge.getSystem());
        assertEquals("a", onsetAge.getUnit());
        assertEquals("a", onsetAge.getCode());
    }

    @Test
    public void shouldConvertABundleWithImmunization() throws Exception {
        URL resource = this.getClass().getResource("/bundles/dstu2/bundle_with_immunization.xml");
        String content = FileUtils.readFileToString(new File(resource.getFile()), "UTF-8");

        String s = allResourceConverter.convertBundleToStu3(content);
        Bundle stu3Buble = (Bundle) xmlParser.parseResource(s);

        Bundle.BundleEntryComponent compositionEntry = getFirstEntryOfType(stu3Buble, ResourceType.Composition);
        Composition composition = (Composition) compositionEntry.getResource();
        assertEquals(2, composition.getSection().size());

        Bundle.BundleEntryComponent immunizationEntry = getFirstEntryOfType(stu3Buble, ResourceType.Immunization);
        Immunization immunization = (Immunization) immunizationEntry.getResource();
        assertTrue(isPresentInCompositionSection(composition, immunizationEntry));

        assertEquals("http://www.mci.com/patients/98104750156", immunization.getPatient().getReference());
        assertEquals("urn:uuid:de711fc8-3b1b-4089-813e-1ae8b3936ea8", immunization.getEncounter().getReference());

        assertTrue(immunization.getPrimarySource());
        assertEquals(Immunization.ImmunizationStatus.COMPLETED, immunization.getStatus());
        assertNotNull(immunization.getDate());

        Coding relation = immunization.getVaccineCode().getCodingFirstRep();
        assertEquals(trSystem, relation.getSystem());
        assertEquals("ABC", relation.getCode());

        assertFalse(immunization.getNotGiven());
        assertEquals("OP", immunization.getPractitionerFirstRep().getRole().getCodingFirstRep().getCode());
        assertEquals("http://www.pr.com/providers/812.json", immunization.getPractitionerFirstRep().getActor().getReference());

        SimpleQuantity doseQuantity = immunization.getDoseQuantity();
        assertEquals(100, doseQuantity.getValue().intValue());
        assertEquals("mg", doseQuantity.getCode());
        assertEquals(trSystem, doseQuantity.getSystem());

        Coding reason = immunization.getExplanation().getReasonFirstRep().getCodingFirstRep();
        assertEquals("281657000", reason.getCode());
        assertEquals(trSystem, reason.getSystem());

        Coding reasonNotGiven = immunization.getExplanation().getReasonNotGivenFirstRep().getCodingFirstRep();
        assertEquals("PATOBJ", reasonNotGiven.getCode());
        assertEquals(trSystem, reasonNotGiven.getSystem());

        Coding route = immunization.getRoute().getCodingFirstRep();
        assertEquals("ORAL-X", route.getCode());
        assertEquals(trSystem, route.getSystem());
        assertEquals("immunization notes", immunization.getNoteFirstRep().getText());
    }

    @Test
    public void shouldConvertABundleWithObservations() throws Exception {
        URL resource = this.getClass().getResource("/bundles/dstu2/bundle_with_observations.xml");
        String content = FileUtils.readFileToString(new File(resource.getFile()), "UTF-8");

        String s = allResourceConverter.convertBundleToStu3(content);
        Bundle stu3Buble = (Bundle) xmlParser.parseResource(s);

        Bundle.BundleEntryComponent compositionEntry = getFirstEntryOfType(stu3Buble, ResourceType.Composition);
        Composition composition = (Composition) compositionEntry.getResource();
        assertEquals(3, composition.getSection().size());

        Supplier<Stream<Bundle.BundleEntryComponent>> streamSupplier = () -> getEntriesOfType(stu3Buble, ResourceType.Observation);
        streamSupplier.get().forEach(observationEntry -> {
            assertTrue(isPresentInCompositionSection(composition, observationEntry));
            Observation observation = (Observation) observationEntry.getResource();
            assertEquals(PRELIMINARY, observation.getStatus());
            assertEquals("http://172.18.46.56:8081/api/v1/patients/98101039678", observation.getSubject().getReference());
            assertEquals("urn:uuid:ff9a0b75-5252-4a65-bb5e-84f1d9fb58c0", observation.getContext().getReference());
            assertEquals("http://hrmtest.dghs.gov.bd/api/1.0/providers/812.json", observation.getPerformerFirstRep().getReference());
        });

        Observation vitals = (Observation) findObservationByName(streamSupplier.get(), "Vitals");
        assertFalse(vitals.hasValue());
        assertEquals(1, vitals.getRelated().size());

        Observation pulse = (Observation) findObservationByName(streamSupplier.get(), "Pulse");
        assertEquals(75, pulse.getValueQuantity().getValue().intValue());
        assertEquals(0, pulse.getRelated().size());
        assertEquals(trSystem, pulse.getCode().getCodingFirstRep().getSystem());
        assertEquals("103", pulse.getCode().getCodingFirstRep().getCode());
    }

    @Test
    public void shouldConvertABundleWithProcedureFulfilment() throws Exception {
        URL resource = this.getClass().getResource("/bundles/dstu2/bundle_with_procedure_fullfilment.xml");
        String content = FileUtils.readFileToString(new File(resource.getFile()), "UTF-8");

        String s = allResourceConverter.convertBundleToStu3(content);
        Bundle stu3Buble = (Bundle) xmlParser.parseResource(s);

        Bundle.BundleEntryComponent compositionEntry = getFirstEntryOfType(stu3Buble, ResourceType.Composition);
        Composition composition = (Composition) compositionEntry.getResource();
        assertEquals(4, composition.getSection().size());

        Bundle.BundleEntryComponent procedureEntry = getFirstEntryOfType(stu3Buble, ResourceType.Procedure);
        Procedure procedure = (Procedure) procedureEntry.getResource();
        assertTrue(isPresentInCompositionSection(composition, procedureEntry));

        assertEquals("http://172.18.46.199:8081/api/default/patients/98001084634", procedure.getSubject().getReference());
        assertEquals("urn:uuid:08435da7-d37e-4b63-87be-15f353eff110", procedure.getContext().getReference());
        assertEquals("http://172.18.46.199:8084/api/1.0/providers/24.json", procedure.getPerformerFirstRep().getActor().getReference());

        assertEquals(Procedure.ProcedureStatus.INPROGRESS, procedure.getStatus());
        assertTrue(procedure.getPerformed() instanceof Period);

        Coding code = procedure.getCode().getCodingFirstRep();
        assertEquals(trSystem, code.getSystem());
        assertEquals("Osteopathic-Treatment-of-Abdomen", code.getCode());

        Coding followup = procedure.getFollowUpFirstRep().getCodingFirstRep();
        assertEquals(trSystem, followup.getSystem());
        assertEquals("385669000", followup.getCode());

        Coding outcome = procedure.getOutcome().getCodingFirstRep();
        assertEquals(trSystem, outcome.getSystem());
        assertEquals("SUCCESS", outcome.getCode());

        assertEquals("http://172.18.46.156:8081/patients/HID123/encounters/shr-enc-id-1#ProcedureRequest/procedure-req-id", procedure.getBasedOnFirstRep().getReference());
        assertEquals("procedure notes", procedure.getNoteFirstRep().getText());
        assertTrue(getFirstEntryOfType(stu3Buble, ResourceType.DiagnosticReport).getFullUrl().equals(procedure.getReportFirstRep().getReference()));
    }

    @Test
    public void shouldConvertABundleWithProcedureRequests() throws Exception {
        URL requestedProcedure = this.getClass().getResource("/bundles/dstu2/bundle_with_procedure_request_new.xml");
        String requestedProcedureContent = FileUtils.readFileToString(new File(requestedProcedure.getFile()), "UTF-8");

        Bundle stu3Bundle = (Bundle) xmlParser.parseResource(allResourceConverter.convertBundleToStu3(requestedProcedureContent));

        Bundle.BundleEntryComponent compositionEntry = getFirstEntryOfType(stu3Bundle, ResourceType.Composition);
        Composition composition = (Composition) compositionEntry.getResource();
        assertEquals(3, composition.getSection().size());

        Bundle.BundleEntryComponent procedureRequestEntry = getFirstEntryOfType(stu3Bundle, ResourceType.ProcedureRequest);
        assertTrue(isPresentInCompositionSection(composition, procedureRequestEntry));
        ProcedureRequest procedureRequest = (ProcedureRequest) procedureRequestEntry.getResource();
        assertEquals(ORDER, procedureRequest.getIntent());
        assertEquals("http://tr.com/valuesets/Order-Type", procedureRequest.getCategoryFirstRep().getCodingFirstRep().getSystem());
        assertEquals("PROCEDURE", procedureRequest.getCategoryFirstRep().getCodingFirstRep().getCode());
        assertEquals("http://172.18.46.199:8081/api/v1/patients/98001175044", procedureRequest.getSubject().getReference());
        assertEquals("urn:uuid:763dee64-44d5-4820-b9c0-6c51bf1d3fa9", procedureRequest.getContext().getReference());
        assertEquals("http://172.18.46.199:8084/api/1.0/providers/24.json", procedureRequest.getRequester().getAgent().getReference());
        Coding code = procedureRequest.getCode().getCodingFirstRep();
        assertEquals(trSystem, code.getSystem());
        assertEquals("f73b4c1a-88b1-11e5-8d1e-005056b0145c", code.getCode());
        assertNotNull(procedureRequest.getAuthoredOn());
        assertEquals("Some Notes", procedureRequest.getNoteFirstRep().getText());

        Bundle.BundleEntryComponent provenanceForNewEntry = getFirstEntryOfType(stu3Bundle, ResourceType.Provenance);
        assertTrue(isPresentInCompositionSection(composition, provenanceForNewEntry));
        Provenance provenanceForNew = (Provenance) provenanceForNewEntry.getResource();
        assertEquals(procedureRequestEntry.getFullUrl(), provenanceForNew.getTargetFirstRep().getReference());
        assertEquals(procedureRequest.getAuthoredOn(), provenanceForNew.getRecorded());
        assertEquals(procedureRequest.getRequester().getAgent().getReference(), provenanceForNew.getAgentFirstRep().getWhoReference().getReference());

        URL cancelledProcedure = this.getClass().getResource("/bundles/dstu2/bundle_with_procedure_request_cancelled.xml");
        String cancelledProcedureContent = FileUtils.readFileToString(new File(cancelledProcedure.getFile()), "UTF-8");

        Bundle newBundle = (Bundle) xmlParser.parseResource(allResourceConverter.convertBundleToStu3(cancelledProcedureContent));

        Bundle.BundleEntryComponent newCompositionEntry = getFirstEntryOfType(newBundle, ResourceType.Composition);
        Composition newComposition = (Composition) newCompositionEntry.getResource();
        assertEquals(3, newComposition.getSection().size());

        Bundle.BundleEntryComponent cancelledProcedureRequestEntry = getEntriesOfType(newBundle, ResourceType.ProcedureRequest).findFirst().get();
        assertTrue(isPresentInCompositionSection(newComposition, cancelledProcedureRequestEntry));
        ProcedureRequest cancelledProcedureRequest = (ProcedureRequest) cancelledProcedureRequestEntry.getResource();
        assertEquals(1, cancelledProcedureRequest.getRelevantHistory().size());
        Reference reference = cancelledProcedureRequest.getRelevantHistoryFirstRep();
        assertEquals(provenanceForNewEntry.getFullUrl(), reference.getReference());

        Bundle.BundleEntryComponent provenanceForCancelledEntry = getFirstEntryOfType(newBundle, ResourceType.Provenance);
        assertTrue(isPresentInCompositionSection(newComposition, provenanceForCancelledEntry));
        Provenance provenanceForCancelled = (Provenance) provenanceForCancelledEntry.getResource();

        assertEquals(cancelledProcedureRequestEntry.getFullUrl(), provenanceForCancelled.getTargetFirstRep().getReference());
        assertEquals(cancelledProcedureRequest.getAuthoredOn(), provenanceForCancelled.getRecorded());
        assertEquals(cancelledProcedureRequest.getRequester().getAgent().getReference(), provenanceForCancelled.getAgentFirstRep().getWhoReference().getReference());
    }

    @Test
    public void shouldConvertABundleWithMedicationOrder() throws Exception {
        URL resource = this.getClass().getResource("/bundles/dstu2/bundle_with_medication_orders.xml");
        String content = FileUtils.readFileToString(new File(resource.getFile()), "UTF-8");

        String s = allResourceConverter.convertBundleToStu3(content);
        Bundle stu3Buble = (Bundle) xmlParser.parseResource(s);

        Bundle.BundleEntryComponent compositionEntry = getFirstEntryOfType(stu3Buble, ResourceType.Composition);
        Composition composition = (Composition) compositionEntry.getResource();
        assertEquals(9, composition.getSection().size());
        assertEquals(4, composition.getSection().stream().filter(
                sectionComponent -> "Medication Request".equals(sectionComponent.getEntryFirstRep().getDisplay())
        ).count());
        assertEquals(4, composition.getSection().stream().filter(
                sectionComponent ->
                        "Provenance Medication Request".equals(sectionComponent.getEntryFirstRep().getDisplay())
        ).count());

        Supplier<Stream<Bundle.BundleEntryComponent>> streamSupplier = () -> getEntriesOfType(stu3Buble, ResourceType.MedicationRequest);

        streamSupplier.get().forEach(medicationRequestEntry -> {
            assertTrue(isPresentInCompositionSection(composition, medicationRequestEntry));
            MedicationRequest medicationRequest = (MedicationRequest) medicationRequestEntry.getResource();
            assertEquals("https://mci-showcase.twhosted.com/api/default/patients/98001462467", medicationRequest.getSubject().getReference());
            assertEquals("urn:uuid:df0b047a-6b5f-4953-a37d-e95532f540aa", medicationRequest.getContext().getReference());
            assertEquals("http://hrmtest.dghs.gov.bd/api/1.0/providers/22651.json", medicationRequest.getRequester().getAgent().getReference());

            assertNotNull(medicationRequest.getAuthoredOn());
            assertEquals("Some Notes", medicationRequest.getNoteFirstRep().getText());
        });

        String newRequestWithNormalDosageFullUrl = "urn:uuid:654e0c26-3c63-4775-acaa-2be78c77781f";
        Bundle.BundleEntryComponent newRequestWithNormalDosageEntry = getEntryByFullUrl(streamSupplier, newRequestWithNormalDosageFullUrl);
        MedicationRequest newRequestWithNormalDosage = (MedicationRequest) newRequestWithNormalDosageEntry.getResource();
        assertRequestWithNormalDoses(newRequestWithNormalDosage, "NEW", null);

        String revisedRequestWithNormalDosageFullUrl = "urn:uuid:322fdd75-8bb0-4cd8-845f-51c95ffab9f0";
        Bundle.BundleEntryComponent revisedRequestWithNormalDosageEntry = getEntryByFullUrl(streamSupplier, revisedRequestWithNormalDosageFullUrl);
        MedicationRequest revisedRequestWithNormalDosage = (MedicationRequest) revisedRequestWithNormalDosageEntry.getResource();
        assertRequestWithNormalDoses(revisedRequestWithNormalDosage, "REVISE", newRequestWithNormalDosageFullUrl);

        String newRequestWithCustomDosageFullUrl = "urn:uuid:1c626241-674b-4ba3-baac-fcb7769d6555";
        Bundle.BundleEntryComponent newRequestWithCustomDosageEntry = getEntryByFullUrl(streamSupplier, newRequestWithCustomDosageFullUrl);
        MedicationRequest newRequestWithCustomDosage = (MedicationRequest) newRequestWithCustomDosageEntry.getResource();
        assertRequestWithCustomDosage(newRequestWithCustomDosage, "NEW", null);

        String stoppedRequestWithCustomDosageFullUrl = "urn:uuid:858bbf2d-4d6c-4bb0-a42a-85364afa7501";
        Bundle.BundleEntryComponent stoppedRequestWithCustomDosageEntry = getEntryByFullUrl(streamSupplier, stoppedRequestWithCustomDosageFullUrl);
        MedicationRequest stoppedRequestWithCustomDosage = (MedicationRequest) stoppedRequestWithCustomDosageEntry.getResource();
        assertRequestWithCustomDosage(stoppedRequestWithCustomDosage, "DISCONTINUE", newRequestWithCustomDosageFullUrl);

        Supplier<Stream<Bundle.BundleEntryComponent>> provenanceStreamSupplier = () -> getEntriesOfType(stu3Buble, ResourceType.Provenance);
        assertProvenanceEntry(composition, newRequestWithNormalDosageFullUrl, newRequestWithNormalDosage, provenanceStreamSupplier,
                "2017-04-06T00:00:00.000+0530", "CREATE", "create", null);
        assertProvenanceEntry(composition, revisedRequestWithNormalDosageFullUrl, revisedRequestWithNormalDosage, provenanceStreamSupplier,
                "2017-04-06T00:00:00.000+0530", "UPDATE", "revise", null);
        assertProvenanceEntry(composition, newRequestWithCustomDosageFullUrl, newRequestWithCustomDosage, provenanceStreamSupplier,
                "2017-04-06T00:00:00.000+0530", "CREATE", "create", "2017-04-04T14:41:19.000+0530");
        assertProvenanceEntry(composition, stoppedRequestWithCustomDosageFullUrl, stoppedRequestWithCustomDosage, provenanceStreamSupplier,
                "2017-04-04T14:41:20.000+0530", "ABORT", "abort", "2017-04-04T14:41:19.000+0530");
    }

    @Test
    public void shouldConvertABundleWithDiagnosticOrder() throws Exception {
        when(shrMigrationProperties.getTrValuesetUri()).thenReturn("http://tr.com/valuesets/");

        URL resource = this.getClass().getResource("/bundles/dstu2/bundle_with_diagnostic_order.xml");
        String content = FileUtils.readFileToString(new File(resource.getFile()), "UTF-8");

        String s = allResourceConverter.convertBundleToStu3(content);
        Bundle stu3Buble = (Bundle) xmlParser.parseResource(s);

        Bundle.BundleEntryComponent compositionEntry = getFirstEntryOfType(stu3Buble, ResourceType.Composition);
        Composition composition = (Composition) compositionEntry.getResource();
        assertEquals(3, composition.getSection().size());
        assertEquals(2, composition.getSection().stream().filter(
                sectionComponent -> "Procedure Request".equals(sectionComponent.getEntryFirstRep().getDisplay())
        ).count());

        Supplier<Stream<Bundle.BundleEntryComponent>> streamSupplier = () -> getEntriesOfType(stu3Buble, ResourceType.ProcedureRequest);
        streamSupplier.get().forEach(procedureRequestEntry -> {
            assertTrue(isPresentInCompositionSection(composition, procedureRequestEntry));
            ProcedureRequest procedureRequest = (ProcedureRequest) procedureRequestEntry.getResource();
            assertEquals("http://tr.com/valuesets/Order-Type", procedureRequest.getCategoryFirstRep().getCodingFirstRep().getSystem());
            assertEquals("LAB", procedureRequest.getCategoryFirstRep().getCodingFirstRep().getCode());
            assertEquals("https://mci-showcase.twhosted.com/api/default/patients/98001541849", procedureRequest.getSubject().getReference());
            assertEquals("urn:uuid:8b993f2a-f5bc-4c42-b959-1080928c08ad", procedureRequest.getContext().getReference());
            assertEquals("http://hrmtest.dghs.gov.bd/api/1.0/providers/22651.json", procedureRequest.getRequester().getAgent().getReference());
            assertNotNull(procedureRequest.getAuthoredOn());
            assertEquals(ORDER, procedureRequest.getIntent());
        });

        String fullUrl1 = "urn:uuid:b4576638-1b14-4f29-be92-dbd9c11c1609#d10a0e4e-878d-11e5-95dd-005056b0145c";
        Bundle.BundleEntryComponent entry1 = getEntryByFullUrl(streamSupplier, fullUrl1);
        ProcedureRequest procedureRequest1 = (ProcedureRequest) entry1.getResource();
        assertEquals(fullUrl1, procedureRequest1.getIdentifierFirstRep().getValue());
        assertEquals(fullUrl1, procedureRequest1.getId());
        assertEquals(2, procedureRequest1.getCode().getCoding().size());
        assertEquals("Blood grouping", procedureRequest1.getCode().getCodingFirstRep().getDisplay());

        String fullUrl2 = "urn:uuid:b4576638-1b14-4f29-be92-dbd9c11c1609#d10653d5-878d-11e5-95dd-005056b0145c";
        Bundle.BundleEntryComponent entry2 = getEntryByFullUrl(streamSupplier, fullUrl2);
        ProcedureRequest procedureRequest2 = (ProcedureRequest) entry2.getResource();
        assertEquals(fullUrl2, procedureRequest2.getIdentifierFirstRep().getValue());
        assertEquals(fullUrl2, procedureRequest2.getId());
        assertEquals(2, procedureRequest2.getCode().getCoding().size());
        assertEquals("Blood cross matching by low ionic strength saline (LISS)", procedureRequest2.getCode().getCodingFirstRep().getDisplay());
    }

    @Test
    public void shouldNotConvertCancelledDiagnosticOrder() throws Exception {
        URL resource = this.getClass().getResource("/bundles/dstu2/bundle_with_cancelled_diagnostic_order.xml");
        String content = FileUtils.readFileToString(new File(resource.getFile()), "UTF-8");

        String s = allResourceConverter.convertBundleToStu3(content);
        Bundle stu3Buble = (Bundle) xmlParser.parseResource(s);

        Bundle.BundleEntryComponent compositionEntry = getFirstEntryOfType(stu3Buble, ResourceType.Composition);
        Composition composition = (Composition) compositionEntry.getResource();
        assertEquals(1, composition.getSection().size());
        assertEquals(0, composition.getSection().stream().filter(
                sectionComponent -> "Procedure Request".equals(sectionComponent.getEntryFirstRep().getDisplay())
        ).count());
    }

    private void assertRequestWithCustomDosage(MedicationRequest medicationRequest, String expectedAction, String priorPrescription) throws FHIRException {
        List<Extension> extensions = medicationRequest.getExtensionsByUrl("https://sharedhealth.atlassian.net/wiki/display/docs/fhir-extensions#MedicationOrderAction");
        assertEquals(expectedAction, ((StringType) extensions.get(0).getValue()).getValue());

        assertEquals(MedicationRequestStatus.STOPPED, medicationRequest.getStatus());
        Coding medication = medicationRequest.getMedicationCodeableConcept().getCodingFirstRep();
        assertEquals(trSystem, medication.getSystem());
        assertEquals("d2d9213f-878d-11e5-95dd-005056b0145c", medication.getCode());

        SimpleQuantity dispenseQuantity = medicationRequest.getDispenseRequest().getQuantity();
        assertEquals(6, dispenseQuantity.getValue().intValue());
        assertEquals("Tablet dose form", dispenseQuantity.getUnit());

        Dosage dosageInstruction = medicationRequest.getDosageInstructionFirstRep();
        List<Extension> dosageInstructionExtensions = dosageInstruction.getExtensionsByUrl("https://sharedhealth.atlassian.net/wiki/display/docs/fhir-extensions#DosageInstructionCustomDosage");
        assertEquals("{\"morningDose\":1.0,\"eveningDose\":2.0}", ((StringType) dosageInstructionExtensions.get(0).getValue()).getValue());
        assertFalse(dosageInstruction.getAsNeededBooleanType().booleanValue());
        Coding route = dosageInstruction.getRoute().getCodingFirstRep();
        assertEquals(trSystem, route.getSystem());
        assertEquals("_OralRoute", route.getCode());

        SimpleQuantity doseQuantity = dosageInstruction.getDoseSimpleQuantity();
        assertEquals(trSystem, doseQuantity.getSystem());
        assertEquals("385055001", doseQuantity.getCode());
        assertEquals("Tablet dose form", doseQuantity.getUnit());

        Timing timing = dosageInstruction.getTiming();
        List<Extension> dosageInstructionTimingExtensions = timing.getExtensionsByUrl("https://sharedhealth.atlassian.net/wiki/display/docs/fhir-extensions#TimingScheduledDate");
        assertTrue(dosageInstructionTimingExtensions.get(0).getValue() instanceof DateTimeType);
        Coding code = timing.getCode().getCodingFirstRep();
        assertEquals("http://hl7.org/fhir/v3/GTSAbbreviation", code.getSystem());
        assertEquals("BID", code.getCode());
        Timing.TimingRepeatComponent repeat = timing.getRepeat();
        assertNotNull(repeat.getBounds());

        if (null != priorPrescription) {
            assertEquals(priorPrescription, medicationRequest.getPriorPrescription().getReference());
        } else {
            assertTrue(medicationRequest.getPriorPrescription().isEmpty());
        }
    }

    private void assertRequestWithNormalDoses(MedicationRequest medicationRequest, String action, String priorPrescription) throws FHIRException {
        List<Extension> extensions = medicationRequest.getExtensionsByUrl("https://sharedhealth.atlassian.net/wiki/display/docs/fhir-extensions#MedicationOrderAction");
        assertEquals(action, ((StringType) extensions.get(0).getValue()).getValue());

        assertEquals(MedicationRequestStatus.ACTIVE, medicationRequest.getStatus());
        Coding medication = medicationRequest.getMedicationCodeableConcept().getCodingFirstRep();
        assertEquals(trSystem, medication.getSystem());
        assertEquals("d2d98d73-878d-11e5-95dd-005056b0145c", medication.getCode());

        SimpleQuantity dispenseQuantity = medicationRequest.getDispenseRequest().getQuantity();
        assertEquals(2, dispenseQuantity.getValue().intValue());
        assertEquals("Teaspoonful - unit of product", dispenseQuantity.getUnit());

        Dosage dosageInstruction = medicationRequest.getDosageInstructionFirstRep();
        assertFalse(dosageInstruction.getAsNeededBooleanType().booleanValue());
        Coding route = dosageInstruction.getRoute().getCodingFirstRep();
        assertEquals(trSystem, route.getSystem());
        assertEquals("_OralRoute", route.getCode());

        SimpleQuantity doseQuantity = dosageInstruction.getDoseSimpleQuantity();
        assertEquals(trSystem, doseQuantity.getSystem());
        assertEquals("415703001", doseQuantity.getCode());
        assertEquals("Teaspoonful - unit of product", doseQuantity.getUnit());
        assertEquals(1, doseQuantity.getValue().intValue());

        Timing timing = dosageInstruction.getTiming();
        List<Extension> dosageInstructionTimingExtensions = timing.getExtensionsByUrl("https://sharedhealth.atlassian.net/wiki/display/docs/fhir-extensions#TimingScheduledDate");
        assertTrue(dosageInstructionTimingExtensions.get(0).getValue() instanceof DateTimeType);
        Timing.TimingRepeatComponent repeat = timing.getRepeat();
        assertEquals(2, repeat.getFrequency());
        assertEquals(1, repeat.getPeriod().intValue());
        assertEquals(D, repeat.getPeriodUnit());
        assertNotNull(repeat.getBounds());

        if (null != priorPrescription) {
            assertEquals(priorPrescription, medicationRequest.getPriorPrescription().getReference());
        } else {
            assertTrue(medicationRequest.getPriorPrescription().isEmpty());
        }
    }

    private Bundle.BundleEntryComponent getEntryByFullUrl(Supplier<Stream<Bundle.BundleEntryComponent>> streamSupplier, String fullUrl) {
        return streamSupplier.get().filter(bundleEntryComponent -> fullUrl.equals(bundleEntryComponent.getFullUrl())).findFirst().get();
    }

    private Resource findObservationByName(Stream<Bundle.BundleEntryComponent> observationEntries, String name) {
        return observationEntries.filter(
                entry -> name.equals(((Observation) entry.getResource()).getCode().getCodingFirstRep().getDisplay())
        ).findFirst().get().getResource();
    }

    private boolean isPresentInCompositionSection(Composition composition, Bundle.BundleEntryComponent encounterEntry) {
        return composition.getSection().stream().anyMatch(sectionComponent -> sectionComponent.getEntryFirstRep().getReference().equals(encounterEntry.getFullUrl()));
    }

    private Bundle.BundleEntryComponent getFirstEntryOfType(Bundle stu3Buble, ResourceType resourceType) {
        return getEntriesOfType(stu3Buble, resourceType).findFirst().orElse(null);
    }

    private Stream<Bundle.BundleEntryComponent> getEntriesOfType(Bundle stu3Buble, ResourceType resourceType) {
        return stu3Buble.getEntry().stream().filter(component ->
                component.getResource().getResourceType().equals(resourceType)
        );
    }

    private void assertProvenanceEntry(Composition composition, String fullUrl, MedicationRequest medicationRequest,
                                       Supplier<Stream<Bundle.BundleEntryComponent>> provenanceStreamSupplier,
                                       String startDate, String activityCode, String activityDisplay, Object endDate) throws FHIRException {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZZ");

        Bundle.BundleEntryComponent provenanceEntry = getEntryByFullUrl(provenanceStreamSupplier, buildProvenanceEntryURL(fullUrl));
        Provenance provenance = (Provenance) provenanceEntry.getResource();
        assertTrue(isPresentInCompositionSection(composition, provenanceEntry));
        assertEquals(fullUrl, provenance.getTargetFirstRep().getReference());

        Coding activity = provenance.getActivity();
        assertEquals("http://hl7.org/fhir/v3/DataOperation", activity.getSystem());
        assertEquals(activityCode, activity.getCode());
        assertEquals(activityDisplay, activity.getDisplay());
        assertEquals(medicationRequest.getAuthoredOn(), provenance.getRecorded());
        assertEquals(medicationRequest.getRequester().getAgent().getReference(), provenance.getAgentFirstRep().getWhoReference().getReference());

        assertEquals(startDate, simpleDateFormat.format(provenance.getPeriod().getStart()));
        if (null == endDate) {
            assertNull(provenance.getPeriod().getEnd());
        } else {
            assertEquals(endDate, simpleDateFormat.format(provenance.getPeriod().getEnd()));
        }
    }
}
