package com.emr.fhir.dto;
import lombok.Data; import lombok.NoArgsConstructor; import lombok.AllArgsConstructor;
@Data @NoArgsConstructor @AllArgsConstructor
public class ServiceRequestDTO {
    private String id;
    private String patientId, patientName;
    private String practitionerId, practitionerName;
    private String encounterId;
    private String conditionId, conditionDisplay;
    // Request details
    private String code, display;          // LOINC/SNOMED code + test name
    private String category;              // laboratory, imaging, procedure, counselling, education
    private String intent;                // proposal, plan, directive, order, original-order, reflex-order, filler-order, instance-order
    private String priority;             // routine, urgent, asap, stat
    private String status;               // draft, active, on-hold, revoked, completed, entered-in-error, unknown
    private String orderDetail;          // specific instructions e.g. "fasting", "contrast"
    private String authoredDate;
    private String occurrenceDate;       // when to perform
    private String reasonCode;           // clinical reason / indication
    private String specimen;             // blood, urine, biopsy etc.
    private String bodySite;
    private String notes;
    // Result tracking
    private String resultStatus;         // pending, received, reviewed
    private String resultNotes;
}
