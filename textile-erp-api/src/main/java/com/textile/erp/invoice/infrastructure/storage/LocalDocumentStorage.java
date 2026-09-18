package com.textile.erp.invoice.infrastructure.storage;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class LocalDocumentStorage implements DocumentStorage {

    private final Path rootDir;

    public LocalDocumentStorage(@Value("${storage.local.base-dir:./uploads}") String baseDir) {
        this.rootDir = Paths.get(baseDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.rootDir);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to create root storage directory: " + rootDir, e);
        }
    }

    @Override
    public StoredDocument store(String storageKey, byte[] content, String contentType) {
        if (storageKey == null || storageKey.isBlank()) {
            throw new IllegalArgumentException("Storage key cannot be blank");
        }
        if (content == null) {
            throw new IllegalArgumentException("Content cannot be null");
        }

        Path target = resolveSafePath(storageKey);

        try {
            if (target.getParent() != null) {
                Files.createDirectories(target.getParent());
            }
            Files.write(target, content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);

            String checksum = computeSha256(content);
            String fileName = target.getFileName().toString();

            log.info("Stored document safely at {} (size: {} bytes, SHA-256: {})", target, content.length, checksum);

            return StoredDocument.builder()
                    .storageKey(storageKey)
                    .fileName(fileName)
                    .contentType(contentType != null ? contentType : "application/octet-stream")
                    .fileSize((long) content.length)
                    .checksumSha256(checksum)
                    .build();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write document to storage: " + target, e);
        }
    }

    @Override
    public Resource load(String storageKey) {
        Path target = resolveSafePath(storageKey);
        if (!Files.exists(target) || !Files.isReadable(target)) {
            throw new IllegalArgumentException("Document not found or not readable at key: " + storageKey);
        }
        return new FileSystemResource(target);
    }

    @Override
    public boolean exists(String storageKey) {
        try {
            Path target = resolveSafePath(storageKey);
            return Files.exists(target);
        } catch (Exception e) {
            return false;
        }
    }

    public Path getRootDir() {
        return rootDir;
    }

    private Path resolveSafePath(String storageKey) {
        String sanitizedKey = storageKey.replace('\\', '/').trim();
        while (sanitizedKey.startsWith("/")) {
            sanitizedKey = sanitizedKey.substring(1);
        }
        Path resolved = rootDir.resolve(sanitizedKey).normalize();
        if (!resolved.startsWith(rootDir)) {
            throw new SecurityException("Path traversal attempt detected with key: " + storageKey);
        }
        return resolved;
    }

    private String computeSha256(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data);
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}
