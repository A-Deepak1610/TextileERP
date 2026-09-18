package com.textile.erp.invoice.domain;

public class InvoicePdfGenerationException extends RuntimeException {
    public InvoicePdfGenerationException(String message) {
        super(message);
    }

    public InvoicePdfGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
