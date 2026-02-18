package com.emr.fhir.dto;
import lombok.Data; import lombok.NoArgsConstructor; import lombok.AllArgsConstructor;
@Data @NoArgsConstructor @AllArgsConstructor
public class MedicationRequestDTO {
    private String id;
    private String patientId, patientName;
    private String practitionerId, practitionerName;
    private String medicationCode, medicationDisplay; // drug name / code
    private String genericName;
    private String form;       // Tablet, Capsule, Solution, Injection, etc.
    private String status;    // active, on-hold, cancelled, completed, stopped
    private String intent;    // proposal, plan, order, original-order
    private String priority;  // routine, urgent, asap, stat
    private String dosage;    // simple dosage field (e.g., "500 mg")
    private String dosageText;
    private String doseQuantity, doseUnit;
    private String route;     // oral, topical, intravenous, etc.
    private String frequency; // once daily, twice daily, etc.
    private String duration;
    private String strength;
    private Integer quantity;
    private Integer refills;
    private String startDate, endDate;
    private String instructions;
    private String conditionId; // linked condition/diagnosis
    private String conditionDisplay;
    private String authoredOn;
    private String requesterId;    // practitioner who prescribed (may differ from practitionerId)
    private String dispensePerformer; // pharmacy or facility dispensing the medication
    private String reasonReference;   // reason for the medication
    private String notes;
}
