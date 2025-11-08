package com.app.product.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.app.product.dto.ApiResponse;
import com.app.product.dto.CreateProductRequest;
import com.app.product.dto.ProductListResponse;
import com.app.product.dto.ProductResponse;
import com.app.product.dto.ProductSearchCriteria;
import com.app.product.dto.ProductSummaryResponse;
import com.app.product.dto.UpdateProductRequest;
import com.app.product.enums.ProductStatus;
import com.app.product.enums.ProductType;
import com.app.product.service.ProductService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * REST Controller for Product operations (Lab L9 Enhanced)
 * Provides comprehensive CRUD and search operations for products
 * 
 * Lab L9 Enhancement: Secure integration with Login System
 * - JWT authentication required for all endpoints
 * - Role-based authorization (ADMIN, MANAGER, CUSTOMER)
 * - Audit tracking with user context
 */
@RestController
@RequestMapping  // Empty mapping since context-path is already /api/products
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Product Management", description = "APIs for managing banking products (Lab L9: Secure Integration)")
@SecurityRequirement(name = "Bearer Authentication")
public class ProductController {

    private final ProductService productService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Create a new product (ADMIN/MANAGER only)", 
               description = "Creates a new banking product with all configuration including roles, charges, and interest rate matrix")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Product created successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid product data"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - JWT token missing or invalid"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Insufficient permissions"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Product code already exists")
    })
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(
            @Valid @RequestBody CreateProductRequest request,
            Authentication authentication) {
        
        String username = authentication != null ? authentication.getName() : "SYSTEM";
        log.info("REST: Creating new product with code: {} by user: {}", request.getProductCode(), username);
        
        ProductResponse response = productService.createProduct(request, username);
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Product created successfully by " + username, response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Update an existing product (ADMIN/MANAGER only)", 
               description = "Updates product details. Product code and type cannot be changed.")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Product updated successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid update data"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - JWT token missing or invalid"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Insufficient permissions"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Product not found")
    })
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(
            @Parameter(description = "Product ID") @PathVariable Long id,
            @Valid @RequestBody UpdateProductRequest request,
            Authentication authentication) {
        
        String username = authentication != null ? authentication.getName() : "SYSTEM";
        log.info("REST: Updating product ID: {} by user: {}", id, username);
        
        ProductResponse response = productService.updateProduct(id, request, username);
        
        return ResponseEntity.ok(ApiResponse.success("Product updated successfully by " + username, response));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'CUSTOMER')")
    @Operation(summary = "Get product by ID", 
               description = "Retrieves complete product details including all configurations")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Product found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Product not found")
    })
    public ResponseEntity<ApiResponse<ProductResponse>> getProductById(
            @Parameter(description = "Product ID") @PathVariable Long id) {
        
        log.info("REST: Fetching product ID: {}", id);
        ProductResponse response = productService.getProductById(id);
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/code/{code}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'CUSTOMER')")
    @Operation(summary = "Get product by code", 
               description = "Retrieves product by unique product code")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Product found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Product not found")
    })
    public ResponseEntity<ApiResponse<ProductResponse>> getProductByCode(
            @Parameter(description = "Product Code") @PathVariable String code) {
        
        log.info("REST: Fetching product by code: {}", code);
        ProductResponse response = productService.getProductByCode(code);
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'CUSTOMER')")
    @Operation(summary = "Get all products", 
               description = "Retrieves paginated list of all products")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Products retrieved successfully")
    })
    public ResponseEntity<ApiResponse<ProductListResponse>> getAllProducts(
            @Parameter(description = "Page number (0-indexed)") 
            @RequestParam(defaultValue = "0") int page,
            
            @Parameter(description = "Page size") 
            @RequestParam(defaultValue = "20") int size,
            
            @Parameter(description = "Sort field") 
            @RequestParam(defaultValue = "createdAt") String sortBy,
            
            @Parameter(description = "Sort direction (ASC/DESC)") 
            @RequestParam(defaultValue = "DESC") String sortDirection) {
        
        log.info("REST: Fetching all products - page: {}, size: {}", page, size);
        ProductListResponse response = productService.getAllProducts(page, size, sortBy, sortDirection);
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'CUSTOMER')")
    @Operation(summary = "Search products", 
               description = "Search products with multiple criteria including name, code, type, status, dates, amounts")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Search completed successfully")
    })
    public ResponseEntity<ApiResponse<ProductListResponse>> searchProducts(
            @RequestBody ProductSearchCriteria criteria) {
        
        log.info("REST: Searching products with criteria");
        ProductListResponse response = productService.searchProducts(criteria);
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/type/{type}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'CUSTOMER')")
    @Operation(summary = "Get products by type", 
               description = "Retrieves all products of a specific FD type (FIXED_DEPOSIT, TAX_SAVER_FD, SENIOR_CITIZEN_FD, FLEXI_FD, CUMULATIVE_FD, NON_CUMULATIVE_FD)")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Products retrieved successfully")
    })
    public ResponseEntity<ApiResponse<List<ProductSummaryResponse>>> getProductsByType(
            @Parameter(description = "FD Product Type") @PathVariable ProductType type) {
        
        log.info("REST: Fetching products by type: {}", type);
        List<ProductSummaryResponse> response = productService.getProductsByType(type);
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'CUSTOMER')")
    @Operation(summary = "Get products by status", 
               description = "Retrieves all products with specific status (DRAFT, ACTIVE, INACTIVE, etc.)")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Products retrieved successfully")
    })
    public ResponseEntity<ApiResponse<List<ProductSummaryResponse>>> getProductsByStatus(
            @Parameter(description = "Product Status") @PathVariable ProductStatus status) {
        
        log.info("REST: Fetching products by status: {}", status);
        List<ProductSummaryResponse> response = productService.getProductsByStatus(status);
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/active")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'CUSTOMER')")
    @Operation(summary = "Get all active products", 
               description = "Retrieves all products with ACTIVE status")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Active products retrieved successfully")
    })
    public ResponseEntity<ApiResponse<List<ProductSummaryResponse>>> getActiveProducts() {
        
        log.info("REST: Fetching all active products");
        List<ProductSummaryResponse> response = productService.getActiveProducts();
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/currently-active")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'CUSTOMER')")
    @Operation(summary = "Get currently active products", 
               description = "Retrieves products that are ACTIVE and have reached their effective date")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Currently active products retrieved successfully")
    })
    public ResponseEntity<ApiResponse<List<ProductSummaryResponse>>> getCurrentlyActiveProducts() {
        
        log.info("REST: Fetching currently active products");
        List<ProductSummaryResponse> response = productService.getCurrentlyActiveProducts();
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Update product status (ADMIN/MANAGER only)", 
               description = "Changes the status of a product (DRAFT, ACTIVE, INACTIVE, SUSPENDED, CLOSED)")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Status updated successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - JWT token missing or invalid"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Insufficient permissions"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Product not found")
    })
    public ResponseEntity<ApiResponse<ProductResponse>> updateProductStatus(
            @Parameter(description = "Product ID") @PathVariable Long id,
            @Parameter(description = "New status") @RequestParam ProductStatus status,
            Authentication authentication) {
        
        String username = authentication != null ? authentication.getName() : "SYSTEM";
        log.info("REST: Updating product status - ID: {}, New Status: {} by user: {}", id, status, username);
        
        ProductResponse response = productService.updateProductStatus(id, status, username);
        
        return ResponseEntity.ok(ApiResponse.success("Product status updated successfully by " + username, response));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Delete product (ADMIN/MANAGER only)", 
               description = "Soft deletes a product by setting status to CLOSED")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Product deleted successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - JWT token missing or invalid"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Insufficient permissions"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Product not found")
    })
    public ResponseEntity<ApiResponse<Void>> deleteProduct(
            @Parameter(description = "Product ID") @PathVariable Long id,
            Authentication authentication) {
        
        String username = authentication != null ? authentication.getName() : "SYSTEM";
        log.info("REST: Deleting product ID: {} by user: {}", id, username);
        
        productService.deleteProduct(id, username);
        
        return ResponseEntity.ok(ApiResponse.success("Product deleted successfully by " + username, null));
    }

    @DeleteMapping("/{id}/hard")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Hard delete product (ADMIN only)", 
               description = "Permanently deletes a product from database. Use with extreme caution!")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Product permanently deleted"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - JWT token missing or invalid"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - ADMIN role required"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Product not found")
    })
    public ResponseEntity<ApiResponse<Void>> hardDeleteProduct(
            @Parameter(description = "Product ID") @PathVariable Long id,
            Authentication authentication) {
        
        String username = authentication != null ? authentication.getName() : "SYSTEM";
        log.warn("REST: Hard deleting product ID: {} by user: {}", id, username);
        
        productService.hardDeleteProduct(id, username);
        
        return ResponseEntity.ok(ApiResponse.success("Product permanently deleted by " + username, null));
    }
}
