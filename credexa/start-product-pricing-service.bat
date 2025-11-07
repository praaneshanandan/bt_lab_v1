@echo off
cd /d "%~dp0product-pricing-service"
echo Starting Product Pricing Service...
..\mvnw.cmd spring-boot:run
pause
