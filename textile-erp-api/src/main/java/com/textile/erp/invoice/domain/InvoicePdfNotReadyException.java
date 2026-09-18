package com.textile.erp.invoice.domain;

public class InvoicePdfNotReadyException extends RuntimeException {
    public InvoicePdfNotReadyException(String message) {
        super(message);
    }
}
