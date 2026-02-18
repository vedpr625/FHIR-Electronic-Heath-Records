package com.emr.fhir.dto;
import lombok.Data; import lombok.NoArgsConstructor; import lombok.AllArgsConstructor;
@Data @NoArgsConstructor @AllArgsConstructor
public class AllergyIntoleranceDTO {
    private String id;
    private String patientId, patientName;
    private String practitionerId, practitionerName;
    // What they're allergic to
    private String code, display;           // allergen code/name
    private String category;               // food, medication, environment, biologic
    private String type;                   // allergy, intolerance
    private String criticality;            // low, high, unable-to-assess
    private String clinicalStatus;         // active, inactive, resolved
    private String verificationStatus;     // confirmed, unconfirmed, refuted, entered-in-error
    // Reaction details
    private String reactionSubstance;      // specific substance that caused reaction
    private String reactionManifestation;  // e.g., hives, anaphylaxis, rash
    private String reactionSeverity;       // mild, moderate, severe
    private String reactionDescription;    // free-text description
    private String exposureRoute;          // oral, intravenous, topical, etc.
    private String recorderId;             // practitioner who recorded the allergy
    private String onsetDate;
    private String lastOccurrence;
    private String notes;
    private String recordedDate;
}
