package com.textile.erp.invoice.domain;

public class InvoiceNotIssuedException extends RuntimeException {
    public InvoiceNotIssuedException(String message) {
        super(message);
    }
}
