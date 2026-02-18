package com.emr.fhir.service;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.emr.fhir.dto.FamilyHistoryDTO;
import org.hl7.fhir.r4.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class FamilyHistoryService {
    @Autowired private IGenericClient fhirClient;
    @Autowired private AuditService auditService;
    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

    public List<FamilyHistoryDTO> getHistoryForPatient(String patientId) {
        Bundle b = fhirClient.search().forResource(FamilyMemberHistory.class)
            .where(FamilyMemberHistory.PATIENT.hasId(patientId))
            .returnBundle(Bundle.class).execute();
        List<FamilyHistoryDTO> list = new ArrayList<>();
        for (Bundle.BundleEntryComponent e : b.getEntry())
            if (e.getResource() instanceof FamilyMemberHistory f) list.add(toDTO(f));
        return list;
    }

    public FamilyHistoryDTO getHistoryById(String id) {
        return toDTO(fhirClient.read().resource(FamilyMemberHistory.class).withId(id).execute());
    }

    public FamilyHistoryDTO createHistory(FamilyHistoryDTO dto) {
        MethodOutcome out = fhirClient.create().resource(toResource(dto)).execute();
        dto.setId(out.getId().getIdPart());
        auditService.logEvent("CREATE", "FamilyMemberHistory", dto.getId(),
            "Family history: " + dto.getRelationship() + " - " + dto.getConditionDisplay() + " for patient " + dto.getPatientId());
        return dto;
    }

    public FamilyHistoryDTO updateHistory(FamilyHistoryDTO dto) {
        FamilyMemberHistory f = toResource(dto); f.setId(dto.getId());
        fhirClient.update().resource(f).execute();
        auditService.logEvent("UPDATE", "FamilyMemberHistory", dto.getId(), "Updated family history");
        return dto;
    }

    public void deleteHistory(String id) {
        fhirClient.delete().resourceById("FamilyMemberHistory", id).execute();
        auditService.logEvent("DELETE", "FamilyMemberHistory", id, "Deleted family history: " + id);
    }

    public FamilyHistoryDTO toDTO(FamilyMemberHistory f) {
        FamilyHistoryDTO dto = new FamilyHistoryDTO();
        dto.setId(f.getIdElement().getIdPart());
        if (f.getPatient() != null && f.getPatient().getReference() != null) { dto.setPatientId(f.getPatient().getReference().replace("Patient/","")); dto.setPatientName(f.getPatient().getDisplay()); }
        if (f.getStatus() != null) dto.setStatus(f.getStatus().toCode());
        if (!f.getRelationship().getCoding().isEmpty()) dto.setRelationship(f.getRelationship().getCodingFirstRep().getDisplay() != null ? f.getRelationship().getCodingFirstRep().getDisplay() : f.getRelationship().getText());
        if (dto.getRelationship() == null) dto.setRelationship(f.getRelationship().getText());
        if (f.getSex() != null && !f.getSex().getCoding().isEmpty()) dto.setSex(f.getSex().getCodingFirstRep().getCode());
        if (f.getDate() != null) try { dto.setDate(sdf.format(f.getDate())); } catch (Exception ignored) {}
        if (!f.getNote().isEmpty()) dto.setNotes(f.getNote().get(0).getText());
        if (!f.getCondition().isEmpty()) {
            FamilyMemberHistory.FamilyMemberHistoryConditionComponent c = f.getCondition().get(0);
            if (!c.getCode().getCoding().isEmpty()) { dto.setConditionCode(c.getCode().getCodingFirstRep().getCode()); dto.setConditionDisplay(c.getCode().getCodingFirstRep().getDisplay()); }
            if (dto.getConditionDisplay() == null) dto.setConditionDisplay(c.getCode().getText());
            if (!c.getNote().isEmpty()) dto.setConditionNote(c.getNote().get(0).getText());
        }
        return dto;
    }

    private FamilyMemberHistory toResource(FamilyHistoryDTO dto) {
        FamilyMemberHistory f = new FamilyMemberHistory();
        f.getPatient().setReference("Patient/" + dto.getPatientId()); if (dto.getPatientName() != null) f.getPatient().setDisplay(dto.getPatientName());
        f.setStatus(dto.getStatus() != null && !dto.getStatus().isEmpty() ? FamilyMemberHistory.FamilyHistoryStatus.fromCode(dto.getStatus()) : FamilyMemberHistory.FamilyHistoryStatus.COMPLETED);
        // Relationship
        if (dto.getRelationship() != null && !dto.getRelationship().isEmpty()) { CodeableConcept rel = new CodeableConcept(); rel.addCoding().setSystem("http://terminology.hl7.org/CodeSystem/v3-RoleCode").setCode(dto.getRelationship().toLowerCase()).setDisplay(dto.getRelationship()); rel.setText(dto.getRelationship()); f.setRelationship(rel); }
        if (dto.getSex() != null && !dto.getSex().isEmpty()) f.getSex().addCoding().setCode(dto.getSex());
        f.setDate(new Date());
        if (dto.getNotes() != null && !dto.getNotes().isEmpty()) f.addNote().setText(dto.getNotes());
        // Condition
        if (dto.getConditionDisplay() != null && !dto.getConditionDisplay().isEmpty()) {
            FamilyMemberHistory.FamilyMemberHistoryConditionComponent c = new FamilyMemberHistory.FamilyMemberHistoryConditionComponent();
            CodeableConcept cc = new CodeableConcept();
            if (dto.getConditionCode() != null && !dto.getConditionCode().isEmpty()) cc.addCoding().setSystem("http://hl7.org/fhir/sid/icd-10").setCode(dto.getConditionCode()).setDisplay(dto.getConditionDisplay());
            cc.setText(dto.getConditionDisplay()); c.setCode(cc);
            if (dto.getConditionNote() != null && !dto.getConditionNote().isEmpty()) c.addNote().setText(dto.getConditionNote());
            f.addCondition(c);
        }
        return f;
    }
}
