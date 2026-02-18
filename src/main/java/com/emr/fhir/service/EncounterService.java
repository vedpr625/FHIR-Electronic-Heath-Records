package com.emr.fhir.service;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.emr.fhir.dto.EncounterDTO;
import org.hl7.fhir.r4.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.text.*;
import java.util.*;

@Service
public class EncounterService {
    @Autowired private IGenericClient fhirClient;
    @Autowired private AuditService auditService;
    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm");

    public List<EncounterDTO> getEncountersForPatient(String patientId) {
        Bundle b = fhirClient.search().forResource(Encounter.class)
            .where(Encounter.PATIENT.hasId(patientId)).returnBundle(Bundle.class).execute();
        List<EncounterDTO> list = new ArrayList<>();
        for (Bundle.BundleEntryComponent e : b.getEntry())
            if (e.getResource() instanceof Encounter enc) list.add(toDTO(enc));
        return list;
    }

    public List<EncounterDTO> getAllEncounters() {
        Bundle b = fhirClient.search().forResource(Encounter.class)
            .count(100).returnBundle(Bundle.class).execute();
        List<EncounterDTO> list = new ArrayList<>();
        for (Bundle.BundleEntryComponent e : b.getEntry())
            if (e.getResource() instanceof Encounter enc) list.add(toDTO(enc));
        return list;
    }

    public EncounterDTO getEncounterById(String id) {
        return toDTO(fhirClient.read().resource(Encounter.class).withId(id).execute());
    }

    public EncounterDTO createEncounter(EncounterDTO dto) {
        MethodOutcome out = fhirClient.create().resource(toResource(dto)).execute();
        dto.setId(out.getId().getIdPart());
        auditService.logEvent("CREATE", "Encounter", dto.getId(),
            "Encounter created for patient: " + dto.getPatientId() + " type: " + dto.getType());
        return dto;
    }

    public EncounterDTO updateEncounter(EncounterDTO dto) {
        Encounter enc = toResource(dto);
        enc.setId(dto.getId());
        fhirClient.update().resource(enc).execute();
        auditService.logEvent("UPDATE", "Encounter", dto.getId(), "Encounter updated: " + dto.getId());
        return dto;
    }

    public void deleteEncounter(String id) {
        fhirClient.delete().resourceById("Encounter", id).execute();
        auditService.logEvent("DELETE", "Encounter", id, "Encounter deleted: " + id);
    }

    // -----------------------------------------------------------------------
    // toDTO  —  Encounter resource → EncounterDTO
    // -----------------------------------------------------------------------
    public EncounterDTO toDTO(Encounter enc) {
        EncounterDTO dto = new EncounterDTO();
        dto.setId(enc.getIdElement().getIdPart());

        // Patient reference
        if (enc.getSubject() != null) {
            String ref = enc.getSubject().getReference();
            if (ref != null) dto.setPatientId(ref.replace("Patient/", ""));
            dto.setPatientName(enc.getSubject().getDisplay());
        }

        // Status
        if (enc.getStatus() != null) dto.setStatus(enc.getStatus().toCode());

        // FIX 1: class_ is a Coding in FHIR R4 — getClass_() returns Coding directly.
        // getClassElement() does NOT exist; it would resolve to Java Object.getClass()
        // which returns java.lang.Class, not a FHIR element.
        Coding classCoding = enc.getClass_();
        if (classCoding != null && classCoding.getCode() != null)
            dto.setClassCode(classCoding.getCode());

        // Type
        if (!enc.getType().isEmpty()) {
            CodeableConcept firstType = enc.getType().get(0);
            if (!firstType.getCoding().isEmpty())
                dto.setType(firstType.getCodingFirstRep().getDisplay());
            if (dto.getType() == null)
                dto.setType(firstType.getText());
        }

        // Reason code
        if (!enc.getReasonCode().isEmpty()) {
            CodeableConcept firstReason = enc.getReasonCode().get(0);
            if (!firstReason.getCoding().isEmpty())
                dto.setReasonCode(firstReason.getCodingFirstRep().getDisplay());
            if (dto.getReasonCode() == null)
                dto.setReasonCode(firstReason.getText());
        }

        // Participants → practitioner
        for (Encounter.EncounterParticipantComponent p : enc.getParticipant()) {
            if (p.getIndividual() != null
                    && p.getIndividual().getReference() != null
                    && p.getIndividual().getReference().startsWith("Practitioner/")) {
                dto.setPractitionerId(p.getIndividual().getReference().replace("Practitioner/", ""));
                dto.setPractitionerName(p.getIndividual().getDisplay());
                break;
            }
        }

        // Period
        if (enc.hasPeriod()) {
            try { if (enc.getPeriod().hasStart()) dto.setStartDate(sdf.format(enc.getPeriod().getStart())); } catch (Exception ignored) {}
            try { if (enc.getPeriod().hasEnd())  dto.setEndDate(sdf.format(enc.getPeriod().getEnd())); } catch (Exception ignored) {}
        }

        // Appointment
        if (!enc.getAppointment().isEmpty()) {
            String ref = enc.getAppointment().get(0).getReference();
            if (ref != null) dto.setAppointmentId(ref.replace("Appointment/", ""));
        }

        // FIX 2: Encounter in FHIR R4 does NOT have addNote() / getNote().
        // Notes are stored as a custom Extension with URL "http://emr.local/encounter-notes".
        Extension notesExt = enc.getExtensionByUrl("http://emr.local/encounter-notes");
        if (notesExt != null && notesExt.getValue() instanceof StringType st)
            dto.setNotes(st.getValue());

        // Service provider (organization)
        if (enc.hasServiceProvider()) {
            String ref = enc.getServiceProvider().getReference();
            if (ref != null) dto.setOrganizationId(ref.replace("Organization/", ""));
            dto.setOrganizationName(enc.getServiceProvider().getDisplay());
        }

        // Linked diagnosis
        if (!enc.getDiagnosis().isEmpty()) {
            Encounter.DiagnosisComponent diag = enc.getDiagnosis().get(0);
            String ref = diag.getCondition().getReference();
            if (ref != null) dto.setConditionId(ref.replace("Condition/", ""));
            dto.setConditionDisplay(diag.getCondition().getDisplay());
        }

        return dto;
    }

    // -----------------------------------------------------------------------
    // toResource  —  EncounterDTO → Encounter resource
    // -----------------------------------------------------------------------
    private Encounter toResource(EncounterDTO dto) {
        Encounter enc = new Encounter();

        // Patient
        enc.getSubject().setReference("Patient/" + dto.getPatientId());
        if (dto.getPatientName() != null) enc.getSubject().setDisplay(dto.getPatientName());

        // Status
        enc.setStatus(dto.getStatus() != null && !dto.getStatus().isEmpty()
            ? Encounter.EncounterStatus.fromCode(dto.getStatus())
            : Encounter.EncounterStatus.INPROGRESS);

        // FIX 1: Set class_ as a Coding directly — no setClassElement(), just setClass_()
        String classCode = (dto.getClassCode() != null && !dto.getClassCode().isEmpty())
            ? dto.getClassCode() : "AMB";
        enc.setClass_(new Coding()
            .setSystem("http://terminology.hl7.org/CodeSystem/v3-ActCode")
            .setCode(classCode));

        // Type
        if (dto.getType() != null && !dto.getType().isEmpty()) {
            CodeableConcept t = new CodeableConcept();
            t.addCoding().setSystem("http://snomed.info/sct").setDisplay(dto.getType());
            t.setText(dto.getType());
            enc.addType(t);
        }

        // Reason code
        if (dto.getReasonCode() != null && !dto.getReasonCode().isEmpty()) {
            CodeableConcept r = new CodeableConcept();
            r.addCoding().setDisplay(dto.getReasonCode());
            r.setText(dto.getReasonCode());
            enc.addReasonCode(r);
        }

        // Participant (practitioner)
        if (dto.getPractitionerId() != null && !dto.getPractitionerId().isEmpty()) {
            Encounter.EncounterParticipantComponent p = new Encounter.EncounterParticipantComponent();
            p.getIndividual().setReference("Practitioner/" + dto.getPractitionerId());
            if (dto.getPractitionerName() != null)
                p.getIndividual().setDisplay(dto.getPractitionerName());
            enc.addParticipant(p);
        }

        // Period
        Period period = new Period();
        try {
            if (dto.getStartDate() != null && !dto.getStartDate().isEmpty())
                period.setStart(sdf.parse(dto.getStartDate()));
            if (dto.getEndDate() != null && !dto.getEndDate().isEmpty())
                period.setEnd(sdf.parse(dto.getEndDate()));
        } catch (ParseException ignored) {}
        enc.setPeriod(period);

        // Appointment
        if (dto.getAppointmentId() != null && !dto.getAppointmentId().isEmpty())
            enc.addAppointment().setReference("Appointment/" + dto.getAppointmentId());

        // FIX 2: Encounter has no addNote() in FHIR R4.
        // Store notes as a custom Extension (StringType value).
        if (dto.getNotes() != null && !dto.getNotes().isEmpty())
            enc.addExtension("http://emr.local/encounter-notes",
                new StringType(dto.getNotes()));

        // Service provider
        if (dto.getOrganizationId() != null && !dto.getOrganizationId().isEmpty()) {
            enc.getServiceProvider().setReference("Organization/" + dto.getOrganizationId());
            if (dto.getOrganizationName() != null)
                enc.getServiceProvider().setDisplay(dto.getOrganizationName());
        }

        // Diagnosis (linked condition)
        if (dto.getConditionId() != null && !dto.getConditionId().isEmpty()) {
            Encounter.DiagnosisComponent diag = new Encounter.DiagnosisComponent();
            diag.getCondition().setReference("Condition/" + dto.getConditionId());
            if (dto.getConditionDisplay() != null)
                diag.getCondition().setDisplay(dto.getConditionDisplay());
            enc.addDiagnosis(diag);
        }

        return enc;
    }
}
