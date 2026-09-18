package com.textile.erp.customer.service;

import com.textile.erp.auth.security.CurrentUser;
import com.textile.erp.auth.security.SecurityUtils;
import com.textile.erp.customer.dto.CreateShippingPartyRequest;
import com.textile.erp.customer.dto.ShippingPartyResponse;
import com.textile.erp.customer.dto.UpdateShippingPartyRequest;
import com.textile.erp.customer.entity.BillingAddress;
import com.textile.erp.customer.entity.Customer;
import com.textile.erp.customer.entity.CustomerStatus;
import com.textile.erp.customer.entity.ShippingParty;
import com.textile.erp.customer.repository.CustomerRepository;
import com.textile.erp.customer.repository.ShippingPartyRepository;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ShippingPartyServiceImpl implements ShippingPartyService {

    private static final Pattern GSTIN_PATTERN = Pattern.compile("^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$");
    private static final Pattern PAN_PATTERN = Pattern.compile("^[A-Z]{5}[0-9]{4}[A-Z]{1}$");
    private static final Pattern PINCODE_PATTERN = Pattern.compile("^[1-9][0-9]{5}$");

    private final ShippingPartyRepository shippingPartyRepository;
    private final CustomerRepository customerRepository;

    @Override
    public List<ShippingPartyResponse> getShippingParties(UUID customerId) {
        UUID tenantId = resolveCurrentTenantId();
        validateCustomerExistsInTenant(tenantId, customerId);

        return shippingPartyRepository.findByTenantIdAndCustomerId(tenantId, customerId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public ShippingPartyResponse getShippingPartyById(UUID customerId, UUID shippingPartyId) {
        UUID tenantId = resolveCurrentTenantId();
        validateCustomerExistsInTenant(tenantId, customerId);

        ShippingParty party = shippingPartyRepository.findByTenantIdAndCustomerIdAndId(tenantId, customerId, shippingPartyId)
                .orElseThrow(() -> new NoSuchElementException("Shipping party not found with ID: " + shippingPartyId));

        return mapToResponse(party);
    }

    @Override
    @Transactional
    public ShippingPartyResponse createShippingParty(UUID customerId, CreateShippingPartyRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("CreateShippingPartyRequest cannot be null");
        }

        UUID tenantId = resolveCurrentTenantId();
        Customer customer = customerRepository.findByTenantIdAndId(tenantId, customerId)
                .orElseThrow(() -> new NoSuchElementException("Customer not found with ID: " + customerId));

        ShippingParty party = buildShippingPartyFromRequest(tenantId, customer, request);
        ShippingParty saved = shippingPartyRepository.saveAndFlush(party);
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public ShippingPartyResponse updateShippingParty(UUID customerId, UUID shippingPartyId, UpdateShippingPartyRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("UpdateShippingPartyRequest cannot be null");
        }

        UUID tenantId = resolveCurrentTenantId();
        Customer customer = customerRepository.findByTenantIdAndId(tenantId, customerId)
                .orElseThrow(() -> new NoSuchElementException("Customer not found with ID: " + customerId));

        ShippingParty party = shippingPartyRepository.findByTenantIdAndCustomerIdAndId(tenantId, customerId, shippingPartyId)
                .orElseThrow(() -> new NoSuchElementException("Shipping party not found with ID: " + shippingPartyId));

        boolean sameAsBilling = Boolean.TRUE.equals(request.getSameAsBilling());
        party.setSameAsBilling(sameAsBilling);

        if (sameAsBilling) {
            BillingAddress billing = customer.getBillingAddress();
            party.setName(customer.getName());
            party.setGstin(customer.getGstin());
            party.setPan(customer.getPan());
            party.setPhone(customer.getPhone());
            party.setEmail(customer.getEmail());

            if (billing != null) {
                party.setAddressLine1(billing.getAddressLine1());
                party.setAddressLine2(billing.getAddressLine2());
                party.setCity(billing.getCity());
                party.setDistrict(billing.getDistrict());
                party.setState(billing.getState());
                party.setStateCode(billing.getStateCode());
                party.setPincode(billing.getPincode());
                party.setCountry(billing.getCountry());
            }
        } else {
            if (request.getName() == null || request.getName().trim().isEmpty()) {
                throw new IllegalArgumentException("Shipping party name cannot be blank");
            }
            validateFormats(request.getGstin(), request.getPan(), request.getPincode());

            party.setName(request.getName().trim());
            party.setGstin(normalizeUpper(request.getGstin()));
            party.setPan(normalizeUpper(request.getPan()));
            party.setPhone(trimOrNull(request.getPhone()));
            party.setEmail(trimOrNull(request.getEmail()));
            party.setAddressLine1(trimOrNull(request.getAddressLine1()));
            party.setAddressLine2(trimOrNull(request.getAddressLine2()));
            party.setCity(trimOrNull(request.getCity()));
            party.setDistrict(trimOrNull(request.getDistrict()));
            party.setState(trimOrNull(request.getState()));
            party.setStateCode(trimOrNull(request.getStateCode()));
            party.setPincode(trimOrNull(request.getPincode()));
            party.setCountry(request.getCountry() != null && !request.getCountry().isBlank() ? request.getCountry().trim() : "India");
        }

        if (request.getStatus() != null) {
            party.setStatus(request.getStatus());
        }

        ShippingParty updated = shippingPartyRepository.saveAndFlush(party);
        return mapToResponse(updated);
    }

    @Override
    @Transactional
    public void deleteShippingParty(UUID customerId, UUID shippingPartyId) {
        UUID tenantId = resolveCurrentTenantId();
        validateCustomerExistsInTenant(tenantId, customerId);

        ShippingParty party = shippingPartyRepository.findByTenantIdAndCustomerIdAndId(tenantId, customerId, shippingPartyId)
                .orElseThrow(() -> new NoSuchElementException("Shipping party not found with ID: " + shippingPartyId));

        shippingPartyRepository.delete(party);
    }

    public ShippingParty buildShippingPartyFromRequest(UUID tenantId, Customer customer, CreateShippingPartyRequest request) {
        boolean sameAsBilling = Boolean.TRUE.equals(request.getSameAsBilling());

        if (sameAsBilling) {
            BillingAddress billing = customer.getBillingAddress();
            return ShippingParty.builder()
                    .tenantId(tenantId)
                    .customerId(customer.getId())
                    .name(customer.getName())
                    .gstin(customer.getGstin())
                    .pan(customer.getPan())
                    .phone(customer.getPhone())
                    .email(customer.getEmail())
                    .addressLine1(billing != null ? billing.getAddressLine1() : null)
                    .addressLine2(billing != null ? billing.getAddressLine2() : null)
                    .city(billing != null ? billing.getCity() : null)
                    .district(billing != null ? billing.getDistrict() : null)
                    .state(billing != null ? billing.getState() : null)
                    .stateCode(billing != null ? billing.getStateCode() : null)
                    .pincode(billing != null ? billing.getPincode() : null)
                    .country(billing != null ? billing.getCountry() : "India")
                    .sameAsBilling(true)
                    .status(CustomerStatus.ACTIVE)
                    .build();
        }

        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Shipping party name cannot be blank");
        }
        validateFormats(request.getGstin(), request.getPan(), request.getPincode());

        return ShippingParty.builder()
                .tenantId(tenantId)
                .customerId(customer.getId())
                .name(request.getName().trim())
                .gstin(normalizeUpper(request.getGstin()))
                .pan(normalizeUpper(request.getPan()))
                .phone(trimOrNull(request.getPhone()))
                .email(trimOrNull(request.getEmail()))
                .addressLine1(trimOrNull(request.getAddressLine1()))
                .addressLine2(trimOrNull(request.getAddressLine2()))
                .city(trimOrNull(request.getCity()))
                .district(trimOrNull(request.getDistrict()))
                .state(trimOrNull(request.getState()))
                .stateCode(trimOrNull(request.getStateCode()))
                .pincode(trimOrNull(request.getPincode()))
                .country(request.getCountry() != null && !request.getCountry().isBlank() ? request.getCountry().trim() : "India")
                .sameAsBilling(false)
                .status(CustomerStatus.ACTIVE)
                .build();
    }

    private void validateFormats(String gstin, String pan, String pincode) {
        if (gstin != null && !gstin.trim().isEmpty() && !GSTIN_PATTERN.matcher(gstin.trim().toUpperCase()).matches()) {
            throw new IllegalArgumentException("Invalid GSTIN format: '" + gstin + "'");
        }
        if (pan != null && !pan.trim().isEmpty() && !PAN_PATTERN.matcher(pan.trim().toUpperCase()).matches()) {
            throw new IllegalArgumentException("Invalid PAN format: '" + pan + "'");
        }
        if (pincode != null && !pincode.trim().isEmpty() && !PINCODE_PATTERN.matcher(pincode.trim()).matches()) {
            throw new IllegalArgumentException("Invalid Pincode format: '" + pincode + "'. Must be 6 digits.");
        }
    }

    private void validateCustomerExistsInTenant(UUID tenantId, UUID customerId) {
        customerRepository.findByTenantIdAndId(tenantId, customerId)
                .orElseThrow(() -> new NoSuchElementException("Customer not found with ID: " + customerId));
    }

    private UUID resolveCurrentTenantId() {
        CurrentUser currentUser = SecurityUtils.getCurrentUser()
                .orElseThrow(() -> new AccessDeniedException("Authentication required"));
        if (currentUser.getTenantId() != null) {
            return currentUser.getTenantId();
        }
        throw new IllegalArgumentException("Action requires an active tenant context");
    }

    private ShippingPartyResponse mapToResponse(ShippingParty party) {
        return ShippingPartyResponse.builder()
                .id(party.getId())
                .tenantId(party.getTenantId())
                .customerId(party.getCustomerId())
                .name(party.getName())
                .gstin(party.getGstin())
                .pan(party.getPan())
                .phone(party.getPhone())
                .email(party.getEmail())
                .addressLine1(party.getAddressLine1())
                .addressLine2(party.getAddressLine2())
                .city(party.getCity())
                .district(party.getDistrict())
                .state(party.getState())
                .stateCode(party.getStateCode())
                .pincode(party.getPincode())
                .country(party.getCountry())
                .sameAsBilling(party.getSameAsBilling())
                .status(party.getStatus())
                .createdAt(party.getCreatedAt())
                .updatedAt(party.getUpdatedAt())
                .build();
    }

    private String normalizeUpper(String val) {
        return val != null && !val.trim().isEmpty() ? val.trim().toUpperCase() : null;
    }

    private String trimOrNull(String val) {
        return val != null && !val.trim().isEmpty() ? val.trim() : null;
    }
}
