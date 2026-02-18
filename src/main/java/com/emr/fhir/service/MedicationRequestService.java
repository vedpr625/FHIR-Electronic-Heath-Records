package com.emr.fhir.service;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.emr.fhir.dto.MedicationRequestDTO;
import org.hl7.fhir.r4.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.text.*;
import java.util.*;
@Service
public class MedicationRequestService {
    @Autowired private IGenericClient fhirClient;
    @Autowired private AuditService auditService;
    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

    public List<MedicationRequestDTO> getPrescriptionsForPatient(String patientId) {
        Bundle b = fhirClient.search().forResource(MedicationRequest.class)
            .where(MedicationRequest.PATIENT.hasId(patientId)).returnBundle(Bundle.class).execute();
        List<MedicationRequestDTO> list = new ArrayList<>();
        for (Bundle.BundleEntryComponent e : b.getEntry()) if (e.getResource() instanceof MedicationRequest mr) list.add(toDTO(mr));
        return list;
    }
    public List<MedicationRequestDTO> getAllPrescriptions() {
        Bundle b = fhirClient.search().forResource(MedicationRequest.class).count(100).returnBundle(Bundle.class).execute();
        List<MedicationRequestDTO> list = new ArrayList<>();
        for (Bundle.BundleEntryComponent e : b.getEntry()) if (e.getResource() instanceof MedicationRequest mr) list.add(toDTO(mr));
        return list;
    }
    public MedicationRequestDTO getPrescriptionById(String id) { return toDTO(fhirClient.read().resource(MedicationRequest.class).withId(id).execute()); }
    public MedicationRequestDTO createPrescription(MedicationRequestDTO dto) {
        MethodOutcome out = fhirClient.create().resource(toResource(dto)).execute();
        dto.setId(out.getId().getIdPart());
        auditService.logEvent("CREATE", "MedicationRequest", dto.getId(), "Prescription created: " + dto.getMedicationDisplay() + " for patient " + dto.getPatientId());
        return dto;
    }
    public MedicationRequestDTO updatePrescription(MedicationRequestDTO dto) {
        MedicationRequest mr = toResource(dto); mr.setId(dto.getId());
        fhirClient.update().resource(mr).execute();
        auditService.logEvent("UPDATE", "MedicationRequest", dto.getId(), "Prescription updated: " + dto.getMedicationDisplay());
        return dto;
    }
    public void deletePrescription(String id) { fhirClient.delete().resourceById("MedicationRequest", id).execute(); auditService.logEvent("DELETE", "MedicationRequest", id, "Prescription deleted: " + id); }

    public MedicationRequestDTO toDTO(MedicationRequest mr) {
        MedicationRequestDTO dto = new MedicationRequestDTO();
        dto.setId(mr.getIdElement().getIdPart());
        if (mr.getSubject() != null) { String ref = mr.getSubject().getReference(); if (ref != null) dto.setPatientId(ref.replace("Patient/", "")); dto.setPatientName(mr.getSubject().getDisplay()); }
        if (mr.getRequester() != null) { String ref = mr.getRequester().getReference(); if (ref != null) dto.setPractitionerId(ref.replace("Practitioner/", "")); dto.setPractitionerName(mr.getRequester().getDisplay()); }
        if (mr.getMedication() instanceof CodeableConcept cc) { if (!cc.getCoding().isEmpty()) { dto.setMedicationCode(cc.getCodingFirstRep().getCode()); dto.setMedicationDisplay(cc.getCodingFirstRep().getDisplay()); } if (dto.getMedicationDisplay() == null) dto.setMedicationDisplay(cc.getText()); }
        if (mr.getStatus() != null) dto.setStatus(mr.getStatus().toCode());
        if (mr.getIntent() != null) dto.setIntent(mr.getIntent().toCode());
        if (mr.getAuthoredOn() != null) dto.setAuthoredOn(sdf.format(mr.getAuthoredOn()));
        if (!mr.getDosageInstruction().isEmpty()) {
            Dosage d = mr.getDosageInstruction().get(0);
            dto.setDosageText(d.getText()); dto.setRoute(d.getRoute() != null ? d.getRoute().getText() : null);
            if (d.getTiming() != null && d.getTiming().getCode() != null) dto.setFrequency(d.getTiming().getCode().getText());
            if (d.getDoseAndRate() != null && !d.getDoseAndRate().isEmpty()) { Dosage.DosageDoseAndRateComponent dr = d.getDoseAndRate().get(0); if (dr.getDose() instanceof Quantity q) { dto.setDoseQuantity(q.getValue() != null ? q.getValue().toPlainString() : null); dto.setDoseUnit(q.getUnit()); } }
        }
        if (mr.getDispenseRequest() != null) { if (mr.getDispenseRequest().getQuantity() != null) dto.setQuantity(mr.getDispenseRequest().getQuantity().getValue() != null ? mr.getDispenseRequest().getQuantity().getValue().intValue() : null); dto.setRefills(mr.getDispenseRequest().getNumberOfRepeatsAllowed()); if (mr.getDispenseRequest().getValidityPeriod() != null) { Period vp = mr.getDispenseRequest().getValidityPeriod(); if (vp.getStart() != null) dto.setStartDate(sdf.format(vp.getStart())); if (vp.getEnd() != null) dto.setEndDate(sdf.format(vp.getEnd())); } }
        if (!mr.getNote().isEmpty()) dto.setInstructions(mr.getNote().get(0).getText());
        if (!mr.getReasonReference().isEmpty()) { String ref = mr.getReasonReference().get(0).getReference(); if (ref != null) dto.setConditionId(ref.replace("Condition/", "")); dto.setConditionDisplay(mr.getReasonReference().get(0).getDisplay()); }
        return dto;
    }
    private MedicationRequest toResource(MedicationRequestDTO dto) {
        MedicationRequest mr = new MedicationRequest();
        mr.getSubject().setReference("Patient/" + dto.getPatientId());
        if (dto.getPatientName() != null) mr.getSubject().setDisplay(dto.getPatientName());
        if (dto.getPractitionerId() != null && !dto.getPractitionerId().isEmpty()) { mr.getRequester().setReference("Practitioner/" + dto.getPractitionerId()); if (dto.getPractitionerName() != null) mr.getRequester().setDisplay(dto.getPractitionerName()); }
        CodeableConcept med = new CodeableConcept();
        if (dto.getMedicationCode() != null && !dto.getMedicationCode().isEmpty()) med.addCoding().setSystem("http://www.nlm.nih.gov/research/umls/rxnorm").setCode(dto.getMedicationCode()).setDisplay(dto.getMedicationDisplay());
        if (dto.getMedicationDisplay() != null) med.setText(dto.getMedicationDisplay());
        mr.setMedication(med);
        mr.setStatus(dto.getStatus() != null && !dto.getStatus().isEmpty() ? MedicationRequest.MedicationRequestStatus.fromCode(dto.getStatus()) : MedicationRequest.MedicationRequestStatus.ACTIVE);
        mr.setIntent(dto.getIntent() != null && !dto.getIntent().isEmpty() ? MedicationRequest.MedicationRequestIntent.fromCode(dto.getIntent()) : MedicationRequest.MedicationRequestIntent.ORDER);
        mr.setAuthoredOn(new Date());
        Dosage dosage = new Dosage();
        if (dto.getDosageText() != null) dosage.setText(dto.getDosageText());
        if (dto.getRoute() != null && !dto.getRoute().isEmpty()) dosage.getRoute().setText(dto.getRoute());
        if (dto.getFrequency() != null && !dto.getFrequency().isEmpty()) dosage.getTiming().getCode().setText(dto.getFrequency());
        if (dto.getDoseQuantity() != null && !dto.getDoseQuantity().isEmpty()) { Quantity q = new Quantity(); try { q.setValue(new java.math.BigDecimal(dto.getDoseQuantity())); } catch (NumberFormatException ignored) {} if (dto.getDoseUnit() != null) q.setUnit(dto.getDoseUnit()); dosage.addDoseAndRate().setDose(q); }
        mr.addDosageInstruction(dosage);
        MedicationRequest.MedicationRequestDispenseRequestComponent dr = new MedicationRequest.MedicationRequestDispenseRequestComponent();
        if (dto.getQuantity() != null) dr.getQuantity().setValue(dto.getQuantity());
        if (dto.getRefills() != null) dr.setNumberOfRepeatsAllowed(dto.getRefills());
        if (dto.getStartDate() != null || dto.getEndDate() != null) { Period vp = new Period(); try { if (dto.getStartDate() != null && !dto.getStartDate().isEmpty()) vp.setStart(sdf.parse(dto.getStartDate())); if (dto.getEndDate() != null && !dto.getEndDate().isEmpty()) vp.setEnd(sdf.parse(dto.getEndDate())); } catch (ParseException ignored) {} dr.setValidityPeriod(vp); }
        mr.setDispenseRequest(dr);
        if (dto.getInstructions() != null && !dto.getInstructions().isEmpty()) mr.addNote().setText(dto.getInstructions());
        if (dto.getConditionId() != null && !dto.getConditionId().isEmpty()) mr.addReasonReference().setReference("Condition/" + dto.getConditionId()).setDisplay(dto.getConditionDisplay());
        return mr;
    }
}
