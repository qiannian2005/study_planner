@echo off
setlocal

cd /d "%~dp0"

echo Starting backend with Maven...
mvn spring-boot:run

pause
