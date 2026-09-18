package com.textile.erp.invoice.infrastructure.storage;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoredDocument {
    private String storageKey;
    private String fileName;
    private String contentType;
    private long fileSize;
    private String checksumSha256;
}
