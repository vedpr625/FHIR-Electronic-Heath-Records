package com.emr.fhir.dto;
import lombok.Data; import lombok.NoArgsConstructor; import lombok.AllArgsConstructor;
@Data @NoArgsConstructor @AllArgsConstructor
public class EncounterDTO {
    private String id;
    private String patientId, patientName;
    private String practitionerId, practitionerName;
    private String appointmentId;
    private String status;       // planned, arrived, triaged, in-progress, onleave, finished, cancelled
    private String classCode;    // AMB=ambulatory, IMP=inpatient, EMER=emergency, HH=home health
    private String type;         // consultation, follow-up, emergency, routine, etc.
    private String reasonCode;   // chief complaint
    private String startDate, endDate;
    private String notes;
    private String organizationId, organizationName;
    // Linked clinical data
    private String conditionId, conditionDisplay;
    private String prescriptionId;
}
