package com.textile.erp.product.service;

import com.textile.erp.product.dto.CreateProductRequest;
import com.textile.erp.product.dto.ProductResponse;
import com.textile.erp.product.dto.UpdateProductRequest;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProductService {

    ProductResponse createProduct(CreateProductRequest request);

    ProductResponse getProductById(UUID id);

    Page<ProductResponse> listProducts(String query, Boolean active, Pageable pageable);

    ProductResponse updateProduct(UUID id, UpdateProductRequest request);

    ProductResponse toggleProductStatus(UUID id, Boolean active);

    void deleteProduct(UUID id);
}
