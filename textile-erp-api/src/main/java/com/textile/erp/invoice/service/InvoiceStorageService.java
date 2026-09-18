package com.textile.erp.invoice.service;

import com.textile.erp.invoice.entity.Invoice;
import java.util.UUID;

public interface InvoiceStorageService {

    String generateAndStoreInvoicePdf(UUID tenantId, Invoice invoice);
}
