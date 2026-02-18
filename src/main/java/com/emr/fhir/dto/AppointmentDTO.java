package com.emr.fhir.dto;
import lombok.Data; import lombok.NoArgsConstructor; import lombok.AllArgsConstructor;
@Data @NoArgsConstructor @AllArgsConstructor
public class AppointmentDTO {
    private String id, patientId, patientName, practitionerId, practitionerName;
    private String start, end, status, description, serviceType;
    private Integer minutesDuration;
}
