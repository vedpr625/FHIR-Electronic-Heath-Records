package com.emr.fhir.service;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.emr.fhir.dto.ProcedureDTO;
import org.hl7.fhir.r4.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.text.SimpleDateFormat; import java.util.*;

@Service
public class ProcedureService {
    @Autowired private IGenericClient fhirClient;
    @Autowired private AuditService auditService;
    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

    public List<ProcedureDTO> getProceduresForPatient(String patientId) {
        Bundle b = fhirClient.search().forResource(Procedure.class)
            .where(Procedure.PATIENT.hasId(patientId)).returnBundle(Bundle.class).execute();
        List<ProcedureDTO> list = new ArrayList<>();
        for (Bundle.BundleEntryComponent e : b.getEntry())
            if (e.getResource() instanceof Procedure p) list.add(toDTO(p));
        return list;
    }

    public ProcedureDTO getProcedureById(String id) {
        return toDTO(fhirClient.read().resource(Procedure.class).withId(id).execute());
    }

    public ProcedureDTO createProcedure(ProcedureDTO dto) {
        MethodOutcome out = fhirClient.create().resource(toResource(dto)).execute();
        dto.setId(out.getId().getIdPart());
        auditService.logEvent("CREATE", "Procedure", dto.getId(),
            "Procedure: " + dto.getDisplay() + " for patient " + dto.getPatientId());
        return dto;
    }

    public ProcedureDTO updateProcedure(ProcedureDTO dto) {
        Procedure p = toResource(dto); p.setId(dto.getId());
        fhirClient.update().resource(p).execute();
        auditService.logEvent("UPDATE", "Procedure", dto.getId(), "Updated: " + dto.getDisplay());
        return dto;
    }

    public void deleteProcedure(String id) {
        fhirClient.delete().resourceById("Procedure", id).execute();
        auditService.logEvent("DELETE", "Procedure", id, "Deleted procedure: " + id);
    }

    public ProcedureDTO toDTO(Procedure p) {
        ProcedureDTO dto = new ProcedureDTO();
        dto.setId(p.getIdElement().getIdPart());
        if (p.getSubject() != null && p.getSubject().getReference() != null) { dto.setPatientId(p.getSubject().getReference().replace("Patient/","")); dto.setPatientName(p.getSubject().getDisplay()); }
        if (p.getStatus() != null) dto.setStatus(p.getStatus().toCode());
        if (!p.getCode().getCoding().isEmpty()) { dto.setCode(p.getCode().getCodingFirstRep().getCode()); dto.setDisplay(p.getCode().getCodingFirstRep().getDisplay()); }
        if (dto.getDisplay() == null) dto.setDisplay(p.getCode().getText());
        if (!p.getCategory().getCoding().isEmpty()) dto.setCategory(p.getCategory().getCodingFirstRep().getDisplay());
        if (p.getPerformed() instanceof DateTimeType dt && dt.getValue() != null) try { dto.setPerformedDate(sdf.format(dt.getValue())); } catch (Exception ignored) {}
        if (!p.getPerformer().isEmpty() && p.getPerformer().get(0).getActor() != null && p.getPerformer().get(0).getActor().getReference() != null) { dto.setPractitionerId(p.getPerformer().get(0).getActor().getReference().replace("Practitioner/","")); dto.setPractitionerName(p.getPerformer().get(0).getActor().getDisplay()); }
        if (p.getEncounter() != null && p.getEncounter().getReference() != null) dto.setEncounterId(p.getEncounter().getReference().replace("Encounter/",""));
        if (!p.getReasonCode().isEmpty()) dto.setReasonCode(p.getReasonCode().get(0).getText());
        if (!p.getBodySite().isEmpty() && !p.getBodySite().get(0).getCoding().isEmpty()) dto.setBodySite(p.getBodySite().get(0).getCodingFirstRep().getDisplay());
        if (dto.getBodySite() == null && !p.getBodySite().isEmpty()) dto.setBodySite(p.getBodySite().get(0).getText());
        if (!p.getOutcome().getCoding().isEmpty()) dto.setOutcome(p.getOutcome().getCodingFirstRep().getDisplay());
        if (dto.getOutcome() == null) dto.setOutcome(p.getOutcome().getText());
        if (!p.getComplication().isEmpty()) dto.setComplication(p.getComplication().get(0).getText());
        if (!p.getFollowUp().isEmpty()) dto.setFollowUp(p.getFollowUp().get(0).getText());
        if (!p.getNote().isEmpty()) dto.setNotes(p.getNote().get(0).getText());
        if (p.getLocation() != null) dto.setLocation(p.getLocation().getDisplay());
        return dto;
    }

    private Procedure toResource(ProcedureDTO dto) {
        Procedure p = new Procedure();
        p.getSubject().setReference("Patient/" + dto.getPatientId()); if (dto.getPatientName() != null) p.getSubject().setDisplay(dto.getPatientName());
        p.setStatus(dto.getStatus() != null && !dto.getStatus().isEmpty() ? Procedure.ProcedureStatus.fromCode(dto.getStatus()) : Procedure.ProcedureStatus.COMPLETED);
        CodeableConcept code = new CodeableConcept();
        if (dto.getCode() != null && !dto.getCode().isEmpty()) code.addCoding().setSystem("http://snomed.info/sct").setCode(dto.getCode()).setDisplay(dto.getDisplay());
        if (dto.getDisplay() != null) code.setText(dto.getDisplay()); p.setCode(code);
        if (dto.getCategory() != null && !dto.getCategory().isEmpty()) { CodeableConcept cat = new CodeableConcept(); cat.addCoding().setDisplay(dto.getCategory()); cat.setText(dto.getCategory()); p.setCategory(cat); }
        if (dto.getPerformedDate() != null && !dto.getPerformedDate().isEmpty()) { try { p.setPerformed(new DateTimeType(sdf.parse(dto.getPerformedDate()))); } catch (Exception ignored) {} }
        if (dto.getPractitionerId() != null && !dto.getPractitionerId().isEmpty()) { Procedure.ProcedurePerformerComponent perf = new Procedure.ProcedurePerformerComponent(); perf.getActor().setReference("Practitioner/" + dto.getPractitionerId()).setDisplay(dto.getPractitionerName()); p.addPerformer(perf); }
        if (dto.getEncounterId() != null && !dto.getEncounterId().isEmpty()) p.getEncounter().setReference("Encounter/" + dto.getEncounterId());
        if (dto.getReasonCode() != null && !dto.getReasonCode().isEmpty()) { CodeableConcept r = new CodeableConcept(); r.setText(dto.getReasonCode()); p.addReasonCode(r); }
        if (dto.getBodySite() != null && !dto.getBodySite().isEmpty()) { CodeableConcept bs = new CodeableConcept(); bs.addCoding().setDisplay(dto.getBodySite()); bs.setText(dto.getBodySite()); p.addBodySite(bs); }
        if (dto.getOutcome() != null && !dto.getOutcome().isEmpty()) { CodeableConcept o = new CodeableConcept(); o.setText(dto.getOutcome()); p.setOutcome(o); }
        if (dto.getComplication() != null && !dto.getComplication().isEmpty()) { CodeableConcept c = new CodeableConcept(); c.setText(dto.getComplication()); p.addComplication(c); }
        if (dto.getFollowUp() != null && !dto.getFollowUp().isEmpty()) { CodeableConcept f = new CodeableConcept(); f.setText(dto.getFollowUp()); p.addFollowUp(f); }
        if (dto.getNotes() != null && !dto.getNotes().isEmpty()) p.addNote().setText(dto.getNotes());
        if (dto.getLocation() != null && !dto.getLocation().isEmpty()) p.getLocation().setDisplay(dto.getLocation());
        return p;
    }
}
