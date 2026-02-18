package com.emr.fhir.dto;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PatientDTO {
    private String id;
    private String firstName, lastName, birthDate, gender;
    private String phone, email;
    private String addressLine, city, state, postalCode, country;
    private String mrn, bloodGroup, photoUrl;
    private String emergencyName, emergencyPhone, emergencyRelationship;
    private String insuranceProvider, insurancePolicyNumber;
    private boolean active = true;

    public String getFullName() {
        return (firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "");
    }

    /**
     * Calculate age from birthDate
     * Returns format: "45y" or "2m" or "15d" for infants
     */
    public String getAge() {
        if (birthDate == null || birthDate.isEmpty()) return null;
        try {
            // Parse birthDate (format: yyyy-MM-dd or yyyy-MM-dd'T'HH:mm:ss)
            LocalDate birth = LocalDate.parse(birthDate.substring(0, 10), DateTimeFormatter.ISO_DATE);
            LocalDate now = LocalDate.now();
            Period period = Period.between(birth, now);
            
            int years = period.getYears();
            int months = period.getMonths();
            int days = period.getDays();
            
            if (years > 0) {
                return years + "y";
            } else if (months > 0) {
                return months + "m";
            } else {
                return days + "d";
            }
        } catch (Exception e) {
            return null;
        }
    }
}
