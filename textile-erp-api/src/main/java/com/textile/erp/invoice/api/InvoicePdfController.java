package com.textile.erp.invoice.api;

import com.textile.erp.auth.security.CurrentUser;
import com.textile.erp.auth.security.SecurityUtils;
import com.textile.erp.invoice.application.InvoicePdfApplicationService;
import com.textile.erp.invoice.application.InvoicePdfApplicationService.PdfDownloadResult;
import com.textile.erp.invoice.dto.InvoiceDocumentResponse;
import com.textile.erp.invoice.persistence.InvoiceDocumentEntity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping({"/api/v1/invoices", "/api/invoices"})
@RequiredArgsConstructor
@Tag(name = "Invoice PDF", description = "Endpoints for generating and downloading JasperReports tax invoice PDFs")
@SecurityRequirement(name = "bearerAuth")
public class InvoicePdfController {

    private final InvoicePdfApplicationService invoicePdfApplicationService;

    @PostMapping("/{invoiceId}/pdf")
    @Operation(summary = "Generate invoice PDF", description = "Generates a JasperReports A4 tax invoice PDF for an ISSUED invoice. If already generated and force=false, returns existing document metadata.")
    @ApiResponse(responseCode = "200", description = "PDF generated or existing ready document returned")
    @ApiResponse(responseCode = "404", description = "Invoice not found")
    @ApiResponse(responseCode = "409", description = "Invoice is not ISSUED")
    @ApiResponse(responseCode = "422", description = "PDF generation failed")
    public ResponseEntity<InvoiceDocumentResponse> generatePdf(
            @PathVariable UUID invoiceId,
            @RequestParam(defaultValue = "false") boolean force) {

        UUID tenantId = resolveCurrentTenantId();
        InvoiceDocumentEntity entity = invoicePdfApplicationService.generatePdf(invoiceId, tenantId, force);

        InvoiceDocumentResponse response = InvoiceDocumentResponse.builder()
                .id(entity.getId())
                .tenantId(entity.getTenantId())
                .invoiceId(entity.getInvoiceId())
                .documentType(entity.getDocumentType())
                .storageProvider(entity.getStorageProvider())
                .storageKey(entity.getStorageKey())
                .fileName(entity.getFileName())
                .contentType(entity.getContentType())
                .fileSize(entity.getFileSize())
                .checksumSha256(entity.getChecksumSha256())
                .status(entity.getStatus())
                .failureReason(entity.getFailureReason())
                .generatedAt(entity.getGeneratedAt())
                .createdAt(entity.getCreatedAt())
                .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{invoiceId}/pdf")
    @Operation(summary = "Download invoice PDF", description = "Streams the generated tax invoice PDF inline for display or download.")
    @ApiResponse(responseCode = "200", description = "PDF stream returned")
    @ApiResponse(responseCode = "404", description = "Invoice or document not found")
    @ApiResponse(responseCode = "409", description = "Invoice not ISSUED or PDF not ready yet")
    @ApiResponse(responseCode = "422", description = "PDF generation had failed")
    public ResponseEntity<Resource> downloadPdf(@PathVariable UUID invoiceId) {
        UUID tenantId = resolveCurrentTenantId();
        PdfDownloadResult download = invoicePdfApplicationService.getPdfForDownload(invoiceId, tenantId);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(download.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + download.getFileName() + "\"")
                .contentLength(download.getFileSize())
                .body(download.getResource());
    }

    private UUID resolveCurrentTenantId() {
        CurrentUser currentUser = SecurityUtils.getCurrentUser()
                .orElseThrow(() -> new AccessDeniedException("Authentication required"));
        if (currentUser.getTenantId() != null) {
            return currentUser.getTenantId();
        }
        throw new IllegalArgumentException("Action requires an active tenant context");
    }
}
