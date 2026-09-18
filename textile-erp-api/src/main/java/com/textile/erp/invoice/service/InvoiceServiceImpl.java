package com.textile.erp.invoice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.textile.erp.agent.entity.Agent;
import com.textile.erp.agent.entity.CommissionType;
import com.textile.erp.agent.repository.AgentRepository;
import com.textile.erp.auth.security.CurrentUser;
import com.textile.erp.auth.security.SecurityUtils;
import com.textile.erp.customer.entity.BillingAddress;
import com.textile.erp.customer.entity.Customer;
import com.textile.erp.customer.entity.ShippingParty;
import com.textile.erp.customer.repository.CustomerRepository;
import com.textile.erp.customer.repository.ShippingPartyRepository;
import com.textile.erp.invoice.dto.BankSnapshot;
import com.textile.erp.invoice.dto.BillingSnapshot;
import com.textile.erp.invoice.dto.CancelInvoiceRequest;
import com.textile.erp.invoice.dto.CreateInvoiceRequest;
import com.textile.erp.invoice.dto.InvoiceBaleRequest;
import com.textile.erp.invoice.dto.InvoiceBaleResponse;
import com.textile.erp.invoice.dto.InvoiceItemRequest;
import com.textile.erp.invoice.dto.InvoiceItemResponse;
import com.textile.erp.invoice.dto.InvoiceResponse;
import com.textile.erp.invoice.dto.InvoiceSummaryResponse;
import com.textile.erp.invoice.dto.PaymentResponse;
import com.textile.erp.invoice.dto.RecordPaymentRequest;
import com.textile.erp.invoice.dto.SellerSnapshot;
import com.textile.erp.invoice.dto.ShippingSnapshot;
import com.textile.erp.invoice.dto.UpdateInvoiceRequest;
import com.textile.erp.invoice.entity.Invoice;
import com.textile.erp.invoice.entity.InvoiceBale;
import com.textile.erp.invoice.entity.InvoiceItem;
import com.textile.erp.invoice.entity.InvoicePayment;
import com.textile.erp.invoice.entity.InvoiceStatus;
import com.textile.erp.invoice.entity.InvoiceType;
import com.textile.erp.invoice.entity.LedgerReferenceType;
import com.textile.erp.invoice.entity.PaymentMode;
import com.textile.erp.invoice.entity.PaymentStatus;
import com.textile.erp.invoice.repository.InvoiceBaleRepository;
import com.textile.erp.invoice.repository.InvoiceItemRepository;
import com.textile.erp.invoice.repository.InvoicePaymentRepository;
import com.textile.erp.invoice.repository.InvoiceRepository;
import com.textile.erp.product.entity.Product;
import com.textile.erp.product.repository.ProductRepository;
import com.textile.erp.tenant.entity.Address;
import com.textile.erp.tenant.entity.BankInfo;
import com.textile.erp.tenant.entity.CompanyIdentity;
import com.textile.erp.tenant.entity.ContactInfo;
import com.textile.erp.tenant.entity.TaxInfo;
import com.textile.erp.tenant.entity.TenantProfile;
import com.textile.erp.tenant.repository.TenantProfileRepository;
import com.textile.erp.user.entity.Tenant;
import com.textile.erp.user.repository.TenantRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvoiceServiceImpl implements InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceItemRepository invoiceItemRepository;
    private final InvoiceBaleRepository invoiceBaleRepository;
    private final InvoicePaymentRepository invoicePaymentRepository;
    private final CustomerRepository customerRepository;
    private final ShippingPartyRepository shippingPartyRepository;
    private final ProductRepository productRepository;
    private final AgentRepository agentRepository;
    private final TenantProfileRepository tenantProfileRepository;
    private final TenantRepository tenantRepository;
    private final InvoiceCalculationService calculationService;
    private final InvoiceNumberGenerator invoiceNumberGenerator;
    private final InvoiceStorageService invoiceStorageService;
    private final CustomerLedgerService customerLedgerService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Transactional
    public InvoiceResponse createInvoice(CreateInvoiceRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("CreateInvoiceRequest cannot be null");
        }
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new IllegalArgumentException("At least one invoice item is required");
        }

        UUID tenantId = resolveCurrentTenantId();
        CurrentUser currentUser = SecurityUtils.getCurrentUser().orElse(null);
        UUID createdBy = (currentUser != null) ? currentUser.getUserId() : null;

        LocalDate invoiceDate = (request.getInvoiceDate() != null) ? request.getInvoiceDate() : LocalDate.now();
        String financialYear = invoiceNumberGenerator.calculateFinancialYear(invoiceDate);

        // 1. Resolve Customer & Snapshots
        Customer customer = customerRepository.findByTenantIdAndId(tenantId, request.getCustomerId())
                .orElseThrow(() -> new NoSuchElementException("Customer not found with ID: " + request.getCustomerId()));
        BillingSnapshot billingSnapshot = buildBillingSnapshot(customer);

        // 2. Resolve Shipping Party Snapshot
        ShippingSnapshot shippingSnapshot = resolveShippingSnapshot(tenantId, customer, request.getShippingAddressId(), request.getSameAsBilling());

        // 3. Resolve Seller & Bank Snapshots
        TenantProfile profile = tenantProfileRepository.findById(tenantId).orElse(null);
        Tenant tenant = tenantRepository.findById(tenantId).orElse(null);
        SellerSnapshot sellerSnapshot = buildSellerSnapshot(tenant, profile);
        BankSnapshot bankSnapshot = buildBankSnapshot(profile);
        String termsSnapshot = (profile != null && profile.getInvoiceConfig() != null)
                ? profile.getInvoiceConfig().getTermsAndConditions() : null;

        // 4. Resolve Agent Snapshot
        Agent agent = null;
        String agentNameSnapshot = null;
        CommissionType commissionType = request.getCommissionType();
        BigDecimal commissionValue = request.getCommissionValue();

        if (request.getAgentId() != null) {
            agent = agentRepository.findByTenantIdAndId(tenantId, request.getAgentId())
                    .orElseThrow(() -> new NoSuchElementException("Agent not found with ID: " + request.getAgentId()));
            agentNameSnapshot = agent.getAgentName();
            if (commissionType == null) {
                commissionType = agent.getDefaultCommissionType();
            }
            if (commissionValue == null) {
                commissionValue = agent.getDefaultCommissionValue();
            }
        }

        // 5. Calculate Items
        List<InvoiceCalculationService.CalculatedItem> calculatedItems = new ArrayList<>();
        List<InvoiceItem> entityItems = new ArrayList<>();

        for (InvoiceItemRequest itemReq : request.getItems()) {
            Product product = null;
            String desc = itemReq.getDescription();
            String hsn = itemReq.getHsnCode();
            String unit = (itemReq.getUnit() != null && !itemReq.getUnit().isBlank()) ? itemReq.getUnit() : "METER";
            BigDecimal rate = itemReq.getRatePerMeter();

            if (itemReq.getProductId() != null) {
                product = productRepository.findByTenantIdAndId(tenantId, itemReq.getProductId())
                        .orElseThrow(() -> new NoSuchElementException("Product not found with ID: " + itemReq.getProductId()));
                if (desc == null || desc.isBlank()) {
                    desc = product.getProductName();
                }
                if (hsn == null || hsn.isBlank()) {
                    hsn = product.getHsnCode();
                }
                if (unit == null || unit.isBlank()) {
                    unit = product.getUnit();
                }
                if (rate == null || rate.compareTo(BigDecimal.ZERO) <= 0) {
                    rate = product.getDefaultRatePerUnit();
                }
            }

            InvoiceCalculationService.CalculatedItem calculated = calculationService.calculateItem(
                    itemReq.getMeters(), itemReq.getFoldingLessPercent(), rate);
            calculatedItems.add(calculated);

            InvoiceItem entityItem = InvoiceItem.builder()
                    .tenantId(tenantId)
                    .productId(product != null ? product.getId() : null)
                    .descriptionSnapshot(desc)
                    .hsnCodeSnapshot(hsn)
                    .unit(unit)
                    .meters(calculated.getMeters())
                    .foldingLessPercent(calculated.getFoldingLessPercent())
                    .foldingLessMeters(calculated.getFoldingLessMeters())
                    .totalMeters(calculated.getTotalMeters())
                    .ratePerMeter(calculated.getRatePerMeter())
                    .taxableAmount(calculated.getTaxableAmount())
                    .baleDetails(itemReq.getBaleDetails())
                    .totalBales(itemReq.getTotalBales() != null ? itemReq.getTotalBales() : 0)
                    .build();

            if (itemReq.getBales() != null) {
                for (InvoiceBaleRequest baleReq : itemReq.getBales()) {
                    InvoiceBale bale = InvoiceBale.builder()
                            .tenantId(tenantId)
                            .baleNumber(baleReq.getBaleNumber())
                            .pieceCount(baleReq.getPieceCount())
                            .meters(baleReq.getMeters())
                            .netWeight(baleReq.getNetWeight())
                            .grossWeight(baleReq.getGrossWeight())
                            .remarks(baleReq.getRemarks())
                            .build();
                    entityItem.addBale(bale);
                }
            }
            entityItems.add(entityItem);
        }

        // 6. Calculate Totals & Taxes
        String buyerState = (shippingSnapshot.getState() != null) ? shippingSnapshot.getState() : billingSnapshot.getState();
        String buyerStateCode = (shippingSnapshot.getStateCode() != null) ? shippingSnapshot.getStateCode() : billingSnapshot.getStateCode();

        InvoiceCalculationService.InvoiceTotals totals = calculationService.calculateInvoiceTotals(
                calculatedItems,
                sellerSnapshot.getState(),
                sellerSnapshot.getStateCode(),
                buyerState,
                buyerStateCode
        );

        BigDecimal commissionAmount = calculationService.calculateCommission(
                totals.getSubtotal(), commissionType, commissionValue);

        // 7. Payment terms & due date
        int creditDays = (request.getCreditDays() != null) ? request.getCreditDays() : 0;
        LocalDate dueDate = invoiceDate.plusDays(creditDays);

        boolean issueNow = Boolean.TRUE.equals(request.getIssueNow());
        InvoiceStatus initialStatus = issueNow ? InvoiceStatus.ISSUED : InvoiceStatus.DRAFT;
        String invoiceNumber = issueNow
                ? invoiceNumberGenerator.generateNextInvoiceNumber(tenantId, invoiceDate)
                : "DRAFT-" + System.currentTimeMillis() + "-" + (int)(Math.random() * 1000);

        Invoice invoice = Invoice.builder()
                .tenantId(tenantId)
                .invoiceNumber(invoiceNumber)
                .financialYear(financialYear)
                .invoiceDate(invoiceDate)
                .invoiceType(InvoiceType.TAX_INVOICE)
                .status(initialStatus)
                .customerId(customer.getId())
                .shippingAddressId(request.getShippingAddressId())
                .agentId(agent != null ? agent.getId() : null)
                .sellerSnapshotJson(toJson(sellerSnapshot))
                .billingSnapshotJson(toJson(billingSnapshot))
                .shippingSnapshotJson(toJson(shippingSnapshot))
                .bankSnapshotJson(toJson(bankSnapshot))
                .agentNameSnapshot(agentNameSnapshot)
                .termsAndConditionsSnapshot(termsSnapshot)
                .subtotal(totals.getSubtotal())
                .cgstRate(totals.getCgstRate())
                .cgstAmount(totals.getCgstAmount())
                .sgstRate(totals.getSgstRate())
                .sgstAmount(totals.getSgstAmount())
                .igstRate(totals.getIgstRate())
                .igstAmount(totals.getIgstAmount())
                .roundOffAmount(totals.getRoundOffAmount())
                .grandTotal(totals.getGrandTotal())
                .amountInWords(totals.getAmountInWords())
                .paymentMode(request.getPaymentMode())
                .creditDays(creditDays)
                .dueDate(dueDate)
                .paymentTermsNote(request.getPaymentTermsNote())
                .totalPaidAmount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                .dueAmount(totals.getGrandTotal())
                .paymentStatus(PaymentStatus.UNPAID)
                .commissionType(commissionType != null ? commissionType.name() : null)
                .commissionValue(commissionValue != null ? commissionValue : BigDecimal.ZERO)
                .commissionAmount(commissionAmount)
                .agentNotes(request.getAgentNotes())
                .createdBy(createdBy)
                .issuedAt(issueNow ? Instant.now() : null)
                .build();

        for (InvoiceItem item : entityItems) {
            invoice.addItem(item);
        }

        Invoice saved = invoiceRepository.saveAndFlush(invoice);

        if (issueNow) {
            String pdfUrl = invoiceStorageService.generateAndStoreInvoicePdf(tenantId, saved);
            saved.setPdfUrl(pdfUrl);
            customerLedgerService.recordDebit(
                    tenantId, customer.getId(), saved.getId(), invoiceDate,
                    saved.getGrandTotal(), saved.getInvoiceNumber(), "Sales invoice issued"
            );
            saved = invoiceRepository.saveAndFlush(saved);
        }

        log.info("Created invoice {} (ID: {}) with status {} for tenant {}",
                saved.getInvoiceNumber(), saved.getId(), saved.getStatus(), tenantId);
        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public InvoiceResponse getInvoiceById(UUID id) {
        UUID tenantId = resolveCurrentTenantId();
        Invoice invoice = findSecured(tenantId, id);
        return mapToResponse(invoice);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<InvoiceSummaryResponse> searchInvoices(
            String query,
            InvoiceStatus status,
            PaymentStatus paymentStatus,
            UUID customerId,
            LocalDate startDate,
            LocalDate endDate,
            Pageable pageable) {

        UUID tenantId = resolveCurrentTenantId();
        String term = (query != null && !query.isBlank()) ? query.trim() : null;

        return invoiceRepository.searchInvoices(tenantId, term, status, paymentStatus, customerId, startDate, endDate, pageable)
                .map(this::mapToSummaryResponse);
    }

    @Override
    @Transactional
    public InvoiceResponse updateInvoice(UUID id, UpdateInvoiceRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("UpdateInvoiceRequest cannot be null");
        }
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new IllegalArgumentException("At least one invoice item is required");
        }

        UUID tenantId = resolveCurrentTenantId();
        Invoice invoice = findSecured(tenantId, id);

        if (invoice.getStatus() != InvoiceStatus.DRAFT) {
            throw new IllegalStateException("Cannot edit invoice in " + invoice.getStatus()
                    + " status. Issued and cancelled invoices are immutable.");
        }

        LocalDate invoiceDate = (request.getInvoiceDate() != null) ? request.getInvoiceDate() : invoice.getInvoiceDate();
        invoice.setInvoiceDate(invoiceDate);
        invoice.setFinancialYear(invoiceNumberGenerator.calculateFinancialYear(invoiceDate));

        // Update shipping snapshot if address changed
        Customer customer = customerRepository.findByTenantIdAndId(tenantId, invoice.getCustomerId())
                .orElseThrow(() -> new NoSuchElementException("Customer not found with ID: " + invoice.getCustomerId()));
        ShippingSnapshot shippingSnapshot = resolveShippingSnapshot(tenantId, customer, request.getShippingAddressId(), request.getSameAsBilling());
        invoice.setShippingSnapshotJson(toJson(shippingSnapshot));
        invoice.setShippingAddressId(request.getShippingAddressId());

        // Update Agent if changed
        if (request.getAgentId() != null) {
            Agent agent = agentRepository.findByTenantIdAndId(tenantId, request.getAgentId())
                    .orElseThrow(() -> new NoSuchElementException("Agent not found with ID: " + request.getAgentId()));
            invoice.setAgentId(agent.getId());
            invoice.setAgentNameSnapshot(agent.getAgentName());
            invoice.setCommissionType(request.getCommissionType() != null ? request.getCommissionType().name() : agent.getDefaultCommissionType().name());
            invoice.setCommissionValue(request.getCommissionValue() != null ? request.getCommissionValue() : agent.getDefaultCommissionValue());
        }
        if (request.getAgentNotes() != null) {
            invoice.setAgentNotes(request.getAgentNotes());
        }

        // Recalculate Items
        invoice.getItems().clear();
        List<InvoiceCalculationService.CalculatedItem> calculatedItems = new ArrayList<>();

        for (InvoiceItemRequest itemReq : request.getItems()) {
            Product product = null;
            String desc = itemReq.getDescription();
            String hsn = itemReq.getHsnCode();
            String unit = (itemReq.getUnit() != null && !itemReq.getUnit().isBlank()) ? itemReq.getUnit() : "METER";
            BigDecimal rate = itemReq.getRatePerMeter();

            if (itemReq.getProductId() != null) {
                product = productRepository.findByTenantIdAndId(tenantId, itemReq.getProductId())
                        .orElseThrow(() -> new NoSuchElementException("Product not found with ID: " + itemReq.getProductId()));
                if (desc == null || desc.isBlank()) {
                    desc = product.getProductName();
                }
                if (hsn == null || hsn.isBlank()) {
                    hsn = product.getHsnCode();
                }
                if (unit == null || unit.isBlank()) {
                    unit = product.getUnit();
                }
                if (rate == null || rate.compareTo(BigDecimal.ZERO) <= 0) {
                    rate = product.getDefaultRatePerUnit();
                }
            }

            InvoiceCalculationService.CalculatedItem calculated = calculationService.calculateItem(
                    itemReq.getMeters(), itemReq.getFoldingLessPercent(), rate);
            calculatedItems.add(calculated);

            InvoiceItem entityItem = InvoiceItem.builder()
                    .tenantId(tenantId)
                    .productId(product != null ? product.getId() : null)
                    .descriptionSnapshot(desc)
                    .hsnCodeSnapshot(hsn)
                    .unit(unit)
                    .meters(calculated.getMeters())
                    .foldingLessPercent(calculated.getFoldingLessPercent())
                    .foldingLessMeters(calculated.getFoldingLessMeters())
                    .totalMeters(calculated.getTotalMeters())
                    .ratePerMeter(calculated.getRatePerMeter())
                    .taxableAmount(calculated.getTaxableAmount())
                    .baleDetails(itemReq.getBaleDetails())
                    .totalBales(itemReq.getTotalBales() != null ? itemReq.getTotalBales() : 0)
                    .build();

            if (itemReq.getBales() != null) {
                for (InvoiceBaleRequest baleReq : itemReq.getBales()) {
                    InvoiceBale bale = InvoiceBale.builder()
                            .tenantId(tenantId)
                            .baleNumber(baleReq.getBaleNumber())
                            .pieceCount(baleReq.getPieceCount())
                            .meters(baleReq.getMeters())
                            .netWeight(baleReq.getNetWeight())
                            .grossWeight(baleReq.getGrossWeight())
                            .remarks(baleReq.getRemarks())
                            .build();
                    entityItem.addBale(bale);
                }
            }
            invoice.addItem(entityItem);
        }

        SellerSnapshot sellerSnapshot = fromJson(invoice.getSellerSnapshotJson(), SellerSnapshot.class);
        BillingSnapshot billingSnapshot = fromJson(invoice.getBillingSnapshotJson(), BillingSnapshot.class);

        String buyerState = (shippingSnapshot.getState() != null) ? shippingSnapshot.getState() : billingSnapshot.getState();
        String buyerStateCode = (shippingSnapshot.getStateCode() != null) ? shippingSnapshot.getStateCode() : billingSnapshot.getStateCode();

        InvoiceCalculationService.InvoiceTotals totals = calculationService.calculateInvoiceTotals(
                calculatedItems,
                sellerSnapshot.getState(),
                sellerSnapshot.getStateCode(),
                buyerState,
                buyerStateCode
        );

        CommissionType commType = (invoice.getCommissionType() != null) ? CommissionType.valueOf(invoice.getCommissionType()) : null;
        BigDecimal commissionAmount = calculationService.calculateCommission(totals.getSubtotal(), commType, invoice.getCommissionValue());

        invoice.setSubtotal(totals.getSubtotal());
        invoice.setCgstRate(totals.getCgstRate());
        invoice.setCgstAmount(totals.getCgstAmount());
        invoice.setSgstRate(totals.getSgstRate());
        invoice.setSgstAmount(totals.getSgstAmount());
        invoice.setIgstRate(totals.getIgstRate());
        invoice.setIgstAmount(totals.getIgstAmount());
        invoice.setRoundOffAmount(totals.getRoundOffAmount());
        invoice.setGrandTotal(totals.getGrandTotal());
        invoice.setAmountInWords(totals.getAmountInWords());
        invoice.setDueAmount(totals.getGrandTotal().subtract(invoice.getTotalPaidAmount()));
        invoice.setCommissionAmount(commissionAmount);

        if (request.getPaymentMode() != null) {
            invoice.setPaymentMode(request.getPaymentMode());
        }
        if (request.getCreditDays() != null) {
            invoice.setCreditDays(request.getCreditDays());
            invoice.setDueDate(invoiceDate.plusDays(request.getCreditDays()));
        }
        if (request.getPaymentTermsNote() != null) {
            invoice.setPaymentTermsNote(request.getPaymentTermsNote());
        }

        Invoice updated = invoiceRepository.saveAndFlush(invoice);
        log.info("Updated draft invoice {} for tenant {}", updated.getInvoiceNumber(), tenantId);
        return mapToResponse(updated);
    }

    @Override
    @Transactional
    public InvoiceResponse issueInvoice(UUID id) {
        UUID tenantId = resolveCurrentTenantId();
        Invoice invoice = findSecured(tenantId, id);

        if (invoice.getStatus() != InvoiceStatus.DRAFT) {
            throw new IllegalStateException("Only DRAFT invoices can be issued. Current status: " + invoice.getStatus());
        }

        String officialNumber = invoiceNumberGenerator.generateNextInvoiceNumber(tenantId, invoice.getInvoiceDate());
        invoice.setInvoiceNumber(officialNumber);
        invoice.setStatus(InvoiceStatus.ISSUED);
        invoice.setIssuedAt(Instant.now());

        String pdfUrl = invoiceStorageService.generateAndStoreInvoicePdf(tenantId, invoice);
        invoice.setPdfUrl(pdfUrl);

        customerLedgerService.recordDebit(
                tenantId, invoice.getCustomerId(), invoice.getId(), invoice.getInvoiceDate(),
                invoice.getGrandTotal(), officialNumber, "Sales invoice issued"
        );

        Invoice saved = invoiceRepository.saveAndFlush(invoice);
        log.info("Issued invoice {} (ID: {}) for tenant {}", saved.getInvoiceNumber(), saved.getId(), tenantId);
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public InvoiceResponse cancelInvoice(UUID id, CancelInvoiceRequest request) {
        if (request == null || request.getCancellationReason() == null || request.getCancellationReason().isBlank()) {
            throw new IllegalArgumentException("Cancellation reason is required");
        }

        UUID tenantId = resolveCurrentTenantId();
        Invoice invoice = findSecured(tenantId, id);

        if (invoice.getStatus() == InvoiceStatus.DRAFT) {
            throw new IllegalStateException("Draft invoices cannot be cancelled. Use delete instead.");
        }
        if (invoice.getStatus() == InvoiceStatus.CANCELLED) {
            throw new IllegalStateException("Invoice is already cancelled");
        }

        invoice.setStatus(InvoiceStatus.CANCELLED);
        invoice.setCancellationReason(request.getCancellationReason().trim());
        invoice.setCancelledAt(Instant.now());

        // Reverse debtor balance in customer ledger
        customerLedgerService.recordCredit(
                tenantId, invoice.getCustomerId(), invoice.getId(), LocalDate.now(),
                invoice.getGrandTotal(), LedgerReferenceType.INVOICE_CANCELLATION,
                invoice.getInvoiceNumber(), "Invoice cancelled: " + request.getCancellationReason()
        );

        Invoice saved = invoiceRepository.saveAndFlush(invoice);
        log.info("Cancelled invoice {} (ID: {}) for tenant {}", saved.getInvoiceNumber(), id, tenantId);
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public InvoiceResponse recordPayment(UUID id, RecordPaymentRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("RecordPaymentRequest cannot be null");
        }

        UUID tenantId = resolveCurrentTenantId();
        Invoice invoice = findSecured(tenantId, id);

        if (invoice.getStatus() != InvoiceStatus.ISSUED) {
            throw new IllegalStateException("Payments can only be recorded against ISSUED invoices. Current status: " + invoice.getStatus());
        }
        if (invoice.getPaymentStatus() == PaymentStatus.PAID) {
            throw new IllegalStateException("Invoice is already fully paid");
        }
        if (request.getAmount().compareTo(invoice.getDueAmount()) > 0) {
            throw new IllegalArgumentException(String.format(
                    "Payment amount %s exceeds invoice due amount %s",
                    request.getAmount(), invoice.getDueAmount()));
        }

        LocalDate payDate = (request.getPaymentDate() != null) ? request.getPaymentDate() : LocalDate.now();

        InvoicePayment payment = InvoicePayment.builder()
                .tenantId(tenantId)
                .paymentDate(payDate)
                .amount(request.getAmount().setScale(2, RoundingMode.HALF_UP))
                .paymentMode(request.getPaymentMode())
                .referenceNumber(request.getReferenceNumber())
                .notes(request.getNotes())
                .build();

        invoice.addPayment(payment);

        BigDecimal newPaidTotal = invoice.getTotalPaidAmount().add(payment.getAmount()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal newDue = invoice.getGrandTotal().subtract(newPaidTotal).setScale(2, RoundingMode.HALF_UP);

        invoice.setTotalPaidAmount(newPaidTotal);
        invoice.setDueAmount(newDue.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP) : newDue);

        if (invoice.getDueAmount().compareTo(BigDecimal.ZERO) <= 0) {
            invoice.setPaymentStatus(PaymentStatus.PAID);
        } else {
            invoice.setPaymentStatus(PaymentStatus.PARTIALLY_PAID);
        }

        // Record credit in customer ledger
        customerLedgerService.recordCredit(
                tenantId, invoice.getCustomerId(), invoice.getId(), payDate,
                payment.getAmount(), LedgerReferenceType.PAYMENT,
                (request.getReferenceNumber() != null) ? request.getReferenceNumber() : invoice.getInvoiceNumber(),
                "Payment received via " + request.getPaymentMode()
        );

        Invoice saved = invoiceRepository.saveAndFlush(invoice);
        log.info("Recorded payment of {} on invoice {}, new status {}",
                payment.getAmount(), saved.getInvoiceNumber(), saved.getPaymentStatus());
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public void deleteInvoice(UUID id) {
        UUID tenantId = resolveCurrentTenantId();
        Invoice invoice = findSecured(tenantId, id);

        if (invoice.getStatus() != InvoiceStatus.DRAFT) {
            throw new IllegalStateException("Cannot delete an invoice in " + invoice.getStatus()
                    + " status. Only DRAFT invoices can be deleted.");
        }

        invoiceRepository.delete(invoice);
        log.info("Deleted draft invoice {} for tenant {}", id, tenantId);
    }

    private Invoice findSecured(UUID tenantId, UUID id) {
        return invoiceRepository.findByTenantIdAndId(tenantId, id)
                .orElseThrow(() -> new NoSuchElementException("Invoice not found with ID: " + id));
    }

    private UUID resolveCurrentTenantId() {
        CurrentUser currentUser = SecurityUtils.getCurrentUser()
                .orElseThrow(() -> new AccessDeniedException("Authentication required"));
        if (currentUser.getTenantId() != null) {
            return currentUser.getTenantId();
        }
        throw new IllegalArgumentException("Action requires an active tenant context");
    }

    private BillingSnapshot buildBillingSnapshot(Customer customer) {
        BillingAddress addr = customer.getBillingAddress();
        String fullAddress = (addr != null) ? (addr.getAddressLine1() + " " + (addr.getAddressLine2() != null ? addr.getAddressLine2() : "")).trim() : null;

        return BillingSnapshot.builder()
                .customerId(customer.getId())
                .customerCode(customer.getCustomerCode())
                .customerName(customer.getName())
                .gstin(customer.getGstin())
                .pan(customer.getPan())
                .billingAddress(fullAddress)
                .city(addr != null ? addr.getCity() : null)
                .state(addr != null ? addr.getState() : null)
                .stateCode(addr != null ? addr.getStateCode() : null)
                .pincode(addr != null ? addr.getPincode() : null)
                .phone(customer.getPhone())
                .email(customer.getEmail())
                .build();
    }

    private ShippingSnapshot resolveShippingSnapshot(UUID tenantId, Customer customer, UUID shippingAddressId, Boolean sameAsBilling) {
        if (Boolean.TRUE.equals(sameAsBilling) || shippingAddressId == null) {
            BillingAddress addr = customer.getBillingAddress();
            String fullAddress = (addr != null) ? (addr.getAddressLine1() + " " + (addr.getAddressLine2() != null ? addr.getAddressLine2() : "")).trim() : null;

            return ShippingSnapshot.builder()
                    .sameAsBilling(true)
                    .shippingAddressId(null)
                    .shippingPartyName(customer.getName())
                    .gstin(customer.getGstin())
                    .pan(customer.getPan())
                    .shippingAddress(fullAddress)
                    .city(addr != null ? addr.getCity() : null)
                    .state(addr != null ? addr.getState() : null)
                    .stateCode(addr != null ? addr.getStateCode() : null)
                    .pincode(addr != null ? addr.getPincode() : null)
                    .phone(customer.getPhone())
                    .email(customer.getEmail())
                    .build();
        }

        ShippingParty party = shippingPartyRepository.findByTenantIdAndCustomerIdAndId(tenantId, customer.getId(), shippingAddressId)
                .orElseThrow(() -> new NoSuchElementException("Shipping destination party not found with ID: " + shippingAddressId));

        String fullAddress = (party.getAddressLine1() + " " + (party.getAddressLine2() != null ? party.getAddressLine2() : "")).trim();

        return ShippingSnapshot.builder()
                .sameAsBilling(false)
                .shippingAddressId(party.getId())
                .shippingPartyName(party.getName())
                .gstin(party.getGstin())
                .pan(party.getPan())
                .shippingAddress(fullAddress)
                .city(party.getCity())
                .state(party.getState())
                .stateCode(party.getStateCode())
                .pincode(party.getPincode())
                .phone(party.getPhone())
                .email(party.getEmail())
                .build();
    }

    private SellerSnapshot buildSellerSnapshot(Tenant tenant, TenantProfile profile) {
        String companyName = (profile != null && profile.getCompanyIdentity() != null && profile.getCompanyIdentity().getTradeName() != null)
                ? profile.getCompanyIdentity().getTradeName()
                : (profile != null && profile.getCompanyIdentity() != null && profile.getCompanyIdentity().getLegalName() != null)
                ? profile.getCompanyIdentity().getLegalName()
                : (tenant != null ? tenant.getName() : "Textile Company");

        Address addr = (profile != null) ? profile.getRegisteredAddress() : null;
        String addressLine = (addr != null)
                ? (addr.getAddressLine1() + " " + (addr.getAddressLine2() != null ? addr.getAddressLine2() : "")).trim()
                : null;

        TaxInfo tax = (profile != null) ? profile.getTaxInfo() : null;
        ContactInfo contact = (profile != null) ? profile.getContactInfo() : null;
        BankInfo bank = (profile != null) ? profile.getBankInfo() : null;

        return SellerSnapshot.builder()
                .companyName(companyName)
                .address(addressLine)
                .gstin(tax != null ? tax.getGstin() : null)
                .pan(tax != null ? tax.getPan() : null)
                .state(addr != null ? addr.getState() : null)
                .stateCode(addr != null ? addr.getStateCode() : null)
                .phone(contact != null ? contact.getPrimaryPhone() : null)
                .email(contact != null ? contact.getPrimaryEmail() : null)
                .bankAccountName(companyName)
                .bankAccountNumber(bank != null ? bank.getAccountNumber() : null)
                .bankName(bank != null ? bank.getBankName() : null)
                .ifsc(bank != null ? bank.getIfscCode() : null)
                .branch(bank != null ? bank.getBranchName() : null)
                .build();
    }

    private BankSnapshot buildBankSnapshot(TenantProfile profile) {
        BankInfo bank = (profile != null) ? profile.getBankInfo() : null;
        if (bank == null) {
            return BankSnapshot.builder().build();
        }
        return BankSnapshot.builder()
                .bankName(bank.getBankName())
                .bankAccountNumber(bank.getAccountNumber())
                .ifsc(bank.getIfscCode())
                .branch(bank.getBranchName())
                .accountType(bank.getAccountType() != null ? bank.getAccountType().name() : null)
                .upiId(bank.getUpiId())
                .build();
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            log.error("Could not serialize snapshot to JSON: {}", e.getMessage());
            return "{}";
        }
    }

    private <T> T fromJson(String json, Class<T> clazz) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, clazz);
        } catch (Exception e) {
            log.error("Could not deserialize JSON: {}", e.getMessage());
            return null;
        }
    }

    private InvoiceResponse mapToResponse(Invoice invoice) {
        SellerSnapshot seller = fromJson(invoice.getSellerSnapshotJson(), SellerSnapshot.class);
        BillingSnapshot billing = fromJson(invoice.getBillingSnapshotJson(), BillingSnapshot.class);
        ShippingSnapshot shipping = fromJson(invoice.getShippingSnapshotJson(), ShippingSnapshot.class);
        BankSnapshot bank = fromJson(invoice.getBankSnapshotJson(), BankSnapshot.class);

        List<InvoiceItemResponse> items = (invoice.getItems() != null)
                ? invoice.getItems().stream().map(this::mapItemToResponse).toList()
                : Collections.emptyList();

        List<PaymentResponse> payments = (invoice.getPayments() != null)
                ? invoice.getPayments().stream().map(this::mapPaymentToResponse).toList()
                : Collections.emptyList();

        return InvoiceResponse.builder()
                .id(invoice.getId())
                .tenantId(invoice.getTenantId())
                .invoiceNumber(invoice.getInvoiceNumber())
                .financialYear(invoice.getFinancialYear())
                .invoiceDate(invoice.getInvoiceDate())
                .invoiceType(invoice.getInvoiceType())
                .status(invoice.getStatus())
                .customerId(invoice.getCustomerId())
                .shippingAddressId(invoice.getShippingAddressId())
                .agentId(invoice.getAgentId())
                .sellerSnapshot(seller)
                .billingSnapshot(billing)
                .shippingSnapshot(shipping)
                .bankSnapshot(bank)
                .agentNameSnapshot(invoice.getAgentNameSnapshot())
                .termsAndConditionsSnapshot(invoice.getTermsAndConditionsSnapshot())
                .subtotal(invoice.getSubtotal())
                .cgstRate(invoice.getCgstRate())
                .cgstAmount(invoice.getCgstAmount())
                .sgstRate(invoice.getSgstRate())
                .sgstAmount(invoice.getSgstAmount())
                .igstRate(invoice.getIgstRate())
                .igstAmount(invoice.getIgstAmount())
                .roundOffAmount(invoice.getRoundOffAmount())
                .grandTotal(invoice.getGrandTotal())
                .amountInWords(invoice.getAmountInWords())
                .paymentMode(invoice.getPaymentMode())
                .creditDays(invoice.getCreditDays())
                .dueDate(invoice.getDueDate())
                .paymentTermsNote(invoice.getPaymentTermsNote())
                .totalPaidAmount(invoice.getTotalPaidAmount())
                .dueAmount(invoice.getDueAmount())
                .paymentStatus(invoice.getPaymentStatus())
                .commissionType(invoice.getCommissionType())
                .commissionValue(invoice.getCommissionValue())
                .commissionAmount(invoice.getCommissionAmount())
                .agentNotes(invoice.getAgentNotes())
                .cancellationReason(invoice.getCancellationReason())
                .pdfUrl(invoice.getPdfUrl())
                .createdBy(invoice.getCreatedBy())
                .issuedAt(invoice.getIssuedAt())
                .cancelledAt(invoice.getCancelledAt())
                .createdAt(invoice.getCreatedAt())
                .updatedAt(invoice.getUpdatedAt())
                .items(items)
                .payments(payments)
                .build();
    }

    private InvoiceItemResponse mapItemToResponse(InvoiceItem item) {
        List<InvoiceBaleResponse> bales = (item.getBales() != null)
                ? item.getBales().stream().map(this::mapBaleToResponse).toList()
                : Collections.emptyList();

        return InvoiceItemResponse.builder()
                .id(item.getId())
                .productId(item.getProductId())
                .descriptionSnapshot(item.getDescriptionSnapshot())
                .hsnCodeSnapshot(item.getHsnCodeSnapshot())
                .unit(item.getUnit())
                .meters(item.getMeters())
                .foldingLessPercent(item.getFoldingLessPercent())
                .foldingLessMeters(item.getFoldingLessMeters())
                .totalMeters(item.getTotalMeters())
                .ratePerMeter(item.getRatePerMeter())
                .taxableAmount(item.getTaxableAmount())
                .baleDetails(item.getBaleDetails())
                .totalBales(item.getTotalBales())
                .bales(bales)
                .createdAt(item.getCreatedAt())
                .build();
    }

    private InvoiceBaleResponse mapBaleToResponse(InvoiceBale bale) {
        return InvoiceBaleResponse.builder()
                .id(bale.getId())
                .baleNumber(bale.getBaleNumber())
                .pieceCount(bale.getPieceCount())
                .meters(bale.getMeters())
                .netWeight(bale.getNetWeight())
                .grossWeight(bale.getGrossWeight())
                .remarks(bale.getRemarks())
                .createdAt(bale.getCreatedAt())
                .build();
    }

    private PaymentResponse mapPaymentToResponse(InvoicePayment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .tenantId(payment.getTenantId())
                .invoiceId(payment.getInvoice().getId())
                .paymentDate(payment.getPaymentDate())
                .amount(payment.getAmount())
                .paymentMode(payment.getPaymentMode())
                .referenceNumber(payment.getReferenceNumber())
                .notes(payment.getNotes())
                .createdAt(payment.getCreatedAt())
                .build();
    }

    private InvoiceSummaryResponse mapToSummaryResponse(Invoice invoice) {
        BillingSnapshot billing = fromJson(invoice.getBillingSnapshotJson(), BillingSnapshot.class);
        String customerName = (billing != null) ? billing.getCustomerName() : null;

        return InvoiceSummaryResponse.builder()
                .id(invoice.getId())
                .invoiceNumber(invoice.getInvoiceNumber())
                .invoiceDate(invoice.getInvoiceDate())
                .invoiceType(invoice.getInvoiceType())
                .status(invoice.getStatus())
                .customerId(invoice.getCustomerId())
                .customerName(customerName)
                .agentName(invoice.getAgentNameSnapshot())
                .subtotal(invoice.getSubtotal())
                .grandTotal(invoice.getGrandTotal())
                .totalPaidAmount(invoice.getTotalPaidAmount())
                .dueAmount(invoice.getDueAmount())
                .paymentStatus(invoice.getPaymentStatus())
                .itemCount(invoice.getItems() != null ? invoice.getItems().size() : 0)
                .createdAt(invoice.getCreatedAt())
                .build();
    }
}
