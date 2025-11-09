package com.app.product.mapper;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.app.product.dto.CreateProductRequest;
import com.app.product.dto.InterestRateMatrixRequest;
import com.app.product.dto.InterestRateMatrixResponse;
import com.app.product.dto.ProductChargeRequest;
import com.app.product.dto.ProductChargeResponse;
import com.app.product.dto.ProductResponse;
import com.app.product.dto.ProductRoleRequest;
import com.app.product.dto.ProductRoleResponse;
import com.app.product.dto.ProductSummaryResponse;
import com.app.product.dto.UpdateProductRequest;
import com.app.product.entity.InterestRateMatrix;
import com.app.product.entity.Product;
import com.app.product.entity.ProductCharge;
import com.app.product.entity.ProductRole;
import com.app.product.enums.ProductStatus;

/**
 * Mapper utility for converting between entities and DTOs
 */
@Component
public class ProductMapper {

    // ============ Product Mappings ============
    
    public Product toEntity(CreateProductRequest request) {
        Product product = Product.builder()
                .productName(request.getProductName())
                .productCode(request.getProductCode())
                .productType(request.getProductType())
                .description(request.getDescription())
                .effectiveDate(request.getEffectiveDate())
                .bankBranchCode(request.getBankBranchCode())
                .currencyCode(request.getCurrencyCode())
                .status(request.getStatus() != null ? request.getStatus() : ProductStatus.DRAFT)
                .minTermMonths(request.getMinTermMonths())
                .maxTermMonths(request.getMaxTermMonths())
                .minAmount(request.getMinAmount())
                .maxAmount(request.getMaxAmount())
                .minBalanceRequired(request.getMinBalanceRequired())
                .baseInterestRate(request.getBaseInterestRate())
                .interestCalculationMethod(request.getInterestCalculationMethod())
                .interestPayoutFrequency(request.getInterestPayoutFrequency())
                .prematureWithdrawalAllowed(request.getPrematureWithdrawalAllowed())
                .partialWithdrawalAllowed(request.getPartialWithdrawalAllowed())
                .autoRenewalAllowed(request.getAutoRenewalAllowed())
                .tdsRate(request.getTdsRate())
                .tdsApplicable(request.getTdsApplicable())
                .build();

        // Add roles
        if (request.getAllowedRoles() != null) {
            request.getAllowedRoles().forEach(roleReq -> 
                product.addRole(toRoleEntity(roleReq, product))
            );
        }

        // Add charges
        if (request.getCharges() != null) {
            request.getCharges().forEach(chargeReq -> 
                product.addCharge(toChargeEntity(chargeReq, product))
            );
        }

        // Add interest rate matrix
        if (request.getInterestRateMatrix() != null) {
            request.getInterestRateMatrix().forEach(rateReq -> 
                product.addInterestRateSlab(toInterestRateEntity(rateReq, product))
            );
        }

        return product;
    }

    public void updateEntity(Product product, UpdateProductRequest request) {
        if (request.getProductName() != null) {
            product.setProductName(request.getProductName());
        }
        if (request.getDescription() != null) {
            product.setDescription(request.getDescription());
        }
        if (request.getStatus() != null) {
            product.setStatus(request.getStatus());
        }
        if (request.getMinTermMonths() != null) {
            product.setMinTermMonths(BigDecimal.valueOf(request.getMinTermMonths()));
        }
        if (request.getMaxTermMonths() != null) {
            product.setMaxTermMonths(BigDecimal.valueOf(request.getMaxTermMonths()));
        }
        if (request.getMinAmount() != null) {
            product.setMinAmount(request.getMinAmount());
        }
        if (request.getMaxAmount() != null) {
            product.setMaxAmount(request.getMaxAmount());
        }
        if (request.getMinBalanceRequired() != null) {
            product.setMinBalanceRequired(request.getMinBalanceRequired());
        }
        if (request.getBaseInterestRate() != null) {
            product.setBaseInterestRate(request.getBaseInterestRate());
        }
        if (request.getInterestCalculationMethod() != null) {
            product.setInterestCalculationMethod(request.getInterestCalculationMethod());
        }
        if (request.getInterestPayoutFrequency() != null) {
            product.setInterestPayoutFrequency(request.getInterestPayoutFrequency());
        }
        if (request.getPrematureWithdrawalAllowed() != null) {
            product.setPrematureWithdrawalAllowed(request.getPrematureWithdrawalAllowed());
        }
        if (request.getPartialWithdrawalAllowed() != null) {
            product.setPartialWithdrawalAllowed(request.getPartialWithdrawalAllowed());
        }
        if (request.getAutoRenewalAllowed() != null) {
            product.setAutoRenewalAllowed(request.getAutoRenewalAllowed());
        }
        if (request.getTdsRate() != null) {
            product.setTdsRate(request.getTdsRate());
        }
        if (request.getTdsApplicable() != null) {
            product.setTdsApplicable(request.getTdsApplicable());
        }
        if (request.getUpdatedBy() != null) {
            product.setUpdatedBy(request.getUpdatedBy());
        }
    }

    public ProductResponse toResponse(Product product) {
        return ProductResponse.builder()
                .productId(product.getId())
                .productName(product.getProductName())
                .productCode(product.getProductCode())
                .productType(product.getProductType())
                .description(product.getDescription())
                .effectiveDate(product.getEffectiveDate())
                .bankBranchCode(product.getBankBranchCode())
                .currencyCode(product.getCurrencyCode())
                .status(product.getStatus())
                .minTermMonths(product.getMinTermMonths() != null ? product.getMinTermMonths().intValue() : null)
                .maxTermMonths(product.getMaxTermMonths() != null ? product.getMaxTermMonths().intValue() : null)
                .minAmount(product.getMinAmount())
                .maxAmount(product.getMaxAmount())
                .minBalanceRequired(product.getMinBalanceRequired())
                .baseInterestRate(product.getBaseInterestRate())
                .interestCalculationMethod(product.getInterestCalculationMethod())
                .interestPayoutFrequency(product.getInterestPayoutFrequency())
                .prematureWithdrawalAllowed(product.getPrematureWithdrawalAllowed())
                .partialWithdrawalAllowed(product.getPartialWithdrawalAllowed())
                .autoRenewalAllowed(product.getAutoRenewalAllowed())
                .tdsRate(product.getTdsRate())
                .tdsApplicable(product.getTdsApplicable())
                .allowedRoles(toRoleResponseList(product.getAllowedRoles()))
                .charges(toChargeResponseList(product.getCharges()))
                .interestRateMatrix(toInterestRateResponseList(product.getInterestRateMatrix()))
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .createdBy(product.getCreatedBy())
                .updatedBy(product.getUpdatedBy())
                .currentlyActive(product.isCurrentlyActive())
                .build();
    }

    public ProductSummaryResponse toSummaryResponse(Product product) {
        return ProductSummaryResponse.builder()
                .productId(product.getId())
                .productName(product.getProductName())
                .productCode(product.getProductCode())
                .productType(product.getProductType())
                .description(product.getDescription())
                .status(product.getStatus())
                .effectiveDate(product.getEffectiveDate())
                .baseInterestRate(product.getBaseInterestRate())
                .minAmount(product.getMinAmount())
                .maxAmount(product.getMaxAmount())
                .minTermMonths(product.getMinTermMonths() != null ? product.getMinTermMonths().intValue() : null)
                .maxTermMonths(product.getMaxTermMonths() != null ? product.getMaxTermMonths().intValue() : null)
                .currentlyActive(product.isCurrentlyActive())
                .build();
    }

    // ============ ProductRole Mappings ============
    
    public ProductRole toRoleEntity(ProductRoleRequest request, Product product) {
        return ProductRole.builder()
                .product(product)
                .roleType(request.getRoleType())
                .description(request.getDescription())
                .build();
    }

    public ProductRoleResponse toRoleResponse(ProductRole role) {
        return ProductRoleResponse.builder()
                .id(role.getId())
                .roleType(role.getRoleType())
                .description(role.getDescription())
                .build();
    }

    public List<ProductRoleResponse> toRoleResponseList(List<ProductRole> roles) {
        if (roles == null) return new ArrayList<>();
        return roles.stream()
                .map(this::toRoleResponse)
                .collect(Collectors.toList());
    }

    // ============ ProductCharge Mappings ============
    
    public ProductCharge toChargeEntity(ProductChargeRequest request, Product product) {
        return ProductCharge.builder()
                .product(product)
                .chargeName(request.getChargeName())
                .chargeType(request.getChargeType())
                .description(request.getDescription())
                .fixedAmount(request.getFixedAmount())
                .percentageRate(request.getPercentageRate())
                .frequency(request.getFrequency())
                .applicableTransactionTypes(request.getApplicableTransactionTypes())
                .active(true)
                .build();
    }

    public ProductChargeResponse toChargeResponse(ProductCharge charge) {
        return ProductChargeResponse.builder()
                .id(charge.getId())
                .chargeName(charge.getChargeName())
                .chargeType(charge.getChargeType())
                .description(charge.getDescription())
                .fixedAmount(charge.getFixedAmount())
                .percentageRate(charge.getPercentageRate())
                .frequency(charge.getFrequency())
                .applicableTransactionTypes(charge.getApplicableTransactionTypes())
                .active(charge.getActive())
                .build();
    }

    public List<ProductChargeResponse> toChargeResponseList(List<ProductCharge> charges) {
        if (charges == null) return new ArrayList<>();
        return charges.stream()
                .map(this::toChargeResponse)
                .collect(Collectors.toList());
    }

    // ============ InterestRateMatrix Mappings ============
    
    public InterestRateMatrix toInterestRateEntity(InterestRateMatrixRequest request, Product product) {
        return InterestRateMatrix.builder()
                .product(product)
                .customerClassification(request.getCustomerClassification())
                .interestRate(request.getInterestRate())
                .additionalRate(request.getAdditionalRate())
                .effectiveDate(request.getEffectiveDate())
                .build();
    }

    public InterestRateMatrixResponse toInterestRateResponse(InterestRateMatrix rate) {
        return InterestRateMatrixResponse.builder()
                .id(rate.getId())
                .customerClassification(rate.getCustomerClassification())
                .interestRate(rate.getInterestRate())
                .additionalRate(rate.getAdditionalRate())
                .effectiveDate(rate.getEffectiveDate())
                .totalRate(rate.getTotalRate())
                .build();
    }

    public List<InterestRateMatrixResponse> toInterestRateResponseList(List<InterestRateMatrix> rates) {
        if (rates == null) return new ArrayList<>();
        return rates.stream()
                .map(this::toInterestRateResponse)
                .collect(Collectors.toList());
    }
}
