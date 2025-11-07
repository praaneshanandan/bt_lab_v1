@echo off
cd /d "%~dp0gateway"
echo Starting API Gateway...
..\mvnw.cmd spring-boot:run
pause
