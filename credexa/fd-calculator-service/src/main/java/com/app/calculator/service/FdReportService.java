package com.app.calculator.service;

import com.app.calculator.dto.CalculationResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Lab L11: FD Report Generation Service
 * Integrates Python script execution for generating CSV reports
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class FdReportService {

    private final ObjectMapper objectMapper;
    
    @Value("${report.script.path:scripts/generate_fd_report.py}")
    private String scriptPath;
    
    @Value("${report.output.directory:reports}")
    private String reportOutputDirectory;
    
    @Value("${report.python.command:python}")
    private String pythonCommand;
    
    /**
     * Lab L11: Generate FD calculation report using Python script
     * 
     * @param calculations List of FD calculations
     * @param username Username for report identification
     * @return Path to generated CSV report
     */
    public String generateReport(List<CalculationResponse> calculations, String username) {
        log.info("Lab L11: Generating FD report for user: {} with {} calculations", username, calculations.size());
        
        try {
            // Create reports directory if it doesn't exist
            Path reportDir = Paths.get(reportOutputDirectory);
            if (!Files.exists(reportDir)) {
                Files.createDirectories(reportDir);
            }
            
            // Create temporary JSON file with calculation data
            String timestamp = String.valueOf(System.currentTimeMillis());
            String jsonFileName = String.format("fd_calculations_%s_%s.json", username, timestamp);
            Path jsonFilePath = reportDir.resolve(jsonFileName);
            
            // Prepare calculation data for Python script
            objectMapper.writeValue(jsonFilePath.toFile(), calculations);
            
            // Output CSV file path
            String csvFileName = String.format("fd_report_%s_%s.csv", username, timestamp);
            Path csvFilePath = reportDir.resolve(csvFileName);
            
            // Execute Python script
            boolean success = executePythonScript(jsonFilePath.toString(), csvFilePath.toString());
            
            if (success) {
                log.info("Lab L11: Report generated successfully: {}", csvFilePath);
                
                // Clean up temporary JSON file
                Files.deleteIfExists(jsonFilePath);
                
                return csvFilePath.toString();
            } else {
                log.error("Lab L11: Failed to generate report");
                return null;
            }
            
        } catch (Exception e) {
            log.error("Lab L11: Error generating report: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Execute Python script using Runtime.exec()
     * 
     * @param inputJsonFile Path to input JSON file
     * @param outputCsvFile Path to output CSV file
     * @return true if execution successful, false otherwise
     */
    private boolean executePythonScript(String inputJsonFile, String outputCsvFile) {
        try {
            // Build command
            String[] command = {
                pythonCommand,
                scriptPath,
                inputJsonFile,
                outputCsvFile
            };
            
            log.info("Lab L11: Executing Python script: {} {} {} {}", 
                    command[0], command[1], command[2], command[3]);
            
            // Execute command
            Process process = Runtime.getRuntime().exec(command);
            
            // Capture output
            BufferedReader stdInput = new BufferedReader(new InputStreamReader(process.getInputStream()));
            BufferedReader stdError = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            
            // Wait for process to complete (with timeout)
            boolean completed = process.waitFor(30, TimeUnit.SECONDS);
            
            if (!completed) {
                log.error("Lab L11: Python script execution timed out");
                process.destroy();
                return false;
            }
            
            int exitCode = process.exitValue();
            
            // Log output
            String line;
            StringBuilder output = new StringBuilder();
            while ((line = stdInput.readLine()) != null) {
                output.append(line).append("\n");
            }
            
            StringBuilder errorOutput = new StringBuilder();
            while ((line = stdError.readLine()) != null) {
                errorOutput.append(line).append("\n");
            }
            
            if (exitCode == 0) {
                log.info("Lab L11: Python script executed successfully");
                log.debug("Lab L11: Script output:\n{}", output);
                return true;
            } else {
                log.error("Lab L11: Python script failed with exit code: {}", exitCode);
                log.error("Lab L11: Error output:\n{}", errorOutput);
                return false;
            }
            
        } catch (IOException e) {
            log.error("Lab L11: IOException executing Python script: {}", e.getMessage());
            return false;
        } catch (InterruptedException e) {
            log.error("Lab L11: Python script execution interrupted: {}", e.getMessage());
            Thread.currentThread().interrupt();
            return false;
        }
    }
    
    /**
     * Generate report from single calculation
     * 
     * @param calculation Single FD calculation
     * @param username Username
     * @return Path to generated report
     */
    public String generateReport(CalculationResponse calculation, String username) {
        return generateReport(List.of(calculation), username);
    }
    
    /**
     * Read generated CSV report content
     * 
     * @param reportPath Path to CSV report
     * @return CSV content as string
     */
    public String readReport(String reportPath) {
        try {
            return Files.readString(Paths.get(reportPath));
        } catch (IOException e) {
            log.error("Lab L11: Error reading report: {}", e.getMessage());
            return null;
        }
    }
}
