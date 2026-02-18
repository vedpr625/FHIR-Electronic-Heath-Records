@echo off
echo =========================================
echo   MediCare ERP - FHIR EMR System
echo =========================================
echo.
echo Building...
call mvn clean package -q -DskipTests
if %ERRORLEVEL% NEQ 0 ( echo Build failed! & pause & exit /b 1 )
echo.
echo Open http://localhost:8080 in your browser
echo.
call mvn spring-boot:run
pause
