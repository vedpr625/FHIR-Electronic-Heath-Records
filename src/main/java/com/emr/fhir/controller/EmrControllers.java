package com.emr.fhir.controller;
import com.emr.fhir.dto.*;
import com.emr.fhir.service.*;
import com.emr.fhir.dto.EncounterDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
class HomeController {
    @Autowired PatientService patientService;
    @Autowired PractitionerService practitionerService;
    @Autowired AppointmentService appointmentService;
    @Autowired MedicationRequestService medicationRequestService;
    @Autowired OrganizationService organizationService;
    @Autowired EncounterService encounterService;
    @Autowired AuditService auditService;
    
    @GetMapping("/")
    public String home(Model m) {
        try {
            m.addAttribute("patientCount", patientService.getAllPatients().size());
            m.addAttribute("practitionerCount", practitionerService.getAllPractitioners().size());
            m.addAttribute("appointmentCount", appointmentService.getAllAppointments().size());
            m.addAttribute("prescriptionCount", medicationRequestService.getAllPrescriptions().size());
            m.addAttribute("organizationCount", organizationService.getAllOrganizations().size());
            m.addAttribute("encounterCount", encounterService.getAllEncounters().size());
            m.addAttribute("auditCount", auditService.getAuditEvents().size());
        } catch (Exception e) {
            System.out.println("[DASHBOARD] Error loading counts: " + e.getMessage());
        }
        return "index";
    }
}

@Controller
@RequestMapping("/patients")
class PatientController {
    @Autowired PatientService patientService;
    @Autowired PractitionerService practitionerService;
    @Autowired AllergyIntoleranceService allergyService;

    @GetMapping
    public String list(Model m, @RequestParam(required=false) String search) {
        m.addAttribute("patients", search != null && !search.isEmpty() ? patientService.searchPatients(search) : patientService.getAllPatients());
        m.addAttribute("search", search); m.addAttribute("activePage", "patients"); return "patients/list";
    }
    @GetMapping("/{id}")
    public String view(@PathVariable String id, Model m) {
        PatientDTO p = patientService.getPatientById(id);
        m.addAttribute("patient", p); m.addAttribute("activePage", "patients");
        m.addAttribute("allergies", allergyService.getAllergiesForPatient(id));
        m.addAttribute("activeTab", "overview"); return "patients/view";
    }
    @GetMapping("/new")
    public String newForm(Model m) { m.addAttribute("patient", new PatientDTO()); m.addAttribute("activePage", "patients"); return "patients/form"; }
    @PostMapping("/save")
    public String save(@ModelAttribute PatientDTO p, RedirectAttributes ra) {
        try { if (p.getId() == null || p.getId().isEmpty()) { p.setActive(true); patientService.createPatient(p); ra.addFlashAttribute("success", "Patient registered successfully!"); } else { patientService.updatePatient(p); ra.addFlashAttribute("success", "Patient updated!"); } } catch (Exception e) { ra.addFlashAttribute("error", "Error: " + e.getMessage()); }
        return "redirect:/patients";
    }
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable String id, Model m) { m.addAttribute("patient", patientService.getPatientById(id)); m.addAttribute("activePage", "patients"); return "patients/form"; }
    @GetMapping("/delete/{id}")
    public String delete(@PathVariable String id, RedirectAttributes ra) { try { patientService.deletePatient(id); ra.addFlashAttribute("success", "Patient deleted."); } catch (Exception e) { ra.addFlashAttribute("error", "Error: " + e.getMessage()); } return "redirect:/patients"; }
}

@Controller
@RequestMapping("/practitioners")
class PractitionerController {
    @Autowired PractitionerService practitionerService;
    @GetMapping public String list(Model m) { m.addAttribute("practitioners", practitionerService.getAllPractitioners()); m.addAttribute("activePage", "practitioners"); return "practitioners/list"; }
    @GetMapping("/{id}") public String view(@PathVariable String id, Model m) { m.addAttribute("practitioner", practitionerService.getPractitionerById(id)); m.addAttribute("activePage", "practitioners"); return "practitioners/view"; }
    @GetMapping("/new") public String newForm(Model m) { m.addAttribute("practitioner", new PractitionerDTO()); m.addAttribute("activePage", "practitioners"); return "practitioners/form"; }
    @PostMapping("/save") public String save(@ModelAttribute PractitionerDTO p, RedirectAttributes ra) { try { if (p.getId() == null || p.getId().isEmpty()) { p.setActive(true); practitionerService.createPractitioner(p); ra.addFlashAttribute("success", "Practitioner registered!"); } else { practitionerService.updatePractitioner(p); ra.addFlashAttribute("success", "Updated!"); } } catch(Exception e) { ra.addFlashAttribute("error","Error: "+e.getMessage()); } return "redirect:/practitioners"; }
    @GetMapping("/edit/{id}") public String edit(@PathVariable String id, Model m) { m.addAttribute("practitioner", practitionerService.getPractitionerById(id)); m.addAttribute("activePage", "practitioners"); return "practitioners/form"; }
    @GetMapping("/delete/{id}") public String delete(@PathVariable String id, RedirectAttributes ra) { try { practitionerService.deletePractitioner(id); ra.addFlashAttribute("success","Deleted."); } catch(Exception e){ ra.addFlashAttribute("error","Error: "+e.getMessage()); } return "redirect:/practitioners"; }
}

@Controller
@RequestMapping("/organizations")
class OrganizationController {
    @Autowired OrganizationService organizationService;
    @GetMapping public String list(Model m) { m.addAttribute("organizations", organizationService.getAllOrganizations()); m.addAttribute("activePage", "organizations"); return "organizations/list"; }
    @GetMapping("/new") public String newForm(Model m) { m.addAttribute("organization", new OrganizationDTO()); m.addAttribute("activePage", "organizations"); return "organizations/form"; }
    @PostMapping("/save") public String save(@ModelAttribute OrganizationDTO o, RedirectAttributes ra) { try { if (o.getId() == null || o.getId().isEmpty()) { o.setActive(true); organizationService.createOrganization(o); ra.addFlashAttribute("success","Organization created!"); } else { organizationService.updateOrganization(o); ra.addFlashAttribute("success","Updated!"); } } catch(Exception e){ ra.addFlashAttribute("error","Error: "+e.getMessage()); } return "redirect:/organizations"; }
    @GetMapping("/edit/{id}") public String edit(@PathVariable String id, Model m) { m.addAttribute("organization", organizationService.getOrganizationById(id)); m.addAttribute("activePage", "organizations"); return "organizations/form"; }
    @GetMapping("/delete/{id}") public String delete(@PathVariable String id, RedirectAttributes ra) { try { organizationService.deleteOrganization(id); ra.addFlashAttribute("success","Deleted."); } catch(Exception e){ ra.addFlashAttribute("error","Error: "+e.getMessage()); } return "redirect:/organizations"; }
}

@Controller
@RequestMapping("/appointments")
class AppointmentController {
    @Autowired AppointmentService appointmentService;
    @Autowired PatientService patientService;
    @Autowired PractitionerService practitionerService;
    @GetMapping public String list(Model m) { m.addAttribute("appointments", appointmentService.getAllAppointments()); m.addAttribute("activePage","appointments"); return "appointments/list"; }
    @GetMapping("/new") public String newForm(Model m, @RequestParam(required=false) String patientId) { m.addAttribute("appointment", new AppointmentDTO()); m.addAttribute("patients", patientService.getAllPatients()); m.addAttribute("practitioners", practitionerService.getAllPractitioners()); m.addAttribute("selectedPatientId", patientId); m.addAttribute("activePage","appointments"); return "appointments/form"; }
    @PostMapping("/save") public String save(@ModelAttribute AppointmentDTO a, RedirectAttributes ra) { try { if (a.getId()==null||a.getId().isEmpty()) { appointmentService.createAppointment(a); ra.addFlashAttribute("success","Appointment scheduled!"); } else { appointmentService.updateAppointment(a); ra.addFlashAttribute("success","Updated!"); } } catch(Exception e){ ra.addFlashAttribute("error","Error: "+e.getMessage()); } return "redirect:/appointments"; }
    @GetMapping("/edit/{id}") public String edit(@PathVariable String id, Model m) { m.addAttribute("appointment", appointmentService.getAppointmentById(id)); m.addAttribute("patients", patientService.getAllPatients()); m.addAttribute("practitioners", practitionerService.getAllPractitioners()); m.addAttribute("activePage","appointments"); return "appointments/form"; }
    @GetMapping("/delete/{id}") public String delete(@PathVariable String id, RedirectAttributes ra) { try { appointmentService.deleteAppointment(id); ra.addFlashAttribute("success","Deleted."); } catch(Exception e){ ra.addFlashAttribute("error","Error: "+e.getMessage()); } return "redirect:/appointments"; }
}

@Controller
@RequestMapping("/conditions")
class ConditionController {
    @Autowired ConditionService conditionService;
    @Autowired PatientService patientService;
    @Autowired PractitionerService practitionerService;
    @Autowired AllergyIntoleranceService allergyService;
    // Patient-context: /conditions/patient/{patientId}
    @GetMapping("/patient/{patientId}")
    public String listForPatient(@PathVariable String patientId, Model m) {
        PatientDTO patient = patientService.getPatientById(patientId);
        m.addAttribute("patient", patient);
        if (patient != null) m.addAttribute("allergies", allergyService.getAllergiesForPatient(patient.getId())); m.addAttribute("conditions", conditionService.getConditionsForPatient(patientId));
        m.addAttribute("activePage","patients"); m.addAttribute("activeTab","conditions"); return "conditions/list";
    }
    @GetMapping("/patient/{patientId}/new")
    public String newForm(@PathVariable String patientId, Model m) {
        ConditionDTO c = new ConditionDTO(); c.setPatientId(patientId);
        PatientDTO patient = patientService.getPatientById(patientId);
        c.setPatientName(patient.getFullName());
        m.addAttribute("condition", c); m.addAttribute("patient", patient);
        if (patient != null) m.addAttribute("allergies", allergyService.getAllergiesForPatient(patient.getId())); m.addAttribute("practitioners", practitionerService.getAllPractitioners());
        m.addAttribute("activePage","patients"); return "conditions/form";
    }
    @PostMapping("/save")
    public String save(@ModelAttribute ConditionDTO c, RedirectAttributes ra) { try { if (c.getId()==null||c.getId().isEmpty()) { conditionService.createCondition(c); ra.addFlashAttribute("success","Diagnosis added!"); } else { conditionService.updateCondition(c); ra.addFlashAttribute("success","Updated!"); } } catch(Exception e){ ra.addFlashAttribute("error","Error: "+e.getMessage()); } return "redirect:/conditions/patient/" + c.getPatientId(); }
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable String id, Model m) { ConditionDTO c = conditionService.getConditionById(id); PatientDTO patient = patientService.getPatientById(c.getPatientId()); m.addAttribute("condition", c); m.addAttribute("patient", patient);
        if (patient != null) m.addAttribute("allergies", allergyService.getAllergiesForPatient(patient.getId())); m.addAttribute("practitioners", practitionerService.getAllPractitioners()); m.addAttribute("activePage","patients"); return "conditions/form"; }
    @GetMapping("/delete/{id}")
    public String delete(@PathVariable String id, @RequestParam String patientId, RedirectAttributes ra) { try { conditionService.deleteCondition(id); ra.addFlashAttribute("success","Diagnosis removed."); } catch(Exception e){ ra.addFlashAttribute("error","Error: "+e.getMessage()); } return "redirect:/conditions/patient/" + patientId; }
}

@Controller
@RequestMapping("/prescriptions")
class MedicationRequestController {
    @Autowired MedicationRequestService medicationRequestService;
    @Autowired PatientService patientService;
    @Autowired PractitionerService practitionerService;
    @Autowired ConditionService conditionService;
    @Autowired AllergyIntoleranceService allergyService;
    @GetMapping("/patient/{patientId}")
    public String listForPatient(@PathVariable String patientId, Model m) {
        PatientDTO patient = patientService.getPatientById(patientId);
        m.addAttribute("patient", patient);
        if (patient != null) m.addAttribute("allergies", allergyService.getAllergiesForPatient(patient.getId())); m.addAttribute("prescriptions", medicationRequestService.getPrescriptionsForPatient(patientId));
        m.addAttribute("activePage","patients"); m.addAttribute("activeTab","prescriptions"); return "prescriptions/list";
    }
    @GetMapping("/patient/{patientId}/new")
    public String newForm(@PathVariable String patientId, Model m) {
        MedicationRequestDTO rx = new MedicationRequestDTO(); rx.setPatientId(patientId);
        PatientDTO patient = patientService.getPatientById(patientId); rx.setPatientName(patient.getFullName());
        m.addAttribute("prescription", rx); m.addAttribute("patient", patient);
        if (patient != null) m.addAttribute("allergies", allergyService.getAllergiesForPatient(patient.getId()));
        m.addAttribute("practitioners", practitionerService.getAllPractitioners());
        m.addAttribute("conditions", conditionService.getConditionsForPatient(patientId));
        m.addAttribute("activePage","patients"); return "prescriptions/form";
    }
    @PostMapping("/save")
    public String save(@ModelAttribute MedicationRequestDTO rx, RedirectAttributes ra) { try { if (rx.getId()==null||rx.getId().isEmpty()) { medicationRequestService.createPrescription(rx); ra.addFlashAttribute("success","Prescription created!"); } else { medicationRequestService.updatePrescription(rx); ra.addFlashAttribute("success","Updated!"); } } catch(Exception e){ ra.addFlashAttribute("error","Error: "+e.getMessage()); } return "redirect:/prescriptions/patient/" + rx.getPatientId(); }
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable String id, Model m) { MedicationRequestDTO rx = medicationRequestService.getPrescriptionById(id); PatientDTO patient = patientService.getPatientById(rx.getPatientId()); m.addAttribute("prescription", rx); m.addAttribute("patient", patient);
        if (patient != null) m.addAttribute("allergies", allergyService.getAllergiesForPatient(patient.getId())); m.addAttribute("practitioners", practitionerService.getAllPractitioners()); m.addAttribute("conditions", conditionService.getConditionsForPatient(rx.getPatientId())); m.addAttribute("activePage","patients"); return "prescriptions/form"; }
    @GetMapping("/delete/{id}")
    public String delete(@PathVariable String id, @RequestParam String patientId, RedirectAttributes ra) { try { medicationRequestService.deletePrescription(id); ra.addFlashAttribute("success","Prescription removed."); } catch(Exception e){ ra.addFlashAttribute("error","Error: "+e.getMessage()); } return "redirect:/prescriptions/patient/" + patientId; }
    @GetMapping
    public String listAll(Model m) { m.addAttribute("prescriptions", medicationRequestService.getAllPrescriptions()); m.addAttribute("activePage","prescriptions"); return "prescriptions/all"; }
}

@Controller
@RequestMapping("/encounters")
class EncounterController {
    @Autowired EncounterService encounterService;
    @Autowired PatientService patientService;
    @Autowired PractitionerService practitionerService;
    @Autowired ConditionService conditionService;
    @Autowired OrganizationService organizationService;
    @Autowired AllergyIntoleranceService allergyService;

    @GetMapping("/patient/{patientId}")
    public String listForPatient(@PathVariable String patientId, Model m) {
        PatientDTO patient = patientService.getPatientById(patientId);
        m.addAttribute("patient", patient);
        if (patient != null) m.addAttribute("allergies", allergyService.getAllergiesForPatient(patient.getId()));
        m.addAttribute("encounters", encounterService.getEncountersForPatient(patientId));
        m.addAttribute("activePage","patients"); m.addAttribute("activeTab","encounters");
        return "encounters/list";
    }
    @GetMapping("/patient/{patientId}/new")
    public String newForm(@PathVariable String patientId, Model m, @RequestParam(required=false) String appointmentId) {
        EncounterDTO enc = new EncounterDTO(); enc.setPatientId(patientId);
        PatientDTO patient = patientService.getPatientById(patientId);
        enc.setPatientName(patient.getFullName());
        if (appointmentId != null) enc.setAppointmentId(appointmentId);
        m.addAttribute("encounter", enc); m.addAttribute("patient", patient);
        if (patient != null) m.addAttribute("allergies", allergyService.getAllergiesForPatient(patient.getId()));
        m.addAttribute("practitioners", practitionerService.getAllPractitioners());
        m.addAttribute("conditions", conditionService.getConditionsForPatient(patientId));
        m.addAttribute("organizations", organizationService.getAllOrganizations());
        m.addAttribute("activePage","patients"); return "encounters/form";
    }
    @PostMapping("/save")
    public String save(@ModelAttribute EncounterDTO enc, RedirectAttributes ra) {
        try { if (enc.getId()==null||enc.getId().isEmpty()) { encounterService.createEncounter(enc); ra.addFlashAttribute("success","Encounter created!"); } else { encounterService.updateEncounter(enc); ra.addFlashAttribute("success","Encounter updated!"); } } catch(Exception e){ ra.addFlashAttribute("error","Error: "+e.getMessage()); }
        return "redirect:/encounters/patient/" + enc.getPatientId();
    }
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable String id, Model m) {
        EncounterDTO enc = encounterService.getEncounterById(id);
        PatientDTO patient = patientService.getPatientById(enc.getPatientId());
        m.addAttribute("encounter", enc); m.addAttribute("patient", patient);
        if (patient != null) m.addAttribute("allergies", allergyService.getAllergiesForPatient(patient.getId()));
        m.addAttribute("practitioners", practitionerService.getAllPractitioners());
        m.addAttribute("conditions", conditionService.getConditionsForPatient(enc.getPatientId()));
        m.addAttribute("organizations", organizationService.getAllOrganizations());
        m.addAttribute("activePage","patients"); return "encounters/form";
    }
    @GetMapping("/delete/{id}")
    public String delete(@PathVariable String id, @RequestParam String patientId, RedirectAttributes ra) {
        try { encounterService.deleteEncounter(id); ra.addFlashAttribute("success","Encounter deleted."); } catch(Exception e){ ra.addFlashAttribute("error","Error: "+e.getMessage()); }
        return "redirect:/encounters/patient/" + patientId;
    }
    @GetMapping
    public String listAll(Model m) { m.addAttribute("encounters", encounterService.getAllEncounters()); m.addAttribute("activePage","encounters"); return "encounters/all"; }
}

// ============================================================
// ALLERGY INTOLERANCE CONTROLLER
// ============================================================
@Controller
@RequestMapping("/allergies")
class AllergyController {
    @Autowired AllergyIntoleranceService allergyService;
    @Autowired PatientService patientService;
    @Autowired PractitionerService practitionerService;

    @GetMapping("/patient/{patientId}")
    public String list(@PathVariable String patientId, Model m) {
        m.addAttribute("patient", patientService.getPatientById(patientId));
        m.addAttribute("allergies", allergyService.getAllergiesForPatient(patientId));
        m.addAttribute("activePage","patients"); return "allergies/list";
    }
    @GetMapping("/patient/{patientId}/new")
    public String newForm(@PathVariable String patientId, Model m) {
        AllergyIntoleranceDTO dto = new AllergyIntoleranceDTO(); dto.setPatientId(patientId);
        m.addAttribute("allergy", dto); m.addAttribute("patient", patientService.getPatientById(patientId));
        m.addAttribute("allergies", allergyService.getAllergiesForPatient(patientId));
        m.addAttribute("practitioners", practitionerService.getAllPractitioners());
        return "allergies/form";
    }
    @PostMapping("/save")
    public String save(@ModelAttribute AllergyIntoleranceDTO dto, RedirectAttributes ra) {
        try { if (dto.getId()==null||dto.getId().isEmpty()) allergyService.createAllergy(dto); else allergyService.updateAllergy(dto); ra.addFlashAttribute("success","Allergy saved!"); }
        catch(Exception e){ ra.addFlashAttribute("error","Error: "+e.getMessage()); }
        return "redirect:/allergies/patient/" + dto.getPatientId();
    }
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable String id, Model m) {
        AllergyIntoleranceDTO dto = allergyService.getAllergyById(id);
        m.addAttribute("allergy", dto); m.addAttribute("patient", patientService.getPatientById(dto.getPatientId()));
        m.addAttribute("allergies", allergyService.getAllergiesForPatient(dto.getPatientId()));
        m.addAttribute("practitioners", practitionerService.getAllPractitioners());
        return "allergies/form";
    }
    @GetMapping("/delete/{id}")
    public String delete(@PathVariable String id, @RequestParam String patientId, RedirectAttributes ra) {
        try { allergyService.deleteAllergy(id); ra.addFlashAttribute("success","Allergy deleted."); } catch(Exception e){ ra.addFlashAttribute("error","Error: "+e.getMessage()); }
        return "redirect:/allergies/patient/" + patientId;
    }
}

// ============================================================
// FAMILY HISTORY (MEDICAL HISTORY) CONTROLLER
// ============================================================
@Controller
@RequestMapping("/family-history")
class FamilyHistoryController {
    @Autowired FamilyHistoryService familyHistoryService;
    @Autowired PatientService patientService;
    @Autowired AllergyIntoleranceService allergyService;

    @GetMapping("/patient/{patientId}")
    public String list(@PathVariable String patientId, Model m) {
        m.addAttribute("patient", patientService.getPatientById(patientId));
        m.addAttribute("allergies", allergyService.getAllergiesForPatient(patientId));
        m.addAttribute("histories", familyHistoryService.getHistoryForPatient(patientId));
        m.addAttribute("activePage","patients"); return "family-history/list";
    }
    @GetMapping("/patient/{patientId}/new")
    public String newForm(@PathVariable String patientId, Model m) {
        FamilyHistoryDTO dto = new FamilyHistoryDTO(); dto.setPatientId(patientId);
        PatientDTO patient = patientService.getPatientById(patientId); dto.setPatientName(patient.getFullName());
        m.addAttribute("history", dto); m.addAttribute("patient", patient);
        if (patient != null) m.addAttribute("allergies", allergyService.getAllergiesForPatient(patient.getId()));
        return "family-history/form";
    }
    @PostMapping("/save")
    public String save(@ModelAttribute FamilyHistoryDTO dto, RedirectAttributes ra) {
        try { if (dto.getId()==null||dto.getId().isEmpty()) familyHistoryService.createHistory(dto); else familyHistoryService.updateHistory(dto); ra.addFlashAttribute("success","Medical history saved!"); }
        catch(Exception e){ ra.addFlashAttribute("error","Error: "+e.getMessage()); }
        return "redirect:/family-history/patient/" + dto.getPatientId();
    }
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable String id, Model m) {
        FamilyHistoryDTO dto = familyHistoryService.getHistoryById(id);
        m.addAttribute("history", dto); m.addAttribute("patient", patientService.getPatientById(dto.getPatientId()));
        m.addAttribute("allergies", allergyService.getAllergiesForPatient(dto.getPatientId()));
        return "family-history/form";
    }
    @GetMapping("/delete/{id}")
    public String delete(@PathVariable String id, @RequestParam String patientId, RedirectAttributes ra) {
        try { familyHistoryService.deleteHistory(id); ra.addFlashAttribute("success","History entry deleted."); } catch(Exception e){ ra.addFlashAttribute("error","Error: "+e.getMessage()); }
        return "redirect:/family-history/patient/" + patientId;
    }
}

// ============================================================
// PROCEDURE CONTROLLER
// ============================================================
@Controller
@RequestMapping("/procedures")
class ProcedureController {
    @Autowired ProcedureService procedureService;
    @Autowired PatientService patientService;
    @Autowired PractitionerService practitionerService;
    @Autowired AllergyIntoleranceService allergyService;

    @GetMapping("/patient/{patientId}")
    public String list(@PathVariable String patientId, Model m) {
        m.addAttribute("patient", patientService.getPatientById(patientId));
        m.addAttribute("allergies", allergyService.getAllergiesForPatient(patientId));
        m.addAttribute("procedures", procedureService.getProceduresForPatient(patientId));
        m.addAttribute("activePage","patients"); return "procedures/list";
    }
    @GetMapping("/patient/{patientId}/new")
    public String newForm(@PathVariable String patientId, Model m) {
        ProcedureDTO dto = new ProcedureDTO(); dto.setPatientId(patientId);
        PatientDTO patient = patientService.getPatientById(patientId); dto.setPatientName(patient.getFullName());
        m.addAttribute("procedure", dto); m.addAttribute("patient", patient);
        if (patient != null) m.addAttribute("allergies", allergyService.getAllergiesForPatient(patient.getId()));
        m.addAttribute("practitioners", practitionerService.getAllPractitioners());
        return "procedures/form";
    }
    @PostMapping("/save")
    public String save(@ModelAttribute ProcedureDTO dto, RedirectAttributes ra) {
        try { if (dto.getId()==null||dto.getId().isEmpty()) procedureService.createProcedure(dto); else procedureService.updateProcedure(dto); ra.addFlashAttribute("success","Procedure saved!"); }
        catch(Exception e){ ra.addFlashAttribute("error","Error: "+e.getMessage()); }
        return "redirect:/procedures/patient/" + dto.getPatientId();
    }
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable String id, Model m) {
        ProcedureDTO dto = procedureService.getProcedureById(id);
        m.addAttribute("procedure", dto); m.addAttribute("patient", patientService.getPatientById(dto.getPatientId()));
        m.addAttribute("allergies", allergyService.getAllergiesForPatient(dto.getPatientId()));
        m.addAttribute("practitioners", practitionerService.getAllPractitioners());
        return "procedures/form";
    }
    @GetMapping("/delete/{id}")
    public String delete(@PathVariable String id, @RequestParam String patientId, RedirectAttributes ra) {
        try { procedureService.deleteProcedure(id); ra.addFlashAttribute("success","Procedure deleted."); } catch(Exception e){ ra.addFlashAttribute("error","Error: "+e.getMessage()); }
        return "redirect:/procedures/patient/" + patientId;
    }
}

// ============================================================
// INVESTIGATION ADVICE (SERVICE REQUEST) CONTROLLER
// ============================================================
@Controller
@RequestMapping("/investigations")
class InvestigationController {
    @Autowired ServiceRequestService serviceRequestService;
    @Autowired PatientService patientService;
    @Autowired PractitionerService practitionerService;
    @Autowired ConditionService conditionService;
    @Autowired AllergyIntoleranceService allergyService;

    @GetMapping("/patient/{patientId}")
    public String list(@PathVariable String patientId, Model m) {
        m.addAttribute("patient", patientService.getPatientById(patientId));
        m.addAttribute("allergies", allergyService.getAllergiesForPatient(patientId));
        m.addAttribute("investigations", serviceRequestService.getRequestsForPatient(patientId));
        m.addAttribute("activePage","patients"); return "investigations/list";
    }
    @GetMapping("/patient/{patientId}/new")
    public String newForm(@PathVariable String patientId, Model m) {
        ServiceRequestDTO dto = new ServiceRequestDTO(); dto.setPatientId(patientId);
        PatientDTO patient = patientService.getPatientById(patientId); dto.setPatientName(patient.getFullName());
        m.addAttribute("request", dto); m.addAttribute("patient", patient);
        if (patient != null) m.addAttribute("allergies", allergyService.getAllergiesForPatient(patient.getId()));
        m.addAttribute("practitioners", practitionerService.getAllPractitioners());
        m.addAttribute("conditions", conditionService.getConditionsForPatient(patientId));
        return "investigations/form";
    }
    @PostMapping("/save")
    public String save(@ModelAttribute ServiceRequestDTO dto, RedirectAttributes ra) {
        try { if (dto.getId()==null||dto.getId().isEmpty()) serviceRequestService.createRequest(dto); else serviceRequestService.updateRequest(dto); ra.addFlashAttribute("success","Investigation order saved!"); }
        catch(Exception e){ ra.addFlashAttribute("error","Error: "+e.getMessage()); }
        return "redirect:/investigations/patient/" + dto.getPatientId();
    }
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable String id, Model m) {
        ServiceRequestDTO dto = serviceRequestService.getRequestById(id);
        m.addAttribute("request", dto); m.addAttribute("patient", patientService.getPatientById(dto.getPatientId()));
        m.addAttribute("allergies", allergyService.getAllergiesForPatient(dto.getPatientId()));
        m.addAttribute("practitioners", practitionerService.getAllPractitioners());
        m.addAttribute("conditions", conditionService.getConditionsForPatient(dto.getPatientId()));
        return "investigations/form";
    }
    @GetMapping("/delete/{id}")
    public String delete(@PathVariable String id, @RequestParam String patientId, RedirectAttributes ra) {
        try { serviceRequestService.deleteRequest(id); ra.addFlashAttribute("success","Investigation order cancelled."); } catch(Exception e){ ra.addFlashAttribute("error","Error: "+e.getMessage()); }
        return "redirect:/investigations/patient/" + patientId;
    }
}

// ============================================================
// DOCUMENT REFERENCE CONTROLLER
// ============================================================
@Controller
@RequestMapping("/documents")
class DocumentController {
    @Autowired DocumentReferenceService documentService;
    @Autowired PatientService patientService;
    @Autowired PractitionerService practitionerService;
    @Autowired AllergyIntoleranceService allergyService;

    @GetMapping("/patient/{patientId}")
    public String list(@PathVariable String patientId, Model m) {
        m.addAttribute("patient", patientService.getPatientById(patientId));
        m.addAttribute("allergies", allergyService.getAllergiesForPatient(patientId));
        m.addAttribute("documents", documentService.getDocumentsForPatient(patientId));
        m.addAttribute("activePage","patients"); return "documents/list";
    }
    @GetMapping("/patient/{patientId}/new")
    public String newForm(@PathVariable String patientId, Model m) {
        DocumentReferenceDTO dto = new DocumentReferenceDTO(); dto.setPatientId(patientId);
        PatientDTO patient = patientService.getPatientById(patientId); dto.setPatientName(patient.getFullName());
        m.addAttribute("document", dto); m.addAttribute("patient", patient);
        if (patient != null) m.addAttribute("allergies", allergyService.getAllergiesForPatient(patient.getId()));
        m.addAttribute("practitioners", practitionerService.getAllPractitioners());
        return "documents/form";
    }
    @PostMapping("/save")
    public String save(@ModelAttribute DocumentReferenceDTO dto, RedirectAttributes ra) {
        try { if (dto.getId()==null||dto.getId().isEmpty()) documentService.createDocument(dto); else documentService.updateDocument(dto); ra.addFlashAttribute("success","Document saved!"); }
        catch(Exception e){ ra.addFlashAttribute("error","Error: "+e.getMessage()); }
        return "redirect:/documents/patient/" + dto.getPatientId();
    }
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable String id, Model m) {
        DocumentReferenceDTO dto = documentService.getDocumentById(id);
        m.addAttribute("document", dto); m.addAttribute("patient", patientService.getPatientById(dto.getPatientId()));
        m.addAttribute("allergies", allergyService.getAllergiesForPatient(dto.getPatientId()));
        m.addAttribute("practitioners", practitionerService.getAllPractitioners());
        return "documents/form";
    }
    @GetMapping("/delete/{id}")
    public String delete(@PathVariable String id, @RequestParam String patientId, RedirectAttributes ra) {
        try { documentService.deleteDocument(id); ra.addFlashAttribute("success","Document deleted."); } catch(Exception e){ ra.addFlashAttribute("error","Error: "+e.getMessage()); }
        return "redirect:/documents/patient/" + patientId;
    }
}

@Controller
@RequestMapping("/audit")
class AuditController {
    @Autowired AuditService auditService;
    @GetMapping public String list(Model m) { m.addAttribute("events", auditService.getAuditEvents()); m.addAttribute("activePage","audit"); return "audit/list"; }
}

@RestController
@RequestMapping("/api")
class AutocompleteController {
    /**
     * ICD-10 autocomplete endpoint
     * Fallback search when external API is unavailable
     */
    @GetMapping("/icd10/search")
    public java.util.List<java.util.Map<String, String>> searchICD10(@RequestParam String q) {
        java.util.List<java.util.Map<String, String>> results = new java.util.ArrayList<>();
        
        if (q == null || q.trim().isEmpty() || q.length() < 2) {
            return results;
        }
        
        String query = q.toLowerCase();
        
        // Common ICD-10 codes for frequent conditions
        java.util.Map<String, String> icd10Map = new java.util.LinkedHashMap<>();
        icd10Map.put("E11", "Type 2 diabetes mellitus");
        icd10Map.put("I10", "Essential (primary) hypertension");
        icd10Map.put("J45", "Asthma");
        icd10Map.put("I50", "Heart failure");
        icd10Map.put("J06", "Acute upper respiratory infections");
        icd10Map.put("M79", "Other and unspecified soft tissue disorders");
        icd10Map.put("K21", "Gastro-esophageal reflux disease");
        icd10Map.put("E78", "Abdominal and pelvic pain");
        icd10Map.put("F41", "Anxiety disorders");
        icd10Map.put("I21", "ST elevation (STEMI) and non-ST elevation (NSTEMI) myocardial infarction");
        icd10Map.put("C34", "Malignant neoplasm of unspecified part of unspecified bronchus or lung");
        icd10Map.put("C50", "Malignant neoplasm of breast");
        icd10Map.put("J44", "Chronic obstructive pulmonary disease");
        icd10Map.put("E66", "Overweight, obesity and other hyperalimentation");
        icd10Map.put("I63", "Cerebral infarction");
        icd10Map.put("M17", "Unilateral primary osteoarthritis of knee");
        icd10Map.put("F32", "Depressive episode");
        icd10Map.put("K80", "Cholelithiasis");
        icd10Map.put("N18", "Chronic kidney disease");
        icd10Map.put("B34", "Viral infection of unspecified site");
        
        // Search by code or display name
        for (java.util.Map.Entry<String, String> entry : icd10Map.entrySet()) {
            String code = entry.getKey();
            String display = entry.getValue();
            
            if (code.toLowerCase().contains(query) || display.toLowerCase().contains(query)) {
                java.util.Map<String, String> item = new java.util.LinkedHashMap<>();
                item.put("code", code);
                item.put("display", display);
                item.put("system", "ICD-10-CM");
                results.add(item);
            }
        }
        
        return results;
    }
}
