package com.emr.fhir.service;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.emr.fhir.dto.OrganizationDTO;
import org.hl7.fhir.r4.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.*;
@Service
public class OrganizationService {
    @Autowired private IGenericClient fhirClient;
    @Autowired private AuditService auditService;
    public List<OrganizationDTO> getAllOrganizations() {
        Bundle b = fhirClient.search().forResource(Organization.class).count(50).returnBundle(Bundle.class).execute();
        List<OrganizationDTO> list = new ArrayList<>();
        for (Bundle.BundleEntryComponent e : b.getEntry()) if (e.getResource() instanceof Organization o) list.add(toDTO(o));
        return list;
    }
    public OrganizationDTO getOrganizationById(String id) { return toDTO(fhirClient.read().resource(Organization.class).withId(id).execute()); }
    public OrganizationDTO createOrganization(OrganizationDTO dto) { MethodOutcome out = fhirClient.create().resource(toResource(dto)).execute(); dto.setId(out.getId().getIdPart()); auditService.logEvent("CREATE", "Organization", dto.getId(), "Created: " + dto.getName()); return dto; }
    public OrganizationDTO updateOrganization(OrganizationDTO dto) { Organization o = toResource(dto); o.setId(dto.getId()); fhirClient.update().resource(o).execute(); auditService.logEvent("UPDATE", "Organization", dto.getId(), "Updated: " + dto.getName()); return dto; }
    public void deleteOrganization(String id) { fhirClient.delete().resourceById("Organization", id).execute(); auditService.logEvent("DELETE", "Organization", id, "Deleted: " + id); }
    public OrganizationDTO toDTO(Organization o) {
        OrganizationDTO dto = new OrganizationDTO(); dto.setId(o.getIdElement().getIdPart()); dto.setName(o.getName()); dto.setActive(o.getActive());
        if (!o.getType().isEmpty() && !o.getType().get(0).getCoding().isEmpty()) dto.setType(o.getType().get(0).getCoding().get(0).getDisplay());
        for (ContactPoint cp : o.getTelecom()) { if (cp.getSystem() == ContactPoint.ContactPointSystem.PHONE) dto.setPhone(cp.getValue()); else if (cp.getSystem() == ContactPoint.ContactPointSystem.EMAIL) dto.setEmail(cp.getValue()); }
        if (!o.getAddress().isEmpty()) { Address a = o.getAddress().get(0); if (!a.getLine().isEmpty()) dto.setAddressLine(a.getLine().get(0).getValue()); dto.setCity(a.getCity()); dto.setState(a.getState()); dto.setPostalCode(a.getPostalCode()); dto.setCountry(a.getCountry()); }
        return dto;
    }
    private Organization toResource(OrganizationDTO dto) {
        Organization o = new Organization(); o.setActive(dto.isActive()); o.setName(dto.getName());
        if (dto.getType() != null && !dto.getType().isEmpty()) { CodeableConcept t = new CodeableConcept(); t.addCoding().setDisplay(dto.getType()); o.addType(t); }
        if (dto.getPhone() != null && !dto.getPhone().isEmpty()) o.addTelecom().setSystem(ContactPoint.ContactPointSystem.PHONE).setValue(dto.getPhone());
        if (dto.getEmail() != null && !dto.getEmail().isEmpty()) o.addTelecom().setSystem(ContactPoint.ContactPointSystem.EMAIL).setValue(dto.getEmail());
        Address addr = new Address(); if (dto.getAddressLine() != null) addr.addLine(dto.getAddressLine()); addr.setCity(dto.getCity()); addr.setState(dto.getState()); addr.setPostalCode(dto.getPostalCode()); addr.setCountry(dto.getCountry()); o.addAddress(addr);
        return o;
    }
}
