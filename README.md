# FHIR EMR System

A Spring Boot-based Electronic Medical Records (EMR) system integrated with HL7 FHIR R4 standards.

## Overview

This application provides a comprehensive Electronic Medical Records system built with:
- **Framework**: Spring Boot 6.1.1
- **Standards**: HL7 FHIR R4
- **Template Engine**: Thymeleaf 3.1.2
- **Database**: FHIR-compliant HAPI FHIR Server
- **Build Tool**: Maven

## Features

### Core FHIR Resources
- **Patients**: Patient demographics and identification
- **Practitioners**: Healthcare provider management
- **Organizations**: Healthcare facility management
- **Appointments**: Appointment scheduling and management
- **Encounters**: Patient encounter tracking
- **Conditions**: Patient medical conditions/diagnoses
- **MedicationRequest**: Prescription management
- **AllergyIntolerance**: Patient allergies and intolerances
- **Procedures**: Medical procedures
- **Documents**: Clinical document references
- **Investigations**: Lab orders and results

### Advanced Features
- **Medical Autocomplete**: SNOMED-CT code autocomplete for medications, allergies, procedures, conditions
- **LOINC Codes**: Laboratory test code autocomplete (35+ standard tests)
- **ICD-10**: Condition/diagnosis code search
- **Audit Logging**: FHIR AuditEvent tracking
- **Dashboard**: Real-time statistics from FHIR server
- **Light Blue Theme**: Modern, medical-grade UI

## Prerequisites

- Java 17 or higher
- Maven 3.8.9 or higher
- FHIR Server (HAPI FHIR or compatible)

## Installation

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd fhir-emr-system
   ```

2. **Configure FHIR Server**
   Update `application.properties`:
   ```properties
   fhir.server.url=http://localhost:8080/fhir
   ```

3. **Build the project**
   ```bash
   mvn clean install
   ```

4. **Run the application**
   ```bash
   java -jar target/fhir-emr-system-*.jar
   ```
   Or use Maven:
   ```bash
   mvn spring-boot:run
   ```

5. **Access the application**
   ```
   http://127.0.0.1:8080
   ```

## Project Structure

```
src/main/
├── java/com/emr/fhir/
│   ├── FhirEmrApplication.java
│   ├── config/                 # FHIR client configuration
│   ├── controller/             # Web controllers
│   ├── service/                # Business logic services
│   └── dto/                    # Data Transfer Objects
├── resources/
│   ├── application.properties  # Application configuration
│   ├── static/
│   │   ├── css/               # Stylesheets
│   │   └── js/                # JavaScript (autocomplete, utilities)
│   └── templates/             # Thymeleaf templates
│       ├── patients/
│       ├── practitioners/
│       ├── appointments/
│       ├── conditions/
│       ├── allergies/
│       ├── prescriptions/
│       ├── procedures/
│       ├── investigations/
│       ├── encounters/
│       ├── documents/
│       ├── organizations/
│       ├── family-history/
│       ├── audit/
│       └── fragments/         # Reusable template fragments
```

## Key Technologies

### Backend
- Spring Boot 6.1.1
- Spring Web MVC
- HAPI FHIR Client Library (HL7 FHIR R4)
- Jakarta EE

### Frontend
- Thymeleaf 3.1.2
- CSS3 with CSS Variables for theming
- Font Awesome 6.4.0
- Vanilla JavaScript (no jQuery/frameworks)

### FHIR Integration
- SNOMED-CT codes (medical terminology)
- LOINC codes (laboratory testing)
- ICD-10 codes (diagnoses)
- FHIR R4 bundles and resources

## Autocomplete Features

### Medical Autocomplete System
- **SNOMED-CT Integration**: 150+ medical codes across categories
  - Medications (20+ common drugs)
  - Allergies (20+ common allergens)
  - Procedures (10+ procedures)
  - Body Sites (30+ anatomical locations)
  - Conditions (20+ diagnoses)

- **LOINC Codes**: 35+ laboratory tests
  - CBC, BMP, CMP, Lipid Panel
  - Thyroid, Liver, Renal function
  - Glucose, HbA1c, and more

- **ICD-10 Search**: Diagnosis code search with backend endpoint

## Color Scheme

The application uses a light blue theme for a professional medical appearance:
- **Primary**: #3b82f6 (Sky Blue)
- **Dark Accent**: #1e40af (Dark Blue)
- **Light Accent**: #60a5fa (Light Sky Blue)
- **Backgrounds**: Light blue tints (#eff6ff, #dbeafe)

## Configuration

### FHIR Server Configuration
Edit `src/main/resources/application.properties`:
```properties
# FHIR Server URL
fhir.server.url=http://localhost:8080/fhir

# Server port
server.port=8080

# Application name
spring.application.name=MediCare ERP
```

## Build & Deployment

### Development Build
```bash
mvn clean install
```

### Clean Build (removes old artifacts)
```bash
mvn clean -q && mvn -DskipTests compile
```

### Run Tests
```bash
mvn test
```

### Skip Tests (faster build)
```bash
mvn -DskipTests compile
```

## API Endpoints

### Dashboard
- `GET /` - Dashboard with FHIR resource counts

### Patient Management
- `GET /patients` - List all patients
- `POST /patients/save` - Save patient
- `GET /patients/{id}` - View patient details
- `GET /allergies/patient/{id}` - Patient allergies
- `GET /conditions/patient/{id}` - Patient conditions
- `GET /prescriptions/patient/{id}` - Patient prescriptions

### Practitioners
- `GET /practitioners` - List all practitioners
- `POST /practitioners/save` - Save practitioner

### Appointments
- `GET /appointments` - List all appointments
- `POST /appointments/save` - Save appointment

### Autocomplete
- `GET /api/icd10/search?q=<query>` - ICD-10 code search
- `GET /api/snomed/search?q=<query>` - SNOMED-CT search

### Audit
- `GET /audit` - Audit log entries

## Known Issues & Limitations

1. **FHIR Server Indexing Delay**: New resources may have slight delay before appearing in searches (use dedicated local FHIR server for instant indexing)

## Future Enhancements

- [ ] User authentication and authorization
- [ ] Advanced search with filters
- [ ] Patient portal
- [ ] Mobile-responsive improvements
- [ ] PDF report generation
- [ ] HL7 v2 message support
- [ ] DICOM integration for imaging

## Documentation

- `AUTOCOMPLETE-IMPLEMENTATION.md` - Autocomplete system design
- `AUTOCOMPLETE-COMPLETE.md` - Complete autocomplete codes reference

## License

This project is part of the Akhester healthcare system.

## Support

For issues, questions, or contributions, please contact the development team.

## Version History

- **v1.0.0** - Initial release with core FHIR resources and autocomplete system
  - Dashboard with live FHIR counts
  - Comprehensive autocomplete for medical terms
  - Light blue theme UI
  - Full audit logging

---

**Last Updated**: February 18, 2026
