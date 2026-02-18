#!/bin/bash
echo "========================================="
echo "  MediCare ERP - FHIR EMR System"
echo "========================================="
echo ""
if ! command -v java &> /dev/null; then echo "ERROR: Java 17+ required"; exit 1; fi
if ! command -v mvn &> /dev/null; then echo "ERROR: Maven 3.8+ required"; exit 1; fi
echo "Building..."
mvn clean package -q -DskipTests
if [ $? -ne 0 ]; then echo "Build failed!"; exit 1; fi
echo ""
echo "Open http://localhost:8080 in your browser"
echo ""
mvn spring-boot:run
