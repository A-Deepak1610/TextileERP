package com.textile.erp.invoice.infrastructure.storage;

import org.springframework.core.io.Resource;

public interface DocumentStorage {
    StoredDocument store(String storageKey, byte[] content, String contentType);
    Resource load(String storageKey);
    boolean exists(String storageKey);
}
