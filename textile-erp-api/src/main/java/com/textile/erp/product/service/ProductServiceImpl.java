package com.textile.erp.product.service;

import com.textile.erp.auth.model.CurrentUser;
import com.textile.erp.auth.security.SecurityUtils;
import com.textile.erp.product.dto.CreateProductRequest;
import com.textile.erp.product.dto.ProductResponse;
import com.textile.erp.product.dto.UpdateProductRequest;
import com.textile.erp.product.entity.Product;
import com.textile.erp.product.repository.ProductRepository;
import java.math.BigDecimal;
import java.util.NoSuchElementException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;

    @Override
    @Transactional
    public ProductResponse createProduct(CreateProductRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("CreateProductRequest cannot be null");
        }

        UUID tenantId = resolveCurrentTenantId();
        String code = (request.getProductCode() != null && !request.getProductCode().isBlank())
                ? request.getProductCode().trim().toUpperCase()
                : generateProductCode(tenantId);

        if (productRepository.existsByTenantIdAndProductCode(tenantId, code)) {
            throw new IllegalStateException("Product code '" + code + "' already exists in this tenant");
        }

        Product product = Product.builder()
                .tenantId(tenantId)
                .productCode(code)
                .productName(request.getProductName().trim())
                .description(trimOrNull(request.getDescription()))
                .hsnCode(trimOrNull(request.getHsnCode()))
                .fabricType(trimOrNull(request.getFabricType()))
                .color(trimOrNull(request.getColor()))
                .gsm(request.getGsm())
                .unit((request.getUnit() != null && !request.getUnit().isBlank()) ? request.getUnit().trim().toUpperCase() : "METER")
                .defaultRatePerUnit(request.getDefaultRatePerUnit() != null ? request.getDefaultRatePerUnit() : BigDecimal.ZERO)
                .gstRate(request.getGstRate() != null ? request.getGstRate() : new BigDecimal("5.00"))
                .active(request.getActive() != null ? request.getActive() : true)
                .build();

        Product saved = productRepository.saveAndFlush(product);
        log.info("Created product {} (ID: {}) for tenant {}", saved.getProductCode(), saved.getId(), tenantId);
        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getProductById(UUID id) {
        UUID tenantId = resolveCurrentTenantId();
        Product product = findSecured(tenantId, id);
        return mapToResponse(product);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> listProducts(String query, Boolean active, Pageable pageable) {
        UUID tenantId = resolveCurrentTenantId();
        String term = (query != null && !query.isBlank()) ? query.trim() : null;
        return productRepository.searchProducts(tenantId, term, active, pageable)
                .map(this::mapToResponse);
    }

    @Override
    @Transactional
    public ProductResponse updateProduct(UUID id, UpdateProductRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("UpdateProductRequest cannot be null");
        }

        UUID tenantId = resolveCurrentTenantId();
        Product product = findSecured(tenantId, id);

        if (request.getProductCode() != null && !request.getProductCode().isBlank()) {
            String newCode = request.getProductCode().trim().toUpperCase();
            if (productRepository.existsByTenantIdAndProductCodeAndIdNot(tenantId, newCode, id)) {
                throw new IllegalStateException("Product code '" + newCode + "' already exists in this tenant");
            }
            product.setProductCode(newCode);
        }

        if (request.getProductName() != null && !request.getProductName().isBlank()) {
            product.setProductName(request.getProductName().trim());
        }
        if (request.getDescription() != null) {
            product.setDescription(trimOrNull(request.getDescription()));
        }
        if (request.getHsnCode() != null) {
            product.setHsnCode(trimOrNull(request.getHsnCode()));
        }
        if (request.getFabricType() != null) {
            product.setFabricType(trimOrNull(request.getFabricType()));
        }
        if (request.getColor() != null) {
            product.setColor(trimOrNull(request.getColor()));
        }
        if (request.getGsm() != null) {
            product.setGsm(request.getGsm());
        }
        if (request.getUnit() != null && !request.getUnit().isBlank()) {
            product.setUnit(request.getUnit().trim().toUpperCase());
        }
        if (request.getDefaultRatePerUnit() != null) {
            product.setDefaultRatePerUnit(request.getDefaultRatePerUnit());
        }
        if (request.getGstRate() != null) {
            product.setGstRate(request.getGstRate());
        }
        if (request.getActive() != null) {
            product.setActive(request.getActive());
        }

        Product updated = productRepository.saveAndFlush(product);
        log.info("Updated product {} (ID: {}) for tenant {}", updated.getProductCode(), updated.getId(), tenantId);
        return mapToResponse(updated);
    }

    @Override
    @Transactional
    public ProductResponse toggleProductStatus(UUID id, Boolean active) {
        UUID tenantId = resolveCurrentTenantId();
        Product product = findSecured(tenantId, id);
        product.setActive(Boolean.TRUE.equals(active));
        Product updated = productRepository.saveAndFlush(product);
        return mapToResponse(updated);
    }

    @Override
    @Transactional
    public void deleteProduct(UUID id) {
        UUID tenantId = resolveCurrentTenantId();
        Product product = findSecured(tenantId, id);
        productRepository.delete(product);
        log.info("Deleted product {} (ID: {}) for tenant {}", product.getProductCode(), id, tenantId);
    }

    private Product findSecured(UUID tenantId, UUID id) {
        return productRepository.findByTenantIdAndId(tenantId, id)
                .orElseThrow(() -> new NoSuchElementException("Product not found with ID: " + id));
    }

    private UUID resolveCurrentTenantId() {
        CurrentUser currentUser = SecurityUtils.getCurrentUser()
                .orElseThrow(() -> new AccessDeniedException("Authentication required"));
        if (currentUser.getTenantId() != null) {
            return currentUser.getTenantId();
        }
        throw new IllegalArgumentException("Action requires an active tenant context");
    }

    private String generateProductCode(UUID tenantId) {
        long count = productRepository.count();
        return String.format("PRD-%04d", count + 1);
    }

    private String trimOrNull(String str) {
        if (str == null || str.trim().isEmpty()) {
            return null;
        }
        return str.trim();
    }

    private ProductResponse mapToResponse(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .tenantId(product.getTenantId())
                .productCode(product.getProductCode())
                .productName(product.getProductName())
                .description(product.getDescription())
                .hsnCode(product.getHsnCode())
                .fabricType(product.getFabricType())
                .color(product.getColor())
                .gsm(product.getGsm())
                .unit(product.getUnit())
                .defaultRatePerUnit(product.getDefaultRatePerUnit())
                .gstRate(product.getGstRate())
                .active(product.getActive())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }
}
