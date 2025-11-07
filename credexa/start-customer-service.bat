@echo off
cd /d "%~dp0customer-service"
echo Starting Customer Service...
..\mvnw.cmd spring-boot:run
pause
