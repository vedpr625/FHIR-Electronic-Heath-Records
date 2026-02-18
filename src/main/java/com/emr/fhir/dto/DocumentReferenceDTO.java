package com.emr.fhir.dto;
import lombok.Data; import lombok.NoArgsConstructor; import lombok.AllArgsConstructor;
@Data @NoArgsConstructor @AllArgsConstructor
public class DocumentReferenceDTO {
    private String id;
    private String patientId, patientName;
    private String practitionerId, practitionerName;
    private String encounterId;
    // Document metadata
    private String type;                 // discharge-summary, clinical-note, lab-report, imaging-report, consent, referral, operative-note, etc.
    private String category;             // clinical-note, imaging, lab, administrative
    private String title;
    private String status;               // current, superseded, entered-in-error
    private String docStatus;            // preliminary, final, amended, entered-in-error
    private String date;                 // creation date
    private String description;
    // Content
    private String contentType;          // application/pdf, text/plain, image/jpeg
    private String url;                  // external URL or stored path
    private String content;              // embedded text content (for plain text docs)
    private String language;
    // Security / context
    private String securityLabel;        // restricted, normal
    private String facilityType;
    private String notes;
}
