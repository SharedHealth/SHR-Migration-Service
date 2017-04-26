package org.sharedhealth.migrationservice.converter;

import ca.uhn.fhir.model.dstu2.composite.CodeableConceptDt;
import ca.uhn.fhir.model.dstu2.composite.CodingDt;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;
import org.sharedhealth.migrationservice.config.SHRMigrationProperties;

import java.util.List;

public class FhirBundleUtil {

    private static final String TR_CONCEPT_URI_PART = "/tr/concepts/";

    public static String buildProvenanceEntryURL(String fullUrl) {
        return String.format("%s-provenance", fullUrl);
    }

    public static CodeableConcept convertCode(CodeableConceptDt code) {
        CodeableConcept codeableConcept = new CodeableConcept();
        for (CodingDt codingDt : code.getCoding()) {
            codeableConcept.addCoding(new Coding(codingDt.getSystem(), codingDt.getCode(), codingDt.getDisplay()));
        }
        return codeableConcept;
    }

    public static CodingDt getConceptCodingDt(List<CodingDt> codings) {
        return codings.stream().filter(
                codingDt -> StringUtils.isNotBlank(codingDt.getSystem()) && codingDt.getSystem().contains(TR_CONCEPT_URI_PART)
        ).findFirst().orElse(null);
    }

    public static Coding getConceptCoding(List<Coding> codings) {
        return codings.stream().filter(
                coding -> StringUtils.isNotBlank(coding.getSystem()) && coding.getSystem().contains(TR_CONCEPT_URI_PART)
        ).findFirst().orElse(null);
    }

    public static String getTrValuesetUrl(SHRMigrationProperties migrationProperties, String valuesetName) {
        return String.format("%s%s", migrationProperties.getTrValuesetUri(), valuesetName);
    }


}
