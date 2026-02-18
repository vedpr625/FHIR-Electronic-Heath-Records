package com.emr.fhir.service;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.emr.fhir.dto.ConditionDTO;
import org.hl7.fhir.r4.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.text.*;
import java.util.*;
@Service
public class ConditionService {
    @Autowired private IGenericClient fhirClient;
    @Autowired private AuditService auditService;
    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

    public List<ConditionDTO> getConditionsForPatient(String patientId) {
        Bundle b = fhirClient.search().forResource(Condition.class)
            .where(Condition.PATIENT.hasId(patientId)).returnBundle(Bundle.class).execute();
        List<ConditionDTO> list = new ArrayList<>();
        for (Bundle.BundleEntryComponent e : b.getEntry()) if (e.getResource() instanceof Condition c) list.add(toDTO(c));
        return list;
    }
    public List<ConditionDTO> getAllConditions() {
        Bundle b = fhirClient.search().forResource(Condition.class).count(100).returnBundle(Bundle.class).execute();
        List<ConditionDTO> list = new ArrayList<>();
        for (Bundle.BundleEntryComponent e : b.getEntry()) if (e.getResource() instanceof Condition c) list.add(toDTO(c));
        return list;
    }
    public ConditionDTO getConditionById(String id) { return toDTO(fhirClient.read().resource(Condition.class).withId(id).execute()); }
    public ConditionDTO createCondition(ConditionDTO dto) {
        MethodOutcome out = fhirClient.create().resource(toResource(dto)).execute();
        dto.setId(out.getId().getIdPart());
        auditService.logEvent("CREATE", "Condition", dto.getId(), "Diagnosis created: " + dto.getDisplay() + " for patient " + dto.getPatientId());
        return dto;
    }
    public ConditionDTO updateCondition(ConditionDTO dto) {
        Condition c = toResource(dto); c.setId(dto.getId());
        fhirClient.update().resource(c).execute();
        auditService.logEvent("UPDATE", "Condition", dto.getId(), "Diagnosis updated: " + dto.getDisplay());
        return dto;
    }
    public void deleteCondition(String id) { fhirClient.delete().resourceById("Condition", id).execute(); auditService.logEvent("DELETE", "Condition", id, "Diagnosis deleted: " + id); }

    public ConditionDTO toDTO(Condition c) {
        ConditionDTO dto = new ConditionDTO();
        dto.setId(c.getIdElement().getIdPart());
        if (c.getSubject() != null) { String ref = c.getSubject().getReference(); if (ref != null) dto.setPatientId(ref.replace("Patient/", "")); dto.setPatientName(c.getSubject().getDisplay()); }
        if (!c.getCode().getCoding().isEmpty()) { dto.setCode(c.getCode().getCodingFirstRep().getCode()); dto.setDisplay(c.getCode().getCodingFirstRep().getDisplay()); }
        if (dto.getDisplay() == null && c.getCode().getText() != null) dto.setDisplay(c.getCode().getText());
        if (!c.getCategory().isEmpty() && !c.getCategory().get(0).getCoding().isEmpty()) dto.setCategory(c.getCategory().get(0).getCodingFirstRep().getCode());
        if (c.getClinicalStatus() != null && !c.getClinicalStatus().getCoding().isEmpty()) dto.setClinicalStatus(c.getClinicalStatus().getCodingFirstRep().getCode());
        if (c.getVerificationStatus() != null && !c.getVerificationStatus().getCoding().isEmpty()) dto.setVerificationStatus(c.getVerificationStatus().getCodingFirstRep().getCode());
        if (c.getSeverity() != null && !c.getSeverity().getCoding().isEmpty()) dto.setSeverity(c.getSeverity().getCodingFirstRep().getCode());
        if (c.getOnset() instanceof DateTimeType dt && dt.getValue() != null) dto.setOnsetDate(sdf.format(dt.getValue()));
        if (c.getAbatement() instanceof DateTimeType dt && dt.getValue() != null) dto.setAbatementDate(sdf.format(dt.getValue()));
        if (c.getRecordedDate() != null) dto.setRecordedDate(sdf.format(c.getRecordedDate()));
        if (!c.getNote().isEmpty()) dto.setNotes(c.getNote().get(0).getText());
        if (c.getRecorder() != null) { String ref = c.getRecorder().getReference(); if (ref != null) dto.setPractitionerId(ref.replace("Practitioner/", "")); dto.setPractitionerName(c.getRecorder().getDisplay()); }
        return dto;
    }
    private Condition toResource(ConditionDTO dto) {
        Condition c = new Condition();
        c.getSubject().setReference("Patient/" + dto.getPatientId());
        if (dto.getPatientName() != null) c.getSubject().setDisplay(dto.getPatientName());
        if (dto.getCode() != null && !dto.getCode().isEmpty()) c.getCode().addCoding().setSystem("http://hl7.org/fhir/sid/icd-10").setCode(dto.getCode()).setDisplay(dto.getDisplay());
        if (dto.getDisplay() != null) c.getCode().setText(dto.getDisplay());
        if (dto.getCategory() != null && !dto.getCategory().isEmpty()) c.addCategory().addCoding().setSystem("http://terminology.hl7.org/CodeSystem/condition-category").setCode(dto.getCategory());
        if (dto.getClinicalStatus() != null && !dto.getClinicalStatus().isEmpty()) c.getClinicalStatus().addCoding().setSystem("http://terminology.hl7.org/CodeSystem/condition-clinical").setCode(dto.getClinicalStatus());
        if (dto.getVerificationStatus() != null && !dto.getVerificationStatus().isEmpty()) c.getVerificationStatus().addCoding().setSystem("http://terminology.hl7.org/CodeSystem/condition-ver-status").setCode(dto.getVerificationStatus());
        if (dto.getSeverity() != null && !dto.getSeverity().isEmpty()) c.getSeverity().addCoding().setSystem("http://snomed.info/sct").setCode(dto.getSeverity()).setDisplay(dto.getSeverity());
        if (dto.getOnsetDate() != null && !dto.getOnsetDate().isEmpty()) { try { c.setOnset(new DateTimeType(sdf.parse(dto.getOnsetDate()))); } catch (ParseException ignored) {} }
        c.setRecordedDate(new Date());
        if (dto.getNotes() != null && !dto.getNotes().isEmpty()) c.addNote().setText(dto.getNotes());
        if (dto.getPractitionerId() != null && !dto.getPractitionerId().isEmpty()) c.getRecorder().setReference("Practitioner/" + dto.getPractitionerId()).setDisplay(dto.getPractitionerName());
        return c;
    }
}
