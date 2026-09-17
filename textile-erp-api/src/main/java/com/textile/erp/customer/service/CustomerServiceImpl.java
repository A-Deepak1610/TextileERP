package com.textile.erp.customer.service;

import com.textile.erp.auth.security.CurrentUser;
import com.textile.erp.auth.security.SecurityUtils;
import com.textile.erp.customer.dto.BillingAddressDto;
import com.textile.erp.customer.dto.CreateCustomerRequest;
import com.textile.erp.customer.dto.CreateShippingPartyRequest;
import com.textile.erp.customer.dto.CustomerResponse;
import com.textile.erp.customer.dto.CustomerSummaryResponse;
import com.textile.erp.customer.dto.ShippingPartyResponse;
import com.textile.erp.customer.dto.UpdateCustomerRequest;
import com.textile.erp.customer.dto.UpdateCustomerStatusRequest;
import com.textile.erp.customer.entity.BillingAddress;
import com.textile.erp.customer.entity.Customer;
import com.textile.erp.customer.entity.CustomerStatus;
import com.textile.erp.customer.entity.ShippingParty;
import com.textile.erp.customer.repository.CustomerRepository;
import com.textile.erp.customer.repository.ShippingPartyRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomerServiceImpl implements CustomerService {

    private static final Pattern GSTIN_PATTERN = Pattern.compile("^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$");
    private static final Pattern PAN_PATTERN = Pattern.compile("^[A-Z]{5}[0-9]{4}[A-Z]{1}$");
    private static final Pattern PINCODE_PATTERN = Pattern.compile("^[1-9][0-9]{5}$");

    private final CustomerRepository customerRepository;
    private final ShippingPartyRepository shippingPartyRepository;
    private final ShippingPartyServiceImpl shippingPartyService;

    @Override
    @Transactional
    public CustomerResponse createCustomer(CreateCustomerRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("CreateCustomerRequest cannot be null");
        }
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Customer name cannot be blank");
        }

        UUID tenantId = resolveCurrentTenantId();

        // Customer Code generation or uniqueness validation
        String customerCode = request.getCustomerCode() != null && !request.getCustomerCode().trim().isEmpty()
                ? request.getCustomerCode().trim().toUpperCase()
                : generateCustomerCode(tenantId);

        if (customerRepository.existsByTenantIdAndCustomerCode(tenantId, customerCode)) {
            throw new IllegalStateException("Customer code '" + customerCode + "' already exists in this tenant");
        }

        validateFormats(request.getGstin(), request.getPan(), request.getBillingAddress());

        if (request.getCreditLimit() != null && request.getCreditLimit().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Credit limit cannot be negative");
        }

        BillingAddress billingAddress = mapBillingAddress(request.getBillingAddress());

        Customer customer = Customer.builder()
                .tenantId(tenantId)
                .customerCode(customerCode)
                .name(request.getName().trim())
                .gstin(normalizeUpper(request.getGstin()))
                .pan(normalizeUpper(request.getPan()))
                .phone(trimOrNull(request.getPhone()))
                .email(trimOrNull(request.getEmail()))
                .billingAddress(billingAddress)
                .paymentTermsDays(request.getPaymentTermsDays() != null ? request.getPaymentTermsDays() : 30)
                .creditLimit(request.getCreditLimit() != null ? request.getCreditLimit() : BigDecimal.ZERO)
                .status(CustomerStatus.ACTIVE)
                .build();

        Customer savedCustomer = customerRepository.saveAndFlush(customer);

        List<ShippingParty> savedParties = new ArrayList<>();

        // Handle same_as_billing or explicit initial shipping parties
        if (Boolean.TRUE.equals(request.getSameAsBilling())) {
            CreateShippingPartyRequest sameBillingReq = CreateShippingPartyRequest.builder()
                    .sameAsBilling(true)
                    .build();
            ShippingParty party = shippingPartyService.buildShippingPartyFromRequest(tenantId, savedCustomer, sameBillingReq);
            savedParties.add(shippingPartyRepository.saveAndFlush(party));
        }

        if (request.getShippingParties() != null) {
            for (CreateShippingPartyRequest spReq : request.getShippingParties()) {
                ShippingParty party = shippingPartyService.buildShippingPartyFromRequest(tenantId, savedCustomer, spReq);
                savedParties.add(shippingPartyRepository.saveAndFlush(party));
            }
        }

        return mapToResponse(savedCustomer, savedParties);
    }

    @Override
    public CustomerResponse getCustomerById(UUID id) {
        UUID tenantId = resolveCurrentTenantId();
        Customer customer = findCustomerSecured(tenantId, id);
        List<ShippingParty> parties = shippingPartyRepository.findByTenantIdAndCustomerId(tenantId, id);
        return mapToResponse(customer, parties);
    }

    @Override
    public Page<CustomerSummaryResponse> listCustomers(String search, CustomerStatus status, Pageable pageable) {
        UUID tenantId = resolveCurrentTenantId();
        String term = (search != null && !search.isBlank()) ? search.trim() : null;

        return customerRepository.searchCustomers(tenantId, term, status, pageable)
                .map(cust -> {
                    List<ShippingParty> parties = shippingPartyRepository.findByTenantIdAndCustomerId(tenantId, cust.getId());
                    return CustomerSummaryResponse.builder()
                            .id(cust.getId())
                            .customerCode(cust.getCustomerCode())
                            .name(cust.getName())
                            .gstin(cust.getGstin())
                            .phone(cust.getPhone())
                            .email(cust.getEmail())
                            .city(cust.getBillingAddress() != null ? cust.getBillingAddress().getCity() : null)
                            .state(cust.getBillingAddress() != null ? cust.getBillingAddress().getState() : null)
                            .creditLimit(cust.getCreditLimit())
                            .status(cust.getStatus())
                            .shippingPartyCount(parties.size())
                            .createdAt(cust.getCreatedAt())
                            .build();
                });
    }

    @Override
    @Transactional
    public CustomerResponse updateCustomer(UUID id, UpdateCustomerRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("UpdateCustomerRequest cannot be null");
        }
        if (request.getName() != null && request.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Customer name cannot be blank");
        }

        UUID tenantId = resolveCurrentTenantId();
        Customer customer = findCustomerSecured(tenantId, id);

        if (request.getCustomerCode() != null && !request.getCustomerCode().trim().isEmpty()) {
            String newCode = request.getCustomerCode().trim().toUpperCase();
            if (customerRepository.existsByTenantIdAndCustomerCodeAndIdNot(tenantId, newCode, id)) {
                throw new IllegalStateException("Customer code '" + newCode + "' already exists in this tenant");
            }
            customer.setCustomerCode(newCode);
        }

        validateFormats(request.getGstin(), request.getPan(), request.getBillingAddress());

        if (request.getName() != null) {
            customer.setName(request.getName().trim());
        }
        if (request.getGstin() != null) {
            customer.setGstin(normalizeUpper(request.getGstin()));
        }
        if (request.getPan() != null) {
            customer.setPan(normalizeUpper(request.getPan()));
        }
        if (request.getPhone() != null) {
            customer.setPhone(trimOrNull(request.getPhone()));
        }
        if (request.getEmail() != null) {
            customer.setEmail(trimOrNull(request.getEmail()));
        }
        if (request.getBillingAddress() != null) {
            customer.setBillingAddress(mapBillingAddress(request.getBillingAddress()));
        }
        if (request.getPaymentTermsDays() != null) {
            customer.setPaymentTermsDays(request.getPaymentTermsDays());
        }
        if (request.getCreditLimit() != null) {
            if (request.getCreditLimit().compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Credit limit cannot be negative");
            }
            customer.setCreditLimit(request.getCreditLimit());
        }

        Customer saved = customerRepository.saveAndFlush(customer);
        List<ShippingParty> parties = shippingPartyRepository.findByTenantIdAndCustomerId(tenantId, id);
        return mapToResponse(saved, parties);
    }

    @Override
    @Transactional
    public CustomerResponse updateCustomerStatus(UUID id, UpdateCustomerStatusRequest request) {
        if (request == null || request.getStatus() == null) {
            throw new IllegalArgumentException("Customer status must be specified");
        }
        UUID tenantId = resolveCurrentTenantId();
        Customer customer = findCustomerSecured(tenantId, id);

        customer.setStatus(request.getStatus());
        Customer saved = customerRepository.saveAndFlush(customer);
        List<ShippingParty> parties = shippingPartyRepository.findByTenantIdAndCustomerId(tenantId, id);
        return mapToResponse(saved, parties);
    }

    @Override
    @Transactional
    public void deleteCustomer(UUID id) {
        UUID tenantId = resolveCurrentTenantId();
        Customer customer = findCustomerSecured(tenantId, id);
        shippingPartyRepository.deleteByTenantIdAndCustomerId(tenantId, id);
        customerRepository.delete(customer);
    }

    private Customer findCustomerSecured(UUID tenantId, UUID id) {
        return customerRepository.findByTenantIdAndId(tenantId, id)
                .orElseThrow(() -> new NoSuchElementException("Customer not found with ID: " + id));
    }

    private UUID resolveCurrentTenantId() {
        CurrentUser currentUser = SecurityUtils.getCurrentUser()
                .orElseThrow(() -> new AccessDeniedException("Authentication required"));
        if (currentUser.getTenantId() != null) {
            return currentUser.getTenantId();
        }
        throw new IllegalArgumentException("Customer management requires an active tenant context");
    }

    private String generateCustomerCode(UUID tenantId) {
        long count = customerRepository.count();
        return String.format("CUST-%04d", count + 1);
    }

    private void validateFormats(String gstin, String pan, BillingAddressDto billingAddress) {
        if (gstin != null && !gstin.trim().isEmpty() && !GSTIN_PATTERN.matcher(gstin.trim().toUpperCase()).matches()) {
            throw new IllegalArgumentException("Invalid GSTIN format: '" + gstin + "'");
        }
        if (pan != null && !pan.trim().isEmpty() && !PAN_PATTERN.matcher(pan.trim().toUpperCase()).matches()) {
            throw new IllegalArgumentException("Invalid PAN format: '" + pan + "'");
        }
        if (billingAddress != null && billingAddress.getPincode() != null && !billingAddress.getPincode().trim().isEmpty()) {
            if (!PINCODE_PATTERN.matcher(billingAddress.getPincode().trim()).matches()) {
                throw new IllegalArgumentException("Invalid Pincode format: '" + billingAddress.getPincode() + "'. Must be 6 digits.");
            }
        }
    }

    private BillingAddress mapBillingAddress(BillingAddressDto dto) {
        if (dto == null) {
            return new BillingAddress();
        }
        return BillingAddress.builder()
                .addressLine1(trimOrNull(dto.getAddressLine1()))
                .addressLine2(trimOrNull(dto.getAddressLine2()))
                .city(trimOrNull(dto.getCity()))
                .district(trimOrNull(dto.getDistrict()))
                .state(trimOrNull(dto.getState()))
                .stateCode(trimOrNull(dto.getStateCode()))
                .pincode(trimOrNull(dto.getPincode()))
                .country(dto.getCountry() != null && !dto.getCountry().isBlank() ? dto.getCountry().trim() : "India")
                .build();
    }

    private CustomerResponse mapToResponse(Customer customer, List<ShippingParty> parties) {
        BillingAddress billing = customer.getBillingAddress();
        BillingAddressDto billingDto = null;
        if (billing != null) {
            billingDto = BillingAddressDto.builder()
                    .addressLine1(billing.getAddressLine1())
                    .addressLine2(billing.getAddressLine2())
                    .city(billing.getCity())
                    .district(billing.getDistrict())
                    .state(billing.getState())
                    .stateCode(billing.getStateCode())
                    .pincode(billing.getPincode())
                    .country(billing.getCountry())
                    .build();
        }

        List<ShippingPartyResponse> partyResponses = parties != null
                ? parties.stream().map(p -> ShippingPartyResponse.builder()
                        .id(p.getId())
                        .tenantId(p.getTenantId())
                        .customerId(p.getCustomerId())
                        .name(p.getName())
                        .gstin(p.getGstin())
                        .pan(p.getPan())
                        .phone(p.getPhone())
                        .email(p.getEmail())
                        .addressLine1(p.getAddressLine1())
                        .addressLine2(p.getAddressLine2())
                        .city(p.getCity())
                        .district(p.getDistrict())
                        .state(p.getState())
                        .stateCode(p.getStateCode())
                        .pincode(p.getPincode())
                        .country(p.getCountry())
                        .sameAsBilling(p.getSameAsBilling())
                        .status(p.getStatus())
                        .createdAt(p.getCreatedAt())
                        .updatedAt(p.getUpdatedAt())
                        .build()).toList()
                : List.of();

        return CustomerResponse.builder()
                .id(customer.getId())
                .tenantId(customer.getTenantId())
                .customerCode(customer.getCustomerCode())
                .name(customer.getName())
                .gstin(customer.getGstin())
                .pan(customer.getPan())
                .phone(customer.getPhone())
                .email(customer.getEmail())
                .billingAddress(billingDto)
                .paymentTermsDays(customer.getPaymentTermsDays())
                .creditLimit(customer.getCreditLimit())
                .status(customer.getStatus())
                .shippingParties(partyResponses)
                .createdAt(customer.getCreatedAt())
                .updatedAt(customer.getUpdatedAt())
                .build();
    }

    private String normalizeUpper(String val) {
        return val != null && !val.trim().isEmpty() ? val.trim().toUpperCase() : null;
    }

    private String trimOrNull(String val) {
        return val != null && !val.trim().isEmpty() ? val.trim() : null;
    }
}
