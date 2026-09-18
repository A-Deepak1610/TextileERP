package com.textile.erp.invoice.application;

import com.textile.erp.invoice.domain.InvoiceNotFoundException;
import com.textile.erp.invoice.domain.InvoiceNotIssuedException;
import com.textile.erp.invoice.domain.InvoicePdfGenerationException;
import com.textile.erp.invoice.domain.InvoicePdfNotReadyException;
import com.textile.erp.invoice.domain.InvoicePdfStatus;
import com.textile.erp.invoice.entity.Invoice;
import com.textile.erp.invoice.entity.InvoiceStatus;
import com.textile.erp.invoice.infrastructure.jasper.InvoicePdfData;
import com.textile.erp.invoice.infrastructure.jasper.InvoicePdfDataMapper;
import com.textile.erp.invoice.infrastructure.jasper.JasperInvoicePdfGenerator;
import com.textile.erp.invoice.infrastructure.storage.DocumentStorage;
import com.textile.erp.invoice.infrastructure.storage.StoredDocument;
import com.textile.erp.invoice.persistence.InvoiceDocumentEntity;
import com.textile.erp.invoice.persistence.InvoiceDocumentRepository;
import com.textile.erp.invoice.repository.InvoiceRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvoicePdfApplicationService {

    private static final String DOCUMENT_TYPE_INVOICE_PDF = "INVOICE_PDF";
    private static final String STORAGE_PROVIDER_LOCAL = "LOCAL";
    private static final String CONTENT_TYPE_PDF = "application/pdf";

    private final InvoiceRepository invoiceRepository;
    private final InvoiceDocumentRepository invoiceDocumentRepository;
    private final DocumentStorage documentStorage;
    private final InvoicePdfDataMapper invoicePdfDataMapper;
    private final JasperInvoicePdfGenerator jasperInvoicePdfGenerator;

    @Getter
    @Builder
    public static class PdfDownloadResult {
        private final Resource resource;
        private final String fileName;
        private final String contentType;
        private final long fileSize;
    }

    @Transactional
    public InvoiceDocumentEntity generatePdf(UUID invoiceId, UUID tenantId, boolean force) {
        log.info("Requesting PDF generation for invoice {} under tenant {} (force: {})", invoiceId, tenantId, force);

        Invoice invoice = invoiceRepository.findByTenantIdAndId(tenantId, invoiceId)
                .orElseThrow(() -> new InvoiceNotFoundException("Invoice not found with ID: " + invoiceId));

        if (invoice.getStatus() != InvoiceStatus.ISSUED) {
            throw new InvoiceNotIssuedException(
                    "Cannot generate PDF for invoice with status " + invoice.getStatus() + ". Only ISSUED invoices are allowed.");
        }

        Optional<InvoiceDocumentEntity> existingDocOpt = invoiceDocumentRepository
                .findByTenantIdAndInvoiceIdAndDocumentType(tenantId, invoiceId, DOCUMENT_TYPE_INVOICE_PDF);

        if (existingDocOpt.isPresent()) {
            InvoiceDocumentEntity existing = existingDocOpt.get();
            if (existing.getStatus() == InvoicePdfStatus.READY && !force) {
                log.info("PDF document for invoice {} is already READY. Returning existing document ID: {}", invoiceId, existing.getId());
                return existing;
            }
        }

        InvoiceDocumentEntity document;
        try {
            document = existingDocOpt.orElseGet(() -> InvoiceDocumentEntity.builder()
                    .tenantId(tenantId)
                    .invoiceId(invoiceId)
                    .documentType(DOCUMENT_TYPE_INVOICE_PDF)
                    .storageProvider(STORAGE_PROVIDER_LOCAL)
                    .storageKey("")
                    .fileName("")
                    .fileSize(0L)
                    .status(InvoicePdfStatus.PENDING)
                    .build());
            document.setStatus(InvoicePdfStatus.GENERATING);
            document = invoiceDocumentRepository.saveAndFlush(document);
        } catch (org.springframework.dao.DataIntegrityViolationException dive) {
            // Concurrent creation occurred - reload existing
            document = invoiceDocumentRepository
                    .findByTenantIdAndInvoiceIdAndDocumentType(tenantId, invoiceId, DOCUMENT_TYPE_INVOICE_PDF)
                    .orElseThrow(() -> dive);
            if (document.getStatus() == InvoicePdfStatus.READY && !force) {
                return document;
            }
            document.setStatus(InvoicePdfStatus.GENERATING);
            document = invoiceDocumentRepository.saveAndFlush(document);
        }

        try {
            // 1. Map invoice entities & snapshots to PDF data
            InvoicePdfData pdfData = invoicePdfDataMapper.mapToPdfData(invoice);

            // 2. Render JasperReports PDF bytes
            byte[] pdfBytes = jasperInvoicePdfGenerator.generatePdf(pdfData);

            // 3. Build safe tenant-isolated storage path
            String sanitizedNum = invoice.getInvoiceNumber().replace('/', '-').replace('\\', '-');
            String financialYear = invoice.getFinancialYear() != null ? invoice.getFinancialYear() : "current";
            String storageKey = String.format("tenant/%s/invoices/%s/%s.pdf", tenantId, financialYear, sanitizedNum);

            // 4. Store using DocumentStorage
            StoredDocument stored = documentStorage.store(storageKey, pdfBytes, CONTENT_TYPE_PDF);

            // 5. Update document metadata
            document.setStorageProvider(STORAGE_PROVIDER_LOCAL);
            document.setStorageKey(stored.getStorageKey());
            document.setFileName(stored.getFileName());
            document.setContentType(stored.getContentType());
            document.setFileSize(stored.getFileSize());
            document.setChecksumSha256(stored.getChecksumSha256());
            document.setStatus(InvoicePdfStatus.READY);
            document.setFailureReason(null);
            document.setGeneratedAt(Instant.now());

            InvoiceDocumentEntity savedDoc = invoiceDocumentRepository.saveAndFlush(document);

            // Update invoice pdfUrl reference
            invoice.setPdfUrl(stored.getStorageKey());
            invoiceRepository.saveAndFlush(invoice);

            log.info("Successfully generated and stored invoice PDF for {} at {}", invoice.getInvoiceNumber(), stored.getStorageKey());
            return savedDoc;
        } catch (Exception e) {
            log.error("Failed to generate and store PDF for invoice {}: {}", invoiceId, e.getMessage(), e);
            document.setStatus(InvoicePdfStatus.FAILED);
            document.setFailureReason(e.getMessage());
            invoiceDocumentRepository.saveAndFlush(document);
            throw new InvoicePdfGenerationException("Invoice PDF generation failed: " + e.getMessage(), e);
        }
    }

    @Transactional(readOnly = true)
    public PdfDownloadResult getPdfForDownload(UUID invoiceId, UUID tenantId) {
        Invoice invoice = invoiceRepository.findByTenantIdAndId(tenantId, invoiceId)
                .orElseThrow(() -> new InvoiceNotFoundException("Invoice not found with ID: " + invoiceId));

        if (invoice.getStatus() != InvoiceStatus.ISSUED) {
            throw new InvoiceNotIssuedException("Cannot download PDF for invoice with status " + invoice.getStatus());
        }

        InvoiceDocumentEntity document = invoiceDocumentRepository
                .findByTenantIdAndInvoiceIdAndDocumentType(tenantId, invoiceId, DOCUMENT_TYPE_INVOICE_PDF)
                .orElseThrow(() -> new InvoicePdfNotReadyException("Invoice PDF has not been generated yet"));

        if (document.getStatus() == InvoicePdfStatus.FAILED) {
            throw new InvoicePdfGenerationException("Invoice PDF generation failed: " + document.getFailureReason());
        }

        if (document.getStatus() != InvoicePdfStatus.READY) {
            throw new InvoicePdfNotReadyException("Invoice PDF is currently being generated. Status: " + document.getStatus());
        }

        Resource resource = documentStorage.load(document.getStorageKey());

        return PdfDownloadResult.builder()
                .resource(resource)
                .fileName(document.getFileName())
                .contentType(document.getContentType())
                .fileSize(document.getFileSize())
                .build();
    }
}
