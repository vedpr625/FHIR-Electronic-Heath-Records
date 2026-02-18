package com.emr.fhir.service;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.emr.fhir.dto.AllergyIntoleranceDTO;
import org.hl7.fhir.r4.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class AllergyIntoleranceService {
    @Autowired private IGenericClient fhirClient;
    @Autowired private AuditService auditService;
    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

    public List<AllergyIntoleranceDTO> getAllergiesForPatient(String patientId) {
        Bundle b = fhirClient.search().forResource(AllergyIntolerance.class)
            .where(AllergyIntolerance.PATIENT.hasId(patientId))
            .returnBundle(Bundle.class).execute();
        List<AllergyIntoleranceDTO> list = new ArrayList<>();
        for (Bundle.BundleEntryComponent e : b.getEntry())
            if (e.getResource() instanceof AllergyIntolerance a) list.add(toDTO(a));
        return list;
    }

    public AllergyIntoleranceDTO getAllergyById(String id) {
        return toDTO(fhirClient.read().resource(AllergyIntolerance.class).withId(id).execute());
    }

    public AllergyIntoleranceDTO createAllergy(AllergyIntoleranceDTO dto) {
        MethodOutcome out = fhirClient.create().resource(toResource(dto)).execute();
        dto.setId(out.getId().getIdPart());
        auditService.logEvent("CREATE", "AllergyIntolerance", dto.getId(),
            "Allergy recorded: " + dto.getDisplay() + " for patient " + dto.getPatientId());
        return dto;
    }

    public AllergyIntoleranceDTO updateAllergy(AllergyIntoleranceDTO dto) {
        AllergyIntolerance a = toResource(dto); a.setId(dto.getId());
        fhirClient.update().resource(a).execute();
        auditService.logEvent("UPDATE", "AllergyIntolerance", dto.getId(), "Updated: " + dto.getDisplay());
        return dto;
    }

    public void deleteAllergy(String id) {
        fhirClient.delete().resourceById("AllergyIntolerance", id).execute();
        auditService.logEvent("DELETE", "AllergyIntolerance", id, "Deleted allergy: " + id);
    }

    public AllergyIntoleranceDTO toDTO(AllergyIntolerance a) {
        AllergyIntoleranceDTO dto = new AllergyIntoleranceDTO();
        dto.setId(a.getIdElement().getIdPart());
        if (a.getPatient() != null && a.getPatient().getReference() != null) {
            dto.setPatientId(a.getPatient().getReference().replace("Patient/",""));
            dto.setPatientName(a.getPatient().getDisplay());
        }
        if (!a.getCode().getCoding().isEmpty()) {
            dto.setCode(a.getCode().getCodingFirstRep().getCode());
            dto.setDisplay(a.getCode().getCodingFirstRep().getDisplay());
        }
        if (dto.getDisplay() == null) dto.setDisplay(a.getCode().getText());
        if (!a.getCategory().isEmpty()) dto.setCategory(a.getCategory().get(0).getCode());
        if (a.getType() != null) dto.setType(a.getType().toCode());
        if (a.getCriticality() != null) dto.setCriticality(a.getCriticality().toCode());
        if (a.getClinicalStatus() != null && !a.getClinicalStatus().getCoding().isEmpty())
            dto.setClinicalStatus(a.getClinicalStatus().getCodingFirstRep().getCode());
        if (a.getVerificationStatus() != null && !a.getVerificationStatus().getCoding().isEmpty())
            dto.setVerificationStatus(a.getVerificationStatus().getCodingFirstRep().getCode());
        if (!a.getReaction().isEmpty()) {
            AllergyIntolerance.AllergyIntoleranceReactionComponent r = a.getReaction().get(0);
            if (r.getSubstance() != null && !r.getSubstance().getCoding().isEmpty())
                dto.setReactionSubstance(r.getSubstance().getCodingFirstRep().getDisplay());
            if (!r.getManifestation().isEmpty())
                dto.setReactionManifestation(r.getManifestation().get(0).getCodingFirstRep().getDisplay() != null
                    ? r.getManifestation().get(0).getCodingFirstRep().getDisplay()
                    : r.getManifestation().get(0).getText());
            if (r.getSeverity() != null) dto.setReactionSeverity(r.getSeverity().toCode());
            dto.setReactionDescription(r.getDescription());
        }
        if (a.getOnset() instanceof DateTimeType dt && dt.getValue() != null)
            try { dto.setOnsetDate(sdf.format(dt.getValue())); } catch (Exception ignored) {}
        if (a.getLastOccurrence() != null)
            try { dto.setLastOccurrence(sdf.format(a.getLastOccurrence())); } catch (Exception ignored) {}
        if (a.getRecordedDate() != null)
            try { dto.setRecordedDate(sdf.format(a.getRecordedDate())); } catch (Exception ignored) {}
        if (!a.getNote().isEmpty()) dto.setNotes(a.getNote().get(0).getText());
        if (a.getRecorder() != null && a.getRecorder().getReference() != null) {
            dto.setPractitionerId(a.getRecorder().getReference().replace("Practitioner/",""));
            dto.setPractitionerName(a.getRecorder().getDisplay());
        }
        return dto;
    }

    private AllergyIntolerance toResource(AllergyIntoleranceDTO dto) {
        AllergyIntolerance a = new AllergyIntolerance();
        a.getPatient().setReference("Patient/" + dto.getPatientId());
        if (dto.getPatientName() != null) a.getPatient().setDisplay(dto.getPatientName());
        // Code
        CodeableConcept code = new CodeableConcept();
        if (dto.getCode() != null && !dto.getCode().isEmpty())
            code.addCoding().setSystem("http://snomed.info/sct").setCode(dto.getCode()).setDisplay(dto.getDisplay());
        if (dto.getDisplay() != null) code.setText(dto.getDisplay());
        a.setCode(code);
        // Category
        if (dto.getCategory() != null && !dto.getCategory().isEmpty())
            a.addCategory(AllergyIntolerance.AllergyIntoleranceCategory.fromCode(dto.getCategory()));
        // Type
        if (dto.getType() != null && !dto.getType().isEmpty())
            a.setType(AllergyIntolerance.AllergyIntoleranceType.fromCode(dto.getType()));
        // Criticality
        if (dto.getCriticality() != null && !dto.getCriticality().isEmpty())
            a.setCriticality(AllergyIntolerance.AllergyIntoleranceCriticality.fromCode(dto.getCriticality()));
        // Clinical status
        if (dto.getClinicalStatus() != null && !dto.getClinicalStatus().isEmpty())
            a.getClinicalStatus().addCoding().setSystem("http://terminology.hl7.org/CodeSystem/allergyintolerance-clinical").setCode(dto.getClinicalStatus());
        // Verification status
        if (dto.getVerificationStatus() != null && !dto.getVerificationStatus().isEmpty())
            a.getVerificationStatus().addCoding().setSystem("http://terminology.hl7.org/CodeSystem/allergyintolerance-verification").setCode(dto.getVerificationStatus());
        // Reaction
        if ((dto.getReactionManifestation() != null && !dto.getReactionManifestation().isEmpty()) ||
            (dto.getReactionSeverity() != null && !dto.getReactionSeverity().isEmpty())) {
            AllergyIntolerance.AllergyIntoleranceReactionComponent rxn = new AllergyIntolerance.AllergyIntoleranceReactionComponent();
            if (dto.getReactionSubstance() != null && !dto.getReactionSubstance().isEmpty())
                rxn.getSubstance().addCoding().setDisplay(dto.getReactionSubstance());
            if (dto.getReactionManifestation() != null && !dto.getReactionManifestation().isEmpty()) {
                CodeableConcept m = new CodeableConcept(); m.addCoding().setDisplay(dto.getReactionManifestation()); m.setText(dto.getReactionManifestation()); rxn.addManifestation(m);
            }
            if (dto.getReactionSeverity() != null && !dto.getReactionSeverity().isEmpty())
                rxn.setSeverity(AllergyIntolerance.AllergyIntoleranceSeverity.fromCode(dto.getReactionSeverity()));
            if (dto.getReactionDescription() != null) rxn.setDescription(dto.getReactionDescription());
            a.addReaction(rxn);
        }
        if (dto.getNotes() != null && !dto.getNotes().isEmpty()) a.addNote().setText(dto.getNotes());
        a.setRecordedDate(new Date());
        if (dto.getPractitionerId() != null && !dto.getPractitionerId().isEmpty())
            a.getRecorder().setReference("Practitioner/" + dto.getPractitionerId()).setDisplay(dto.getPractitionerName());
        return a;
    }
}
