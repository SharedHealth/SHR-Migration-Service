package org.sharedhealth.migrationService.converter;

import ca.uhn.fhir.model.dstu2.composite.CodeableConceptDt;
import ca.uhn.fhir.model.dstu2.composite.CodingDt;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;

public class FhirBundleUtil {
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
}
