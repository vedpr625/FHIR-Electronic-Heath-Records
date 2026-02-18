package com.emr.fhir.service;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import org.hl7.fhir.r4.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.*;
@Service
public class AuditService {
    @Autowired private IGenericClient fhirClient;
    public void logEvent(String action, String resourceType, String resourceId, String description) {
        try {
            AuditEvent ae = new AuditEvent();
            ae.setRecorded(new Date());
            ae.getType().setSystem("http://dicom.nema.org/resources/ontology/DCM").setCode(getCode(action)).setDisplay(action);
            ae.setAction(AuditEvent.AuditEventAction.fromCode(getAuditAction(action)));
            ae.setOutcome(AuditEvent.AuditEventOutcome._0);
            ae.addAgent().setRequestor(true).setName("MediCare ERP");
            ae.getSource().setSite("MediCare ERP").getObserver().setDisplay("MediCare ERP");
            ae.addEntity().getWhat().setReference(resourceType + "/" + resourceId);
            ae.getEntity().get(0).setDescription(description).getType().setCode(resourceType);
            fhirClient.create().resource(ae).execute();
        } catch (Exception e) { System.err.println("Audit log failed: " + e.getMessage()); }
    }
    public List<Map<String, String>> getAuditEvents() {
        List<Map<String, String>> events = new ArrayList<>();
        try {
            Bundle bundle = fhirClient.search().forResource(AuditEvent.class).count(100).returnBundle(Bundle.class).execute();
            for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
                if (entry.getResource() instanceof AuditEvent ae) {
                    Map<String, String> e = new LinkedHashMap<>();
                    e.put("id", ae.getIdElement().getIdPart());
                    e.put("recorded", ae.getRecorded() != null ? ae.getRecorded().toString() : "");
                    e.put("action", ae.getAction() != null ? ae.getAction().toCode() : "");
                    e.put("outcome", ae.getOutcome() != null ? ae.getOutcome().toCode() : "");
                    if (!ae.getEntity().isEmpty()) { e.put("resource", ae.getEntity().get(0).getWhat().getReference()); e.put("description", ae.getEntity().get(0).getDescription()); }
                    if (!ae.getAgent().isEmpty()) e.put("agent", ae.getAgent().get(0).getName());
                    events.add(e);
                }
            }
        } catch (Exception ex) { System.err.println("Audit retrieval failed: " + ex.getMessage()); }
        return events;
    }
    private String getCode(String a) { return switch(a.toUpperCase()) { case "CREATE" -> "110100"; case "UPDATE" -> "110107"; case "DELETE" -> "110105"; default -> "110100"; }; }
    private String getAuditAction(String a) { return switch(a.toUpperCase()) { case "CREATE" -> "C"; case "UPDATE" -> "U"; case "DELETE" -> "D"; default -> "R"; }; }
}
