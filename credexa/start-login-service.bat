@echo off
cd /d "%~dp0login-service"
echo Starting Login Service...
..\mvnw.cmd spring-boot:run
pause
