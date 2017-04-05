package org.sharedhealth.migrationService.converter;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import org.apache.commons.io.FileUtils;
import org.hl7.fhir.dstu3.model.*;
import org.hl7.fhir.dstu3.model.MedicationRequest.MedicationRequestStatus;
import org.hl7.fhir.exceptions.FHIRException;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.hl7.fhir.dstu3.model.Condition.ConditionClinicalStatus.ACTIVE;
import static org.hl7.fhir.dstu3.model.Condition.ConditionVerificationStatus.CONFIRMED;
import static org.hl7.fhir.dstu3.model.Condition.ConditionVerificationStatus.PROVISIONAL;
import static org.hl7.fhir.dstu3.model.Encounter.EncounterStatus.FINISHED;
import static org.hl7.fhir.dstu3.model.Observation.ObservationStatus.PRELIMINARY;
import static org.hl7.fhir.dstu3.model.ProcedureRequest.ProcedureRequestStatus.SUSPENDED;
import static org.hl7.fhir.dstu3.model.Timing.UnitsOfTime.D;
import static org.junit.Assert.*;

public class AllResourceConverterTest {
    private final String trSystem = "http://tr.com";
    private final String trCode = "answerCode";
    private final String trDisplay = "answerDisplay";
    private static AllResourceConverter allResourceConverter;

    IParser xmlParser = FhirContext.forDstu3().newXmlParser();

    @BeforeClass
    public static void setUp() throws Exception {
        allResourceConverter = new AllResourceConverter();
    }

    @Test
    public void shouldConvertAnBundleWithJustEncounter() throws Exception {
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
        assertEquals("http://www.mci.com/patients/98104750156", encounter.getSubject().getReference());
        assertEquals("http://www.pr.com/providers/812.json", encounter.getParticipantFirstRep().getIndividual().getReference());
        assertEquals("http://www.fr.com/facilities/10019841.json", encounter.getServiceProvider().getReference());
        assertNotNull(encounter.getPeriod());
    }

    @Test
    public void shouldConvertAnBundleWithComplaint() throws Exception {
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
        assertEquals("http://pr.com/api/1.0/providers/812.json", observation.getPerformerFirstRep().getReference());

        assertEquals("http://shr.com/patients/hid/encounters/shrEncounterId3", diagnosticReport.getBasedOnFirstRep().getReference());
        assertEquals("http://pr.com/api/1.0/providers/812.json", diagnosticReport.getPerformerFirstRep().getActor().getReference());
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

        //this notes conversion will work just for conditions having dignosis from TR
        assertEquals("Some notes", conditionComponent.getNoteFirstRep().getText());
        assertTrue(conditionComponent.hasOnset());
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
    public void shouldConvertABundleWithProcedureFullfilment() throws Exception {
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
        URL resource = this.getClass().getResource("/bundles/dstu2/bundle_with_procedure_request.xml");
        String content = FileUtils.readFileToString(new File(resource.getFile()), "UTF-8");

        String s = allResourceConverter.convertBundleToStu3(content);
        Bundle stu3Buble = (Bundle) xmlParser.parseResource(s);

        Bundle.BundleEntryComponent compositionEntry = getFirstEntryOfType(stu3Buble, ResourceType.Composition);
        Composition composition = (Composition) compositionEntry.getResource();
        assertEquals(3, composition.getSection().size());

        Supplier<Stream<Bundle.BundleEntryComponent>> streamSupplier = () -> getEntriesOfType(stu3Buble, ResourceType.ProcedureRequest);
        streamSupplier.get().forEach(procedureRequestEntry -> {
            assertTrue(isPresentInCompositionSection(composition, procedureRequestEntry));
            ProcedureRequest procedureRequest = (ProcedureRequest) procedureRequestEntry.getResource();
            assertEquals("http://172.18.46.199:8081/api/v1/patients/98001175044", procedureRequest.getSubject().getReference());
            assertEquals("urn:uuid:763dee64-44d5-4820-b9c0-6c51bf1d3fa9", procedureRequest.getContext().getReference());
            assertEquals("http://172.18.46.199:8084/api/1.0/providers/24.json", procedureRequest.getRequester().getAgent().getReference());

            Coding code = procedureRequest.getCode().getCodingFirstRep();
            assertEquals(trSystem, code.getSystem());
            assertEquals("101", code.getCode());

            assertNotNull(procedureRequest.getAuthoredOn());
            assertEquals("Some Notes", procedureRequest.getNoteFirstRep().getText());
        });

        ProcedureRequest suspendedRequest = (ProcedureRequest) streamSupplier.get().filter(
                entry -> SUSPENDED.equals(((ProcedureRequest) entry.getResource()).getStatus()))
                .findFirst().get().getResource();
        List<Extension> extensions = suspendedRequest.getExtensionsByUrl("https://sharedhealth.atlassian.net/wiki/display/docs/fhir-extensions#PreviousProcedureRequest");
        assertEquals(1, extensions.size());
    }

    @Test
    public void shouldConvertABundleWithMedicationOrder() throws Exception {
        URL resource = this.getClass().getResource("/bundles/dstu2/bundle_with_medication_orders.xml");
        String content = FileUtils.readFileToString(new File(resource.getFile()), "UTF-8");

        String s = allResourceConverter.convertBundleToStu3(content);
        Bundle stu3Buble = (Bundle) xmlParser.parseResource(s);

        Bundle.BundleEntryComponent compositionEntry = getFirstEntryOfType(stu3Buble, ResourceType.Composition);
        Composition composition = (Composition) compositionEntry.getResource();
        assertEquals(4, composition.getSection().size());
        assertEquals(3, composition.getSection().stream().filter(
                sectionComponent -> "Medication Request".equals(sectionComponent.getEntryFirstRep().getDisplay())
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

        Bundle.BundleEntryComponent newRequestWithNormalDosageEntry = getEntryByFullUrl(streamSupplier, "urn:uuid:654e0c26-3c63-4775-acaa-2be78c77781f");
        MedicationRequest newRequestWithNormalDosage = (MedicationRequest) newRequestWithNormalDosageEntry.getResource();
        assertNewRequestWithNormalDoses(newRequestWithNormalDosage);

        Bundle.BundleEntryComponent newRequestWithCustomDosageEntry = getEntryByFullUrl(streamSupplier, "urn:uuid:1c626241-674b-4ba3-baac-fcb7769d6555");
        MedicationRequest newRequestWithCustomDosage = (MedicationRequest) newRequestWithCustomDosageEntry.getResource();
        assertRequestWithCustomDosage(newRequestWithCustomDosage, "NEW");

        Bundle.BundleEntryComponent stoppedRequestWithCustomDosageEntry = getEntryByFullUrl(streamSupplier, "urn:uuid:858bbf2d-4d6c-4bb0-a42a-85364afa7501");
        MedicationRequest stoppedRequestWithCustomDosage = (MedicationRequest) stoppedRequestWithCustomDosageEntry.getResource();
        assertRequestWithCustomDosage(stoppedRequestWithCustomDosage, "DISCONTINUE");
        assertEquals(stoppedRequestWithCustomDosage.getPriorPrescription().getReference(), newRequestWithCustomDosageEntry.getFullUrl());
    }

    @Test
    public void shouldConvertABundleWithDiagnosticOrder() throws Exception {
        URL resource = this.getClass().getResource("/bundles/dstu2/bundle_with_diagnostic_order.xml");
        String content = FileUtils.readFileToString(new File(resource.getFile()), "UTF-8");

        String s = allResourceConverter.convertBundleToStu3(content);
        System.out.println(s);
        Bundle stu3Buble = (Bundle) xmlParser.parseResource(s);

        Bundle.BundleEntryComponent compositionEntry = getFirstEntryOfType(stu3Buble, ResourceType.Composition);
        Composition composition = (Composition) compositionEntry.getResource();
        assertEquals(3, composition.getSection().size());
    }

    private void assertRequestWithCustomDosage(MedicationRequest newRequestWithCustomDosage, String expectedAction) throws FHIRException {
        List<Extension> extensions = newRequestWithCustomDosage.getExtensionsByUrl("https://sharedhealth.atlassian.net/wiki/display/docs/fhir-extensions#MedicationOrderAction");
        assertEquals(expectedAction, ((StringType) extensions.get(0).getValue()).getValue());

        assertEquals(MedicationRequestStatus.STOPPED, newRequestWithCustomDosage.getStatus());
        Coding medication = newRequestWithCustomDosage.getMedicationCodeableConcept().getCodingFirstRep();
        assertEquals(trSystem, medication.getSystem());
        assertEquals("d2d9213f-878d-11e5-95dd-005056b0145c", medication.getCode());

        SimpleQuantity dispenseQuantity = newRequestWithCustomDosage.getDispenseRequest().getQuantity();
        assertEquals(6, dispenseQuantity.getValue().intValue());
        assertEquals("Tablet dose form", dispenseQuantity.getUnit());

        Dosage dosageInstruction = newRequestWithCustomDosage.getDosageInstructionFirstRep();
        List<Extension> dosageInstructionExtensions = dosageInstruction.getExtensionsByUrl("https://sharedhealth.atlassian.net/wiki/display/docs/fhir-extensions#DosageInstructionCustomDosage");
        assertEquals("{\"morningDose\":1.0,\"eveningDose\":2.0}",((StringType)dosageInstructionExtensions.get(0).getValue()).getValue());
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
    }

    private void assertNewRequestWithNormalDoses(MedicationRequest newRequestWithNormalDosage) throws FHIRException {
        List<Extension> extensions = newRequestWithNormalDosage.getExtensionsByUrl("https://sharedhealth.atlassian.net/wiki/display/docs/fhir-extensions#MedicationOrderAction");
        assertEquals("NEW", ((StringType) extensions.get(0).getValue()).getValue());

        assertEquals(MedicationRequestStatus.ACTIVE, newRequestWithNormalDosage.getStatus());
        Coding medication = newRequestWithNormalDosage.getMedicationCodeableConcept().getCodingFirstRep();
        assertEquals(trSystem, medication.getSystem());
        assertEquals("d2d98d73-878d-11e5-95dd-005056b0145c", medication.getCode());

        SimpleQuantity dispenseQuantity = newRequestWithNormalDosage.getDispenseRequest().getQuantity();
        assertEquals(2, dispenseQuantity.getValue().intValue());
        assertEquals("Teaspoonful - unit of product", dispenseQuantity.getUnit());

        Dosage dosageInstruction = newRequestWithNormalDosage.getDosageInstructionFirstRep();
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
        return stu3Buble.getEntry().stream().filter(component -> component.getResource().getResourceType().equals(resourceType));
    }
}
