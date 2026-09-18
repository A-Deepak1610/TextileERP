package com.textile.erp.invoice.service;

import com.textile.erp.agent.entity.CommissionType;
import com.textile.erp.invoice.util.NumberToWordsConverter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import org.springframework.stereotype.Service;

@Service
public class InvoiceCalculationService {

    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

    @Getter
    @Builder
    public static class CalculatedItem {
        private final BigDecimal meters;
        private final BigDecimal foldingLessPercent;
        private final BigDecimal foldingLessMeters;
        private final BigDecimal totalMeters;
        private final BigDecimal ratePerMeter;
        private final BigDecimal taxableAmount;
    }

    @Getter
    @Builder
    public static class InvoiceTotals {
        private final BigDecimal subtotal;
        private final BigDecimal cgstRate;
        private final BigDecimal cgstAmount;
        private final BigDecimal sgstRate;
        private final BigDecimal sgstAmount;
        private final BigDecimal igstRate;
        private final BigDecimal igstAmount;
        private final BigDecimal roundOffAmount;
        private final BigDecimal grandTotal;
        private final String amountInWords;
    }

    public CalculatedItem calculateItem(BigDecimal meters, BigDecimal foldingLessPercent, BigDecimal ratePerMeter) {
        if (meters == null || meters.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Item meters must be greater than zero");
        }
        if (ratePerMeter == null || ratePerMeter.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Item rate per meter must be greater than zero");
        }

        BigDecimal foldingPct = (foldingLessPercent != null) ? foldingLessPercent : BigDecimal.ZERO;
        if (foldingPct.compareTo(BigDecimal.ZERO) < 0 || foldingPct.compareTo(ONE_HUNDRED) > 0) {
            throw new IllegalArgumentException("Folding less percentage must be between 0 and 100");
        }

        BigDecimal foldingLessMeters = meters.multiply(foldingPct)
                .divide(ONE_HUNDRED, 3, RoundingMode.HALF_UP);

        BigDecimal totalMeters = meters.subtract(foldingLessMeters)
                .setScale(3, RoundingMode.HALF_UP);

        BigDecimal taxableAmount = totalMeters.multiply(ratePerMeter)
                .setScale(2, RoundingMode.HALF_UP);

        return CalculatedItem.builder()
                .meters(meters.setScale(3, RoundingMode.HALF_UP))
                .foldingLessPercent(foldingPct.setScale(3, RoundingMode.HALF_UP))
                .foldingLessMeters(foldingLessMeters)
                .totalMeters(totalMeters)
                .ratePerMeter(ratePerMeter.setScale(2, RoundingMode.HALF_UP))
                .taxableAmount(taxableAmount)
                .build();
    }

    public InvoiceTotals calculateInvoiceTotals(
            List<CalculatedItem> items,
            String sellerState,
            String sellerStateCode,
            String buyerState,
            String buyerStateCode) {

        BigDecimal subtotal = items.stream()
                .map(CalculatedItem::getTaxableAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        boolean isIntrastate = isSameState(sellerState, sellerStateCode, buyerState, buyerStateCode);

        BigDecimal cgstRate;
        BigDecimal sgstRate;
        BigDecimal igstRate;

        if (isIntrastate) {
            cgstRate = new BigDecimal("2.50");
            sgstRate = new BigDecimal("2.50");
            igstRate = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        } else {
            cgstRate = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            sgstRate = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            igstRate = new BigDecimal("5.00");
        }

        BigDecimal cgstAmount = subtotal.multiply(cgstRate).divide(ONE_HUNDRED, 2, RoundingMode.HALF_UP);
        BigDecimal sgstAmount = subtotal.multiply(sgstRate).divide(ONE_HUNDRED, 2, RoundingMode.HALF_UP);
        BigDecimal igstAmount = subtotal.multiply(igstRate).divide(ONE_HUNDRED, 2, RoundingMode.HALF_UP);

        BigDecimal grandTotalBeforeRounding = subtotal.add(cgstAmount).add(sgstAmount).add(igstAmount);
        BigDecimal grandTotal = grandTotalBeforeRounding.setScale(0, RoundingMode.HALF_UP).setScale(2, RoundingMode.HALF_UP);
        BigDecimal roundOffAmount = grandTotal.subtract(grandTotalBeforeRounding).setScale(2, RoundingMode.HALF_UP);

        String amountInWords = NumberToWordsConverter.convertToIndianCurrencyWords(grandTotal);

        return InvoiceTotals.builder()
                .subtotal(subtotal)
                .cgstRate(cgstRate)
                .cgstAmount(cgstAmount)
                .sgstRate(sgstRate)
                .sgstAmount(sgstAmount)
                .igstRate(igstRate)
                .igstAmount(igstAmount)
                .roundOffAmount(roundOffAmount)
                .grandTotal(grandTotal)
                .amountInWords(amountInWords)
                .build();
    }

    public BigDecimal calculateCommission(
            BigDecimal taxableAmount,
            CommissionType commissionType,
            BigDecimal commissionValue) {

        if (commissionType == null || commissionValue == null || commissionValue.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        if (commissionType == CommissionType.FIXED) {
            return commissionValue.setScale(2, RoundingMode.HALF_UP);
        }

        return taxableAmount.multiply(commissionValue)
                .divide(ONE_HUNDRED, 2, RoundingMode.HALF_UP);
    }

    private boolean isSameState(String sellerState, String sellerStateCode, String buyerState, String buyerStateCode) {
        if (sellerStateCode != null && buyerStateCode != null && !sellerStateCode.isBlank() && !buyerStateCode.isBlank()) {
            return sellerStateCode.trim().equalsIgnoreCase(buyerStateCode.trim());
        }
        if (sellerState != null && buyerState != null && !sellerState.isBlank() && !buyerState.isBlank()) {
            return sellerState.trim().equalsIgnoreCase(buyerState.trim());
        }
        // Default to interstate if not specified or different
        return false;
    }
}
