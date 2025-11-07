@echo off
cd /d "%~dp0fd-account-service"
echo Starting FD Account Service...
..\mvnw.cmd spring-boot:run
pause
