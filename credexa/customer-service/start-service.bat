@echo off
cd /d "%~dp0"
echo Starting Customer Service on port 8083...
call ..\mvnw.cmd spring-boot:run
pause
