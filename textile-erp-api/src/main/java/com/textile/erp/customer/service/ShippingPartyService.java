package com.textile.erp.customer.service;

import com.textile.erp.customer.dto.CreateShippingPartyRequest;
import com.textile.erp.customer.dto.ShippingPartyResponse;
import com.textile.erp.customer.dto.UpdateShippingPartyRequest;
import java.util.List;
import java.util.UUID;

public interface ShippingPartyService {

    List<ShippingPartyResponse> getShippingParties(UUID customerId);

    ShippingPartyResponse getShippingPartyById(UUID customerId, UUID shippingPartyId);

    ShippingPartyResponse createShippingParty(UUID customerId, CreateShippingPartyRequest request);

    ShippingPartyResponse updateShippingParty(UUID customerId, UUID shippingPartyId, UpdateShippingPartyRequest request);

    void deleteShippingParty(UUID customerId, UUID shippingPartyId);
}
