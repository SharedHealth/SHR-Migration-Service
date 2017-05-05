package org.sharedhealth.migrationservice.converter;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;
import org.sharedhealth.migrationservice.config.SHRMigrationProperties;

import java.util.List;
import java.util.stream.Collectors;

public class FhirBundleUtil {

    private static final String TR_CONCEPT_URI_PART = "/tr/concepts/";

    public static String buildProvenanceEntryURL(String fullUrl) {
        return String.format("%s-provenance", fullUrl);
    }

    public static CodeableConcept convertCode(org.hl7.fhir.dstu2.model.CodeableConcept code) {
        CodeableConcept codeableConcept = new CodeableConcept();
        for (org.hl7.fhir.dstu2.model.Coding coding : code.getCoding()) {
            codeableConcept.addCoding(new Coding(coding.getSystem(), coding.getCode(), coding.getDisplay()));
        }
        return codeableConcept;
    }

    public static org.hl7.fhir.dstu2.model.Coding getConceptCodingForDSTU2(List<org.hl7.fhir.dstu2.model.Coding> codings) {
        return codings.stream().filter(
                codingDt -> StringUtils.isNotBlank(codingDt.getSystem()) && codingDt.getSystem().contains(TR_CONCEPT_URI_PART)
        ).findFirst().orElse(null);
    }

    public static Coding getConceptCodingForSTU3(List<Coding> codings) {
        return codings.stream().filter(
                coding -> StringUtils.isNotBlank(coding.getSystem()) && coding.getSystem().contains(TR_CONCEPT_URI_PART)
        ).findFirst().orElse(null);
    }

    public static String getTrValuesetUrl(SHRMigrationProperties migrationProperties, String valuesetName) {
        return String.format("%s%s", migrationProperties.getTrValuesetUri(), valuesetName);
    }

    public static List<org.hl7.fhir.dstu2.model.Extension> getExtensionByUrl(org.hl7.fhir.dstu2.model.DomainResource resource, String url) {
        return resource.getExtension().stream().filter(
                extension -> url.equals(extension.getUrl())
        ).collect(Collectors.toList());
    }

}
