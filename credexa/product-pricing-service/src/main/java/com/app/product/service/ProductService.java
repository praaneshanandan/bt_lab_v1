package com.app.product.service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.app.product.dto.CreateProductRequest;
import com.app.product.dto.InterestRateMatrixRequest;
import com.app.product.dto.ProductListResponse;
import com.app.product.dto.ProductResponse;
import com.app.product.dto.ProductSearchCriteria;
import com.app.product.dto.ProductSummaryResponse;
import com.app.product.dto.UpdateProductRequest;
import com.app.product.entity.Product;
import com.app.product.enums.ProductStatus;
import com.app.product.enums.ProductType;
import com.app.product.exception.DuplicateProductCodeException;
import com.app.product.exception.InvalidProductException;
import com.app.product.exception.ProductNotFoundException;
import com.app.product.mapper.ProductMapper;
import com.app.product.repository.InterestRateMatrixRepository;
import com.app.product.repository.ProductBalanceTypeRepository;
import com.app.product.repository.ProductChargeRepository;
import com.app.product.repository.ProductRepository;
import com.app.product.repository.ProductRoleRepository;
import com.app.product.repository.ProductTransactionTypeRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service layer for Product operations
 * Implements caching strategy for improved performance
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;
    private final InterestRateMatrixRepository interestRateMatrixRepository;
    private final ProductChargeRepository productChargeRepository;
    private final ProductRoleRepository productRoleRepository;
    private final ProductTransactionTypeRepository productTransactionTypeRepository;
    private final ProductBalanceTypeRepository productBalanceTypeRepository;
    private final ProductMapper productMapper;

    /**
     * Create a new product
     * Clears all caches after creation
     */
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "products", allEntries = true),
        @CacheEvict(value = "productsByCode", allEntries = true),
        @CacheEvict(value = "productsByType", allEntries = true),
        @CacheEvict(value = "activeProducts", allEntries = true)
    })
    public ProductResponse createProduct(CreateProductRequest request) {
        return createProduct(request, "SYSTEM");
    }

    /**
     * Create a new product with audit tracking (Lab L9 Enhancement)
     * Clears all caches after creation
     */
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "productsByCode", allEntries = true),
        @CacheEvict(value = "productsByType", allEntries = true),
        @CacheEvict(value = "activeProducts", allEntries = true)
    })
    public ProductResponse createProduct(CreateProductRequest request, String createdBy) {
        log.info("Creating new product with code: {} by user: {}", request.getProductCode(), createdBy);

        // Validate product code uniqueness
        if (productRepository.existsByProductCode(request.getProductCode())) {
            throw new DuplicateProductCodeException(request.getProductCode());
        }

        // Validate business rules
        validateProductBusinessRules(request);

        // Convert DTO to entity and save
        Product product = productMapper.toEntity(request);
        product.setCreatedBy(createdBy);
        product.setUpdatedBy(createdBy);
        Product savedProduct = productRepository.save(product);

        log.info("Product created successfully with ID: {} by user: {}", savedProduct.getId(), createdBy);
        return productMapper.toResponse(savedProduct);
    }

    /**
     * Update an existing product
     * Clears all caches after update
     */
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "products", key = "#productId"),
        @CacheEvict(value = "productsByCode", allEntries = true),
        @CacheEvict(value = "productsByType", allEntries = true),
        @CacheEvict(value = "activeProducts", allEntries = true)
    })
    public ProductResponse updateProduct(Long productId, UpdateProductRequest request) {
        return updateProduct(productId, request, "SYSTEM");
    }

    /**
     * Update an existing product with audit tracking (Lab L9 Enhancement)
     * Clears all caches after update
     */
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "products", key = "#productId"),
        @CacheEvict(value = "productsByCode", allEntries = true),
        @CacheEvict(value = "productsByType", allEntries = true),
        @CacheEvict(value = "activeProducts", allEntries = true)
    })
    public ProductResponse updateProduct(Long productId, UpdateProductRequest request, String updatedBy) {
        log.info("Updating product ID: {} by user: {}", productId, updatedBy);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));

        // Validate business rules for update
        validateProductUpdateRules(product, request);

        // Update entity
        productMapper.updateEntity(product, request);
        product.setUpdatedBy(updatedBy);
        Product updatedProduct = productRepository.save(product);

        log.info("Product updated successfully: {} by user: {}", productId, updatedBy);
        return productMapper.toResponse(updatedProduct);
    }

    /**
     * Get product by ID
     * Cached by product ID
     */
    @Transactional(readOnly = true)
    // @Cacheable(value = "products", key = "#productId")  // Temporarily disabled for debugging
    public ProductResponse getProductById(Long productId) {
        log.info("Fetching product by ID: {}", productId);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));

        return productMapper.toResponse(product);
    }

    /**
     * Get product by code
     * Cached by product code
     */
    @Transactional(readOnly = true)
    // @Cacheable(value = "productsByCode", key = "#productCode")  // Temporarily disabled for debugging
    public ProductResponse getProductByCode(String productCode) {
        log.info("Fetching product by code: {}", productCode);

        Product product = productRepository.findByProductCode(productCode)
                .orElseThrow(() -> new ProductNotFoundException("productCode", productCode));

        return productMapper.toResponse(product);
    }

    /**
     * Get all products with pagination
     */
    public ProductListResponse getAllProducts(int page, int size, String sortBy, String sortDirection) {
        log.info("Fetching all products - page: {}, size: {}", page, size);

        Sort sort = sortDirection.equalsIgnoreCase("ASC") 
                ? Sort.by(sortBy).ascending() 
                : Sort.by(sortBy).descending();
        
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Product> productPage = productRepository.findAll(pageable);

        return buildProductListResponse(productPage);
    }

    /**
     * Search products with multiple criteria
     */
    public ProductListResponse searchProducts(ProductSearchCriteria criteria) {
        log.info("Searching products with criteria: {}", criteria);

        Sort sort = criteria.getSortDirection().equalsIgnoreCase("ASC")
                ? Sort.by(criteria.getSortBy()).ascending()
                : Sort.by(criteria.getSortBy()).descending();

        Pageable pageable = PageRequest.of(criteria.getPage(), criteria.getSize(), sort);

        Page<Product> productPage = productRepository.searchProducts(
                criteria.getProductName(),
                criteria.getProductCode(),
                criteria.getProductType(),
                criteria.getStatus(),
                criteria.getCreatedBy(),
                pageable
        );

        return buildProductListResponse(productPage);
    }

    /**
     * Get products by type
     * Cached by product type
     */
    @Cacheable(value = "productsByType", key = "#productType")
    public List<ProductSummaryResponse> getProductsByType(ProductType productType) {
        log.info("Fetching products by type: {}", productType);

        return productRepository.findByProductType(productType).stream()
                .map(productMapper::toSummaryResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get products by status
     */
    public List<ProductSummaryResponse> getProductsByStatus(ProductStatus status) {
        log.info("Fetching products by status: {}", status);

        return productRepository.findByStatus(status).stream()
                .map(productMapper::toSummaryResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get all active products
     * Cached
     */
    @Cacheable(value = "activeProducts")
    public List<ProductSummaryResponse> getActiveProducts() {
        log.info("Fetching all active products");

        return productRepository.findActiveProducts().stream()
                .map(productMapper::toSummaryResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get currently active products (status=ACTIVE and within effective date range)
     */
    public List<ProductSummaryResponse> getCurrentlyActiveProducts() {
        log.info("Fetching currently active products");

        LocalDate today = LocalDate.now();
        return productRepository.findCurrentlyActiveProducts(today).stream()
                .map(productMapper::toSummaryResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get products by effective date range
     */
    public List<ProductSummaryResponse> getProductsByDateRange(LocalDate startDate, LocalDate endDate) {
        log.info("Fetching products by date range: {} to {}", startDate, endDate);

        return productRepository.findByEffectiveDateRange(startDate, endDate).stream()
                .map(productMapper::toSummaryResponse)
                .collect(Collectors.toList());
    }

    /**
     * Update product status
     * Clears relevant caches
     */
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "products", key = "#productId"),
        @CacheEvict(value = "productsByCode", allEntries = true),
        @CacheEvict(value = "activeProducts", allEntries = true)
    })
    public ProductResponse updateProductStatus(Long productId, ProductStatus newStatus) {
        return updateProductStatus(productId, newStatus, "SYSTEM");
    }

    /**
     * Update product status with audit tracking (Lab L9 Enhancement)
     * Clears relevant caches
     */
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "products", key = "#productId"),
        @CacheEvict(value = "productsByCode", allEntries = true),
        @CacheEvict(value = "activeProducts", allEntries = true)
    })
    public ProductResponse updateProductStatus(Long productId, ProductStatus newStatus, String updatedBy) {
        log.info("Updating product status - ID: {}, New Status: {} by user: {}", productId, newStatus, updatedBy);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));

        product.setStatus(newStatus);
        product.setUpdatedBy(updatedBy);
        Product updatedProduct = productRepository.save(product);

        log.info("Product status updated successfully by user: {}", updatedBy);
        return productMapper.toResponse(updatedProduct);
    }

    /**
     * Delete product (soft delete - set status to CLOSED)
     * Clears all caches
     */
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "products", key = "#productId"),
        @CacheEvict(value = "productsByCode", allEntries = true),
        @CacheEvict(value = "productsByType", allEntries = true),
        @CacheEvict(value = "activeProducts", allEntries = true)
    })
    public void deleteProduct(Long productId) {
        deleteProduct(productId, "SYSTEM");
    }

    /**
     * Delete product with audit tracking (Lab L9 Enhancement)
     * Soft delete - sets status to CLOSED
     * Clears all caches
     */
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "products", key = "#productId"),
        @CacheEvict(value = "productsByCode", allEntries = true),
        @CacheEvict(value = "productsByType", allEntries = true),
        @CacheEvict(value = "activeProducts", allEntries = true)
    })
    public void deleteProduct(Long productId, String deletedBy) {
        log.info("Deleting (soft) product ID: {} by user: {}", productId, deletedBy);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));

        product.setStatus(ProductStatus.CLOSED);
        product.setUpdatedBy(deletedBy);
        productRepository.save(product);

        log.info("Product soft-deleted successfully by user: {}", deletedBy);
    }

    /**
     * Hard delete product (permanent delete)
     * Clears all caches
     */
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "products", key = "#productId"),
        @CacheEvict(value = "productsByCode", allEntries = true),
        @CacheEvict(value = "productsByType", allEntries = true),
        @CacheEvict(value = "activeProducts", allEntries = true)
    })
    public void hardDeleteProduct(Long productId) {
        hardDeleteProduct(productId, "SYSTEM");
    }

    /**
     * Hard delete product with audit tracking (Lab L9 Enhancement)
     * Permanently removes from database
     * Clears all caches
     */
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "products", key = "#productId"),
        @CacheEvict(value = "productsByCode", allEntries = true),
        @CacheEvict(value = "productsByType", allEntries = true),
        @CacheEvict(value = "activeProducts", allEntries = true)
    })
    public void hardDeleteProduct(Long productId, String deletedBy) {
        log.warn("Hard deleting product ID: {} by user: {}", productId, deletedBy);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));

        productRepository.delete(product);

        log.warn("Product permanently deleted by user: {}", deletedBy);
    }

    // ==================== Helper Methods ====================

    private ProductListResponse buildProductListResponse(Page<Product> productPage) {
        List<ProductSummaryResponse> products = productPage.getContent().stream()
                .map(productMapper::toSummaryResponse)
                .collect(Collectors.toList());

        return ProductListResponse.builder()
                .products(products)
                .currentPage(productPage.getNumber())
                .totalPages(productPage.getTotalPages())
                .totalElements(productPage.getTotalElements())
                .pageSize(productPage.getSize())
                .hasNext(productPage.hasNext())
                .hasPrevious(productPage.hasPrevious())
                .build();
    }

    private void validateProductBusinessRules(CreateProductRequest request) {
        // Validate term months
        if (request.getMinTermMonths() != null && request.getMaxTermMonths() != null) {
            if (request.getMinTermMonths().compareTo(request.getMaxTermMonths()) > 0) {
                throw new InvalidProductException("Minimum term months cannot be greater than maximum term months");
            }
        }

        // Validate amounts
        if (request.getMinAmount() != null && request.getMaxAmount() != null) {
            if (request.getMinAmount().compareTo(request.getMaxAmount()) > 0) {
                throw new InvalidProductException("Minimum amount cannot be greater than maximum amount");
            }
        }

        // Validate date range
        if (request.getEffectiveDate() != null && request.getEndDate() != null) {
            if (request.getEffectiveDate().isAfter(request.getEndDate())) {
                throw new InvalidProductException("Effective date cannot be after end date");
            }
        }

        // Validate interest rate matrix
        if (request.getInterestRateMatrix() != null) {
            for (InterestRateMatrixRequest rate : request.getInterestRateMatrix()) {
                if (rate.getEffectiveDate() != null && rate.getEndDate() != null) {
                    if (rate.getEffectiveDate().isAfter(rate.getEndDate())) {
                        throw new InvalidProductException("Interest rate effective date cannot be after end date");
                    }
                }
            }
        }
    }

    private void validateProductUpdateRules(Product product, UpdateProductRequest request) {
        // Validate term months
        Integer minTerm = request.getMinTermMonths() != null ? request.getMinTermMonths() : product.getMinTermMonths().intValue();
        Integer maxTerm = request.getMaxTermMonths() != null ? request.getMaxTermMonths() : product.getMaxTermMonths().intValue();
        
        if (minTerm != null && maxTerm != null && minTerm > maxTerm) {
            throw new InvalidProductException("Minimum term months cannot be greater than maximum term months");
        }

        // Validate date range
        if (request.getEndDate() != null && request.getEndDate().isBefore(product.getEffectiveDate())) {
            throw new InvalidProductException("End date cannot be before effective date");
        }
    }
}
