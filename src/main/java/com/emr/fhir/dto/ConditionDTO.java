package com.emr.fhir.dto;
import lombok.Data; import lombok.NoArgsConstructor; import lombok.AllArgsConstructor;
@Data @NoArgsConstructor @AllArgsConstructor
public class ConditionDTO {
    private String id;
    private String patientId, patientName;
    private String practitionerId, practitionerName;
    private String code;          // ICD-10 code
    private String display;       // Human-readable diagnosis name
    private String category;      // encounter-diagnosis, problem-list-item, etc.
    private String clinicalStatus; // active, recurrence, relapse, inactive, remission, resolved
    private String verificationStatus; // unconfirmed, provisional, differential, confirmed
    private String severity;      // mild, moderate, severe
    private String onsetDate;
    private String abatementDate;
    private String recordedDate;
    private String notes;
}
