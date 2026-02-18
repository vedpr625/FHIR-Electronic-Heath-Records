package com.emr.fhir.dto;
import lombok.Data; import lombok.NoArgsConstructor; import lombok.AllArgsConstructor;
@Data @NoArgsConstructor @AllArgsConstructor
public class OrganizationDTO {
    private String id, name, type, phone, email;
    private String addressLine, city, state, postalCode, country;
    private boolean active = true;
}
