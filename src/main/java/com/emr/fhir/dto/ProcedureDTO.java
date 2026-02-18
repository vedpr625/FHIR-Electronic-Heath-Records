package com.emr.fhir.dto;
import lombok.Data; import lombok.NoArgsConstructor; import lombok.AllArgsConstructor;
@Data @NoArgsConstructor @AllArgsConstructor
public class ProcedureDTO {
    private String id;
    private String patientId, patientName;
    private String practitionerId, practitionerName;
    private String encounterId;
    private String conditionId, conditionDisplay;
    // Procedure details
    private String code, display;          // SNOMED/CPT code + name
    private String category;               // surgical, diagnostic, therapeutic, counselling, etc.
    private String status;                 // preparation, in-progress, not-done, on-hold, stopped, completed, entered-in-error, unknown
    private String statusReason;           // reason if not-done/stopped
    private String performedDate;          // when performed
    private String performedEnd;
    private String bodySite;               // e.g., Left knee, Abdomen
    private String outcome;                // outcome/result
    private String complication;           // any complications
    private String followUp;               // follow-up instructions
    private String notes;
    private String location;               // OT, ICU, Ward, etc.
    private String reasonCode;             // why was this done
}
