package com.emr.fhir.service;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.emr.fhir.dto.AppointmentDTO;
import org.hl7.fhir.r4.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.text.*;
import java.util.*;
@Service
public class AppointmentService {
    @Autowired private IGenericClient fhirClient;
    @Autowired private AuditService auditService;
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm");

    public List<AppointmentDTO> getAllAppointments() {
        Bundle b = fhirClient.search().forResource(Appointment.class).count(50).returnBundle(Bundle.class).execute();
        List<AppointmentDTO> list = new ArrayList<>();
        for (Bundle.BundleEntryComponent e : b.getEntry()) if (e.getResource() instanceof Appointment a) list.add(toDTO(a));
        return list;
    }
    public List<AppointmentDTO> getAppointmentsForPatient(String patientId) {
        Bundle b = fhirClient.search().forResource(Appointment.class).where(Appointment.PATIENT.hasId(patientId)).returnBundle(Bundle.class).execute();
        List<AppointmentDTO> list = new ArrayList<>();
        for (Bundle.BundleEntryComponent e : b.getEntry()) if (e.getResource() instanceof Appointment a) list.add(toDTO(a));
        return list;
    }
    public AppointmentDTO getAppointmentById(String id) { return toDTO(fhirClient.read().resource(Appointment.class).withId(id).execute()); }
    public AppointmentDTO createAppointment(AppointmentDTO dto) {
        MethodOutcome out = fhirClient.create().resource(toResource(dto)).execute();
        dto.setId(out.getId().getIdPart()); auditService.logEvent("CREATE", "Appointment", dto.getId(), "Appointment for patient: " + dto.getPatientId()); return dto;
    }
    public AppointmentDTO updateAppointment(AppointmentDTO dto) {
        Appointment a = toResource(dto); a.setId(dto.getId()); fhirClient.update().resource(a).execute(); auditService.logEvent("UPDATE", "Appointment", dto.getId(), "Updated"); return dto;
    }
    public void deleteAppointment(String id) { fhirClient.delete().resourceById("Appointment", id).execute(); auditService.logEvent("DELETE", "Appointment", id, "Deleted"); }

    public AppointmentDTO toDTO(Appointment a) {
        AppointmentDTO dto = new AppointmentDTO(); dto.setId(a.getIdElement().getIdPart()); dto.setDescription(a.getDescription()); dto.setMinutesDuration(a.getMinutesDuration());
        if (a.getStatus() != null) dto.setStatus(a.getStatus().toCode());
        if (a.getStart() != null) dto.setStart(DATE_FORMAT.format(a.getStart()));
        if (a.getEnd() != null) dto.setEnd(DATE_FORMAT.format(a.getEnd()));
        if (!a.getServiceType().isEmpty() && !a.getServiceType().get(0).getCoding().isEmpty()) dto.setServiceType(a.getServiceType().get(0).getCoding().get(0).getDisplay());
        for (Appointment.AppointmentParticipantComponent p : a.getParticipant()) { Reference ref = p.getActor(); if (ref == null || ref.getReference() == null) continue; if (ref.getReference().startsWith("Patient/")) { dto.setPatientId(ref.getReference().replace("Patient/", "")); dto.setPatientName(ref.getDisplay()); } else if (ref.getReference().startsWith("Practitioner/")) { dto.setPractitionerId(ref.getReference().replace("Practitioner/", "")); dto.setPractitionerName(ref.getDisplay()); } }
        return dto;
    }
    private Appointment toResource(AppointmentDTO dto) {
        Appointment a = new Appointment(); a.setDescription(dto.getDescription()); if (dto.getMinutesDuration() != null) a.setMinutesDuration(dto.getMinutesDuration());
        a.setStatus(dto.getStatus() != null && !dto.getStatus().isEmpty() ? Appointment.AppointmentStatus.fromCode(dto.getStatus()) : Appointment.AppointmentStatus.PROPOSED);
        try { if (dto.getStart() != null && !dto.getStart().isEmpty()) a.setStart(DATE_FORMAT.parse(dto.getStart())); if (dto.getEnd() != null && !dto.getEnd().isEmpty()) a.setEnd(DATE_FORMAT.parse(dto.getEnd())); } catch (ParseException ignored) {}
        if (dto.getServiceType() != null && !dto.getServiceType().isEmpty()) { CodeableConcept st = new CodeableConcept(); st.addCoding().setDisplay(dto.getServiceType()); a.addServiceType(st); }
        if (dto.getPatientId() != null && !dto.getPatientId().isEmpty()) { Appointment.AppointmentParticipantComponent pp = new Appointment.AppointmentParticipantComponent(); pp.getActor().setReference("Patient/" + dto.getPatientId()).setDisplay(dto.getPatientName()); pp.setRequired(Appointment.ParticipantRequired.REQUIRED).setStatus(Appointment.ParticipationStatus.ACCEPTED); a.addParticipant(pp); }
        if (dto.getPractitionerId() != null && !dto.getPractitionerId().isEmpty()) { Appointment.AppointmentParticipantComponent pp = new Appointment.AppointmentParticipantComponent(); pp.getActor().setReference("Practitioner/" + dto.getPractitionerId()).setDisplay(dto.getPractitionerName()); pp.setRequired(Appointment.ParticipantRequired.REQUIRED).setStatus(Appointment.ParticipationStatus.ACCEPTED); a.addParticipant(pp); }
        return a;
    }
}
