package com.emr.fhir.service;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.emr.fhir.dto.DocumentReferenceDTO;
import org.hl7.fhir.r4.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.text.SimpleDateFormat; import java.util.*;

@Service
public class DocumentReferenceService {
    @Autowired private IGenericClient fhirClient;
    @Autowired private AuditService auditService;
    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

    public List<DocumentReferenceDTO> getDocumentsForPatient(String patientId) {
        Bundle b = fhirClient.search().forResource(DocumentReference.class)
            .where(DocumentReference.PATIENT.hasId(patientId)).returnBundle(Bundle.class).execute();
        List<DocumentReferenceDTO> list = new ArrayList<>();
        for (Bundle.BundleEntryComponent e : b.getEntry())
            if (e.getResource() instanceof DocumentReference d) list.add(toDTO(d));
        return list;
    }

    public DocumentReferenceDTO getDocumentById(String id) {
        return toDTO(fhirClient.read().resource(DocumentReference.class).withId(id).execute());
    }

    public DocumentReferenceDTO createDocument(DocumentReferenceDTO dto) {
        MethodOutcome out = fhirClient.create().resource(toResource(dto)).execute();
        dto.setId(out.getId().getIdPart());
        auditService.logEvent("CREATE", "DocumentReference", dto.getId(),
            "Document: " + dto.getTitle() + " (" + dto.getType() + ") for patient " + dto.getPatientId());
        return dto;
    }

    public DocumentReferenceDTO updateDocument(DocumentReferenceDTO dto) {
        DocumentReference d = toResource(dto); d.setId(dto.getId());
        fhirClient.update().resource(d).execute();
        auditService.logEvent("UPDATE", "DocumentReference", dto.getId(), "Updated: " + dto.getTitle());
        return dto;
    }

    public void deleteDocument(String id) {
        fhirClient.delete().resourceById("DocumentReference", id).execute();
        auditService.logEvent("DELETE", "DocumentReference", id, "Deleted document: " + id);
    }

    public DocumentReferenceDTO toDTO(DocumentReference d) {
        DocumentReferenceDTO dto = new DocumentReferenceDTO();
        dto.setId(d.getIdElement().getIdPart());
        if (d.getSubject() != null && d.getSubject().getReference() != null) { dto.setPatientId(d.getSubject().getReference().replace("Patient/","")); dto.setPatientName(d.getSubject().getDisplay()); }
        if (d.getStatus() != null) dto.setStatus(d.getStatus().toCode());
        if (d.getDocStatus() != null) dto.setDocStatus(d.getDocStatus().toCode());
        if (!d.getType().getCoding().isEmpty()) dto.setType(d.getType().getCodingFirstRep().getDisplay());
        if (dto.getType() == null) dto.setType(d.getType().getText());
        if (!d.getCategory().isEmpty() && !d.getCategory().get(0).getCoding().isEmpty()) dto.setCategory(d.getCategory().get(0).getCodingFirstRep().getDisplay());
        if (dto.getCategory() == null && !d.getCategory().isEmpty()) dto.setCategory(d.getCategory().get(0).getText());
        dto.setDescription(d.getDescription());
        if (d.getDate() != null) try { dto.setDate(sdf.format(d.getDate())); } catch (Exception ignored) {}
        if (!d.getAuthor().isEmpty() && d.getAuthor().get(0).getReference() != null) { dto.setPractitionerId(d.getAuthor().get(0).getReference().replace("Practitioner/","")); dto.setPractitionerName(d.getAuthor().get(0).getDisplay()); }
        if (d.getContext() != null && !d.getContext().getEncounter().isEmpty() && d.getContext().getEncounter().get(0).getReference() != null) dto.setEncounterId(d.getContext().getEncounter().get(0).getReference().replace("Encounter/",""));
        if (!d.getContent().isEmpty()) {
            DocumentReference.DocumentReferenceContentComponent content = d.getContent().get(0);
            if (content.getAttachment() != null) {
                dto.setContentType(content.getAttachment().getContentType());
                dto.setTitle(content.getAttachment().getTitle());
                dto.setUrl(content.getAttachment().getUrl());
                if (content.getAttachment().getData() != null) dto.setContent(new String(content.getAttachment().getData()));
                dto.setLanguage(content.getAttachment().getLanguage());
            }
        }
        if (!d.getSecurityLabel().isEmpty() && !d.getSecurityLabel().get(0).getCoding().isEmpty()) dto.setSecurityLabel(d.getSecurityLabel().get(0).getCodingFirstRep().getDisplay());
        Extension notesExt = d.getExtensionByUrl("http://emr.local/doc-notes");
        if (notesExt != null && notesExt.getValue() instanceof StringType st) dto.setNotes(st.getValue());
        return dto;
    }

    private DocumentReference toResource(DocumentReferenceDTO dto) {
        DocumentReference d = new DocumentReference();
        d.getSubject().setReference("Patient/" + dto.getPatientId()); if (dto.getPatientName() != null) d.getSubject().setDisplay(dto.getPatientName());
        d.setStatus(dto.getStatus() != null && !dto.getStatus().isEmpty() ? Enumerations.DocumentReferenceStatus.fromCode(dto.getStatus()) : Enumerations.DocumentReferenceStatus.CURRENT);
        if (dto.getDocStatus() != null && !dto.getDocStatus().isEmpty()) d.setDocStatus(DocumentReference.ReferredDocumentStatus.fromCode(dto.getDocStatus()));
        CodeableConcept type = new CodeableConcept();
        if (dto.getType() != null) { type.addCoding().setSystem("http://loinc.org").setDisplay(dto.getType()); type.setText(dto.getType()); } d.setType(type);
        if (dto.getCategory() != null && !dto.getCategory().isEmpty()) { CodeableConcept cat = new CodeableConcept(); cat.addCoding().setDisplay(dto.getCategory()); cat.setText(dto.getCategory()); d.addCategory(cat); }
        d.setDescription(dto.getDescription()); d.setDate(new Date());
        if (dto.getPractitionerId() != null && !dto.getPractitionerId().isEmpty()) d.addAuthor().setReference("Practitioner/" + dto.getPractitionerId()).setDisplay(dto.getPractitionerName());
        if (dto.getEncounterId() != null && !dto.getEncounterId().isEmpty()) { DocumentReference.DocumentReferenceContextComponent ctx = new DocumentReference.DocumentReferenceContextComponent(); ctx.addEncounter().setReference("Encounter/" + dto.getEncounterId()); d.setContext(ctx); }
        // Content / Attachment
        DocumentReference.DocumentReferenceContentComponent contentComp = new DocumentReference.DocumentReferenceContentComponent();
        Attachment att = new Attachment();
        if (dto.getContentType() != null && !dto.getContentType().isEmpty()) att.setContentType(dto.getContentType()); else att.setContentType("text/plain");
        if (dto.getTitle() != null) att.setTitle(dto.getTitle());
        if (dto.getUrl() != null && !dto.getUrl().isEmpty()) att.setUrl(dto.getUrl());
        if (dto.getContent() != null && !dto.getContent().isEmpty()) att.setData(dto.getContent().getBytes());
        if (dto.getLanguage() != null && !dto.getLanguage().isEmpty()) att.setLanguage(dto.getLanguage());
        contentComp.setAttachment(att); d.addContent(contentComp);
        if (dto.getNotes() != null && !dto.getNotes().isEmpty()) d.addExtension("http://emr.local/doc-notes", new StringType(dto.getNotes()));
        return d;
    }
}
