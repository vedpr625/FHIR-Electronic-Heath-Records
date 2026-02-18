package com.emr.fhir.service;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.emr.fhir.dto.PractitionerDTO;
import org.hl7.fhir.r4.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.*;
@Service
public class PractitionerService {
    @Autowired private IGenericClient fhirClient;
    @Autowired private AuditService auditService;

    public List<PractitionerDTO> getAllPractitioners() {
        Bundle b = fhirClient.search().forResource(Practitioner.class).count(50).returnBundle(Bundle.class).execute();
        List<PractitionerDTO> list = new ArrayList<>();
        for (Bundle.BundleEntryComponent e : b.getEntry()) if (e.getResource() instanceof Practitioner p) list.add(toDTO(p));
        return list;
    }
    public PractitionerDTO getPractitionerById(String id) { return toDTO(fhirClient.read().resource(Practitioner.class).withId(id).execute()); }
    public PractitionerDTO createPractitioner(PractitionerDTO dto) {
        MethodOutcome out = fhirClient.create().resource(toResource(dto)).execute();
        dto.setId(out.getId().getIdPart());
        auditService.logEvent("CREATE", "Practitioner", dto.getId(), "Created: " + dto.getFullName());
        return dto;
    }
    public PractitionerDTO updatePractitioner(PractitionerDTO dto) {
        Practitioner p = toResource(dto); p.setId(dto.getId());
        fhirClient.update().resource(p).execute();
        auditService.logEvent("UPDATE", "Practitioner", dto.getId(), "Updated: " + dto.getFullName());
        return dto;
    }
    public void deletePractitioner(String id) { fhirClient.delete().resourceById("Practitioner", id).execute(); auditService.logEvent("DELETE", "Practitioner", id, "Deleted: " + id); }

    public PractitionerDTO toDTO(Practitioner p) {
        PractitionerDTO dto = new PractitionerDTO(); dto.setId(p.getIdElement().getIdPart()); dto.setActive(p.getActive());
        if (!p.getName().isEmpty()) { HumanName n = p.getName().get(0); dto.setLastName(n.getFamily()); if (!n.getGiven().isEmpty()) dto.setFirstName(n.getGiven().get(0).getValue()); }
        for (ContactPoint cp : p.getTelecom()) { if (cp.getSystem() == ContactPoint.ContactPointSystem.PHONE) dto.setPhone(cp.getValue()); else if (cp.getSystem() == ContactPoint.ContactPointSystem.EMAIL) dto.setEmail(cp.getValue()); }
        for (Practitioner.PractitionerQualificationComponent q : p.getQualification()) { if (q.getCode().getText() != null) dto.setSpecialization(q.getCode().getText()); if (!q.getIdentifier().isEmpty()) dto.setLicenseNumber(q.getIdentifier().get(0).getValue()); }
        return dto;
    }
    private Practitioner toResource(PractitionerDTO dto) {
        Practitioner p = new Practitioner(); p.setActive(dto.isActive());
        HumanName n = new HumanName(); n.setFamily(dto.getLastName()); n.addGiven(dto.getFirstName()); n.setUse(HumanName.NameUse.OFFICIAL); p.addName(n);
        if (dto.getPhone() != null && !dto.getPhone().isEmpty()) p.addTelecom().setSystem(ContactPoint.ContactPointSystem.PHONE).setValue(dto.getPhone());
        if (dto.getEmail() != null && !dto.getEmail().isEmpty()) p.addTelecom().setSystem(ContactPoint.ContactPointSystem.EMAIL).setValue(dto.getEmail());
        if (dto.getSpecialization() != null && !dto.getSpecialization().isEmpty()) { Practitioner.PractitionerQualificationComponent q = new Practitioner.PractitionerQualificationComponent(); q.getCode().setText(dto.getSpecialization()); if (dto.getLicenseNumber() != null) q.addIdentifier().setValue(dto.getLicenseNumber()); p.addQualification(q); }
        return p;
    }
}
