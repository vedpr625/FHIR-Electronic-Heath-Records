package com.emr.fhir.dto;
import lombok.Data; import lombok.NoArgsConstructor; import lombok.AllArgsConstructor;
@Data @NoArgsConstructor @AllArgsConstructor
public class FamilyHistoryDTO {
    private String id;
    private String patientId, patientName;
    // Family member
    private String relationship;       // father, mother, sibling, child, etc.
    private String sex;                // male, female, unknown
    private String bornDate;
    private String deceasedBoolean;    // "true"/"false"
    private String deceasedAge;
    // Condition
    private String conditionCode;      // ICD-10 or SNOMED code
    private String conditionDisplay;   // Human readable condition name
    private String conditionOnsetAge;  // age of onset in family member
    private String conditionNote;
    // Status
    private String status;             // partial, completed, health-unknown, error
    private String dataAbsentReason;
    private String date;               // when this history was recorded
    private String notes;
}
