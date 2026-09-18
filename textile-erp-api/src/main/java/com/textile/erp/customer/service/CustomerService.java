package com.textile.erp.customer.service;

import com.textile.erp.customer.dto.CreateCustomerRequest;
import com.textile.erp.customer.dto.CustomerResponse;
import com.textile.erp.customer.dto.CustomerSummaryResponse;
import com.textile.erp.customer.dto.UpdateCustomerRequest;
import com.textile.erp.customer.dto.UpdateCustomerStatusRequest;
import com.textile.erp.customer.entity.CustomerStatus;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CustomerService {

    CustomerResponse createCustomer(CreateCustomerRequest request);

    CustomerResponse getCustomerById(UUID id);

    Page<CustomerSummaryResponse> listCustomers(String search, CustomerStatus status, Pageable pageable);

    CustomerResponse updateCustomer(UUID id, UpdateCustomerRequest request);

    CustomerResponse updateCustomerStatus(UUID id, UpdateCustomerStatusRequest request);

    void deleteCustomer(UUID id);
}
