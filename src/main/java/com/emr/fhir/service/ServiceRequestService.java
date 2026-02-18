package com.emr.fhir.service;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.emr.fhir.dto.ServiceRequestDTO;
import org.hl7.fhir.r4.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.text.SimpleDateFormat; import java.util.*;

@Service
public class ServiceRequestService {
    @Autowired private IGenericClient fhirClient;
    @Autowired private AuditService auditService;
    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

    public List<ServiceRequestDTO> getRequestsForPatient(String patientId) {
        Bundle b = fhirClient.search().forResource(ServiceRequest.class)
            .where(ServiceRequest.PATIENT.hasId(patientId)).returnBundle(Bundle.class).execute();
        List<ServiceRequestDTO> list = new ArrayList<>();
        for (Bundle.BundleEntryComponent e : b.getEntry())
            if (e.getResource() instanceof ServiceRequest sr) list.add(toDTO(sr));
        return list;
    }

    public ServiceRequestDTO getRequestById(String id) {
        return toDTO(fhirClient.read().resource(ServiceRequest.class).withId(id).execute());
    }

    public ServiceRequestDTO createRequest(ServiceRequestDTO dto) {
        MethodOutcome out = fhirClient.create().resource(toResource(dto)).execute();
        dto.setId(out.getId().getIdPart());
        auditService.logEvent("CREATE", "ServiceRequest", dto.getId(),
            "Investigation ordered: " + dto.getDisplay() + " for patient " + dto.getPatientId());
        return dto;
    }

    public ServiceRequestDTO updateRequest(ServiceRequestDTO dto) {
        ServiceRequest sr = toResource(dto); sr.setId(dto.getId());
        fhirClient.update().resource(sr).execute();
        auditService.logEvent("UPDATE", "ServiceRequest", dto.getId(), "Updated: " + dto.getDisplay());
        return dto;
    }

    public void deleteRequest(String id) {
        fhirClient.delete().resourceById("ServiceRequest", id).execute();
        auditService.logEvent("DELETE", "ServiceRequest", id, "Deleted investigation: " + id);
    }

    public ServiceRequestDTO toDTO(ServiceRequest sr) {
        ServiceRequestDTO dto = new ServiceRequestDTO();
        dto.setId(sr.getIdElement().getIdPart());
        if (sr.getSubject() != null && sr.getSubject().getReference() != null) { dto.setPatientId(sr.getSubject().getReference().replace("Patient/","")); dto.setPatientName(sr.getSubject().getDisplay()); }
        if (sr.getStatus() != null) dto.setStatus(sr.getStatus().toCode());
        if (sr.getIntent() != null) dto.setIntent(sr.getIntent().toCode());
        if (sr.getPriority() != null) dto.setPriority(sr.getPriority().toCode());
        if (!sr.getCode().getCoding().isEmpty()) { dto.setCode(sr.getCode().getCodingFirstRep().getCode()); dto.setDisplay(sr.getCode().getCodingFirstRep().getDisplay()); }
        if (dto.getDisplay() == null) dto.setDisplay(sr.getCode().getText());
        if (!sr.getCategory().isEmpty() && !sr.getCategory().get(0).getCoding().isEmpty()) dto.setCategory(sr.getCategory().get(0).getCodingFirstRep().getDisplay());
        if (dto.getCategory() == null && !sr.getCategory().isEmpty()) dto.setCategory(sr.getCategory().get(0).getText());
        if (!sr.getOrderDetail().isEmpty()) dto.setOrderDetail(sr.getOrderDetail().get(0).getText());
        if (sr.getRequester() != null && sr.getRequester().getReference() != null) { dto.setPractitionerId(sr.getRequester().getReference().replace("Practitioner/","")); dto.setPractitionerName(sr.getRequester().getDisplay()); }
        if (sr.getEncounter() != null && sr.getEncounter().getReference() != null) dto.setEncounterId(sr.getEncounter().getReference().replace("Encounter/",""));
        if (sr.getAuthoredOn() != null) try { dto.setAuthoredDate(sdf.format(sr.getAuthoredOn())); } catch (Exception ignored) {}
        if (sr.getOccurrence() instanceof DateTimeType dt && dt.getValue() != null) try { dto.setOccurrenceDate(sdf.format(dt.getValue())); } catch (Exception ignored) {}
        if (!sr.getReasonCode().isEmpty()) dto.setReasonCode(sr.getReasonCode().get(0).getText() != null ? sr.getReasonCode().get(0).getText() : (sr.getReasonCode().get(0).getCodingFirstRep().getDisplay()));
        if (!sr.getSpecimen().isEmpty()) dto.setSpecimen(sr.getSpecimen().get(0).getDisplay());
        if (!sr.getBodySite().isEmpty()) dto.setBodySite(sr.getBodySite().get(0).getText());
        if (!sr.getNote().isEmpty()) dto.setNotes(sr.getNote().get(0).getText());
        // result status via extension
        Extension ext = sr.getExtensionByUrl("http://emr.local/result-status");
        if (ext != null && ext.getValue() instanceof StringType st) dto.setResultStatus(st.getValue());
        Extension ext2 = sr.getExtensionByUrl("http://emr.local/result-notes");
        if (ext2 != null && ext2.getValue() instanceof StringType st2) dto.setResultNotes(st2.getValue());
        return dto;
    }

    private ServiceRequest toResource(ServiceRequestDTO dto) {
        ServiceRequest sr = new ServiceRequest();
        sr.getSubject().setReference("Patient/" + dto.getPatientId()); if (dto.getPatientName() != null) sr.getSubject().setDisplay(dto.getPatientName());
        sr.setStatus(dto.getStatus() != null && !dto.getStatus().isEmpty() ? ServiceRequest.ServiceRequestStatus.fromCode(dto.getStatus()) : ServiceRequest.ServiceRequestStatus.ACTIVE);
        sr.setIntent(dto.getIntent() != null && !dto.getIntent().isEmpty() ? ServiceRequest.ServiceRequestIntent.fromCode(dto.getIntent()) : ServiceRequest.ServiceRequestIntent.ORDER);
        if (dto.getPriority() != null && !dto.getPriority().isEmpty()) sr.setPriority(ServiceRequest.ServiceRequestPriority.fromCode(dto.getPriority()));
        CodeableConcept code = new CodeableConcept();
        if (dto.getCode() != null && !dto.getCode().isEmpty()) code.addCoding().setSystem("http://loinc.org").setCode(dto.getCode()).setDisplay(dto.getDisplay());
        if (dto.getDisplay() != null) code.setText(dto.getDisplay()); sr.setCode(code);
        if (dto.getCategory() != null && !dto.getCategory().isEmpty()) { CodeableConcept cat = new CodeableConcept(); cat.addCoding().setDisplay(dto.getCategory()); cat.setText(dto.getCategory()); sr.addCategory(cat); }
        if (dto.getOrderDetail() != null && !dto.getOrderDetail().isEmpty()) { CodeableConcept od = new CodeableConcept(); od.setText(dto.getOrderDetail()); sr.addOrderDetail(od); }
        if (dto.getPractitionerId() != null && !dto.getPractitionerId().isEmpty()) sr.getRequester().setReference("Practitioner/" + dto.getPractitionerId()).setDisplay(dto.getPractitionerName());
        if (dto.getEncounterId() != null && !dto.getEncounterId().isEmpty()) sr.getEncounter().setReference("Encounter/" + dto.getEncounterId());
        sr.setAuthoredOn(new Date());
        if (dto.getOccurrenceDate() != null && !dto.getOccurrenceDate().isEmpty()) { try { sr.setOccurrence(new DateTimeType(sdf.parse(dto.getOccurrenceDate()))); } catch (Exception ignored) {} }
        if (dto.getReasonCode() != null && !dto.getReasonCode().isEmpty()) { CodeableConcept r = new CodeableConcept(); r.setText(dto.getReasonCode()); sr.addReasonCode(r); }
        if (dto.getSpecimen() != null && !dto.getSpecimen().isEmpty()) sr.addSpecimen().setDisplay(dto.getSpecimen());
        if (dto.getBodySite() != null && !dto.getBodySite().isEmpty()) { CodeableConcept bs = new CodeableConcept(); bs.setText(dto.getBodySite()); sr.addBodySite(bs); }
        if (dto.getNotes() != null && !dto.getNotes().isEmpty()) sr.addNote().setText(dto.getNotes());
        if (dto.getConditionId() != null && !dto.getConditionId().isEmpty()) sr.addReasonReference().setReference("Condition/" + dto.getConditionId()).setDisplay(dto.getConditionDisplay());
        if (dto.getResultStatus() != null && !dto.getResultStatus().isEmpty()) sr.addExtension("http://emr.local/result-status", new StringType(dto.getResultStatus()));
        if (dto.getResultNotes() != null && !dto.getResultNotes().isEmpty()) sr.addExtension("http://emr.local/result-notes", new StringType(dto.getResultNotes()));
        return sr;
    }
}
