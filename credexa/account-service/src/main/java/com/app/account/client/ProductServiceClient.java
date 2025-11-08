package com.app.account.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.app.account.dto.external.ProductDto;
import com.app.common.dto.ApiResponse;

/**
 * Client for product-pricing-service integration
 */
@Component
public class ProductServiceClient {

    private static final Logger logger = LoggerFactory.getLogger(ProductServiceClient.class);

    private final WebClient webClient;
    private final String productServiceUrl;

    public ProductServiceClient(WebClient.Builder webClientBuilder,
                                 @Value("${integration.product-service.url}") String productServiceUrl) {
        this.productServiceUrl = productServiceUrl;
        this.webClient = webClientBuilder.baseUrl(productServiceUrl).build();
    }

    /**
     * Get product details by product code
     */
    public ProductDto getProductByCode(String productCode) {
        logger.info("🔍 Fetching product details for code: {}", productCode);

        try {
            ApiResponse<ProductDto> response = webClient.get()
                    .uri("/products/code/{productCode}", productCode)
                    .retrieve()
                    .bodyToMono(new org.springframework.core.ParameterizedTypeReference<ApiResponse<ProductDto>>() {})
                    .block();

            if (response != null && response.isSuccess() && response.getData() != null) {
                ProductDto product = response.getData();
                logger.info("✅ Product details fetched successfully: {} - {}", product.getProductCode(), product.getProductName());
                return product;
            } else {
                logger.error("❌ Product not found or invalid response for code: {}", productCode);
                throw new RuntimeException("Product not found with code: " + productCode);
            }
        } catch (Exception e) {
            logger.error("❌ Error fetching product with code {}: {}", productCode, e.getMessage(), e);
            throw new RuntimeException("Failed to fetch product details: " + e.getMessage(), e);
        }
    }

    /**
     * Validate product exists and is active
     */
    public boolean validateProduct(String productCode) {
        try {
            ProductDto product = getProductByCode(productCode);
            if (product == null) {
                return false;
            }
            if (product.getActive() == null || !product.getActive()) {
                logger.warn("⚠️ Product {} is not active", productCode);
                return false;
            }
            return true;
        } catch (Exception e) {
            logger.error("❌ Product validation failed for code: {}", productCode);
            return false;
        }
    }
}
