package com.emr.fhir.service;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.StringClientParam;
import com.emr.fhir.dto.PatientDTO;
import org.hl7.fhir.r4.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.text.*;
import java.util.*;
@Service
public class PatientService {
    @Autowired private IGenericClient fhirClient;
    @Autowired private AuditService auditService;
    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

    public List<PatientDTO> getAllPatients() {
        Bundle b = fhirClient.search().forResource(Patient.class).count(50).returnBundle(Bundle.class).execute();
        List<PatientDTO> list = new ArrayList<>();
        for (Bundle.BundleEntryComponent e : b.getEntry()) if (e.getResource() instanceof Patient p) list.add(toDTO(p));
        return list;
    }
    public PatientDTO getPatientById(String id) { return toDTO(fhirClient.read().resource(Patient.class).withId(id).execute()); }
    public List<PatientDTO> searchPatients(String name) {
        Bundle b = fhirClient.search().forResource(Patient.class).where(new StringClientParam("name").matches().value(name)).returnBundle(Bundle.class).execute();
        List<PatientDTO> list = new ArrayList<>();
        for (Bundle.BundleEntryComponent e : b.getEntry()) if (e.getResource() instanceof Patient p) list.add(toDTO(p));
        return list;
    }
    public PatientDTO createPatient(PatientDTO dto) {
        MethodOutcome out = fhirClient.create().resource(toResource(dto)).execute();
        dto.setId(out.getId().getIdPart());
        auditService.logEvent("CREATE", "Patient", dto.getId(), "Created: " + dto.getFullName());
        return dto;
    }
    public PatientDTO updatePatient(PatientDTO dto) {
        Patient p = toResource(dto); p.setId(dto.getId());
        fhirClient.update().resource(p).execute();
        auditService.logEvent("UPDATE", "Patient", dto.getId(), "Updated: " + dto.getFullName());
        return dto;
    }
    public void deletePatient(String id) { fhirClient.delete().resourceById("Patient", id).execute(); auditService.logEvent("DELETE", "Patient", id, "Deleted patient: " + id); }

    public PatientDTO toDTO(Patient p) {
        PatientDTO dto = new PatientDTO();
        dto.setId(p.getIdElement().getIdPart()); dto.setActive(p.getActive());
        if (!p.getName().isEmpty()) { HumanName n = p.getName().get(0); dto.setLastName(n.getFamily()); if (!n.getGiven().isEmpty()) dto.setFirstName(n.getGiven().get(0).getValue()); }
        if (p.getBirthDate() != null) dto.setBirthDate(sdf.format(p.getBirthDate()));
        if (p.getGender() != null) dto.setGender(p.getGender().toCode());
        for (ContactPoint cp : p.getTelecom()) { if (cp.getSystem() == ContactPoint.ContactPointSystem.PHONE) dto.setPhone(cp.getValue()); else if (cp.getSystem() == ContactPoint.ContactPointSystem.EMAIL) dto.setEmail(cp.getValue()); }
        if (!p.getAddress().isEmpty()) { Address a = p.getAddress().get(0); if (!a.getLine().isEmpty()) dto.setAddressLine(a.getLine().get(0).getValue()); dto.setCity(a.getCity()); dto.setState(a.getState()); dto.setPostalCode(a.getPostalCode()); dto.setCountry(a.getCountry()); }
        for (Identifier id : p.getIdentifier()) { if (id.getType() != null && "MRN".equals(id.getType().getText())) dto.setMrn(id.getValue()); }
        // Extension: blood group
        Extension bg = p.getExtensionByUrl("http://hl7.org/fhir/StructureDefinition/patient-bloodGroup");
        if (bg != null && bg.getValue() instanceof StringType st) dto.setBloodGroup(st.getValue());
        return dto;
    }
    private Patient toResource(PatientDTO dto) {
        Patient p = new Patient(); p.setActive(dto.isActive());
        HumanName n = new HumanName(); n.setFamily(dto.getLastName()); n.addGiven(dto.getFirstName()); n.setUse(HumanName.NameUse.OFFICIAL); p.addName(n);
        if (dto.getBirthDate() != null && !dto.getBirthDate().isEmpty()) { try { p.setBirthDate(sdf.parse(dto.getBirthDate())); } catch (ParseException ignored) {} }
        if (dto.getGender() != null && !dto.getGender().isEmpty()) p.setGender(Enumerations.AdministrativeGender.fromCode(dto.getGender()));
        if (dto.getPhone() != null && !dto.getPhone().isEmpty()) p.addTelecom().setSystem(ContactPoint.ContactPointSystem.PHONE).setValue(dto.getPhone());
        if (dto.getEmail() != null && !dto.getEmail().isEmpty()) p.addTelecom().setSystem(ContactPoint.ContactPointSystem.EMAIL).setValue(dto.getEmail());
        Address addr = new Address(); if (dto.getAddressLine() != null) addr.addLine(dto.getAddressLine()); addr.setCity(dto.getCity()); addr.setState(dto.getState()); addr.setPostalCode(dto.getPostalCode()); addr.setCountry(dto.getCountry()); p.addAddress(addr);
        if (dto.getMrn() != null && !dto.getMrn().isEmpty()) { Identifier id = new Identifier(); id.getType().setText("MRN"); id.setValue(dto.getMrn()); p.addIdentifier(id); }
        if (dto.getBloodGroup() != null && !dto.getBloodGroup().isEmpty()) p.addExtension("http://hl7.org/fhir/StructureDefinition/patient-bloodGroup", new StringType(dto.getBloodGroup()));
        return p;
    }
}
