package com.app.account.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.app.account.dto.external.CalculationRequest;
import com.app.account.dto.external.CalculationResponse;
import com.app.common.dto.ApiResponse;

/**
 * Client for calculator-service integration
 */
@Component
public class CalculatorServiceClient {

    private static final Logger logger = LoggerFactory.getLogger(CalculatorServiceClient.class);

    private final WebClient webClient;
    private final String calculatorServiceUrl;

    public CalculatorServiceClient(WebClient.Builder webClientBuilder,
                                    @Value("${integration.calculator-service.url}") String calculatorServiceUrl) {
        this.calculatorServiceUrl = calculatorServiceUrl;
        this.webClient = webClientBuilder.baseUrl(calculatorServiceUrl).build();
    }

    /**
     * Calculate FD maturity details
     */
    public CalculationResponse calculateMaturity(CalculationRequest request) {
        logger.info("🔍 Calculating FD maturity: Principal={}, Rate={}, Tenure={} {}", 
                request.getPrincipalAmount(), request.getInterestRate(), request.getTenure(), request.getTenureUnit());

        try {
            ApiResponse<CalculationResponse> response = webClient.post()
                    .uri("/calculate/standalone")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(new org.springframework.core.ParameterizedTypeReference<ApiResponse<CalculationResponse>>() {})
                    .block();

            if (response != null && response.isSuccess() && response.getData() != null) {
                CalculationResponse calculation = response.getData();
                logger.info("✅ Calculation completed: Maturity Amount={}, Interest={}", 
                        calculation.getMaturityAmount(), calculation.getInterestEarned());
                return calculation;
            } else {
                logger.error("❌ Calculation failed or invalid response");
                throw new RuntimeException("Failed to calculate FD maturity");
            }
        } catch (Exception e) {
            logger.error("❌ Error during FD calculation: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to calculate FD maturity: " + e.getMessage(), e);
        }
    }
}
