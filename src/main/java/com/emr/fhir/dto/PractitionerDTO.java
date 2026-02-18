package com.emr.fhir.dto;
import lombok.Data; import lombok.NoArgsConstructor; import lombok.AllArgsConstructor;
@Data @NoArgsConstructor @AllArgsConstructor
public class PractitionerDTO {
    private String id;
    private String firstName, lastName, specialization, licenseNumber;
    private String phone, email, department;
    private boolean active = true;
    public String getFullName() { return (firstName != null ? firstName : "") + " " + (lastName != null ? lastName : ""); }
}
