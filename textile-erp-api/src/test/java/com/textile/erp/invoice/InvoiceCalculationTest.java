package com.textile.erp.invoice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.textile.erp.agent.entity.CommissionType;
import com.textile.erp.invoice.service.InvoiceCalculationService;
import com.textile.erp.invoice.service.InvoiceCalculationService.CalculatedItem;
import com.textile.erp.invoice.service.InvoiceCalculationService.InvoiceTotals;
import com.textile.erp.invoice.service.InvoiceNumberGenerator;
import com.textile.erp.invoice.util.NumberToWordsConverter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class InvoiceCalculationTest {

    private InvoiceCalculationService calculationService;
    private InvoiceNumberGenerator numberGenerator;

    @BeforeEach
    void setUp() {
        calculationService = new InvoiceCalculationService();
        numberGenerator = new InvoiceNumberGenerator(null);
    }

    @Test
    @DisplayName("Should correctly calculate folding less deduction and taxable amount for item")
    void testCalculateItem_success() {
        // Given: 1000 meters, 2.0% folding less, Rs. 45.50 per meter
        BigDecimal meters = new BigDecimal("1000.000");
        BigDecimal foldingLessPercent = new BigDecimal("2.000");
        BigDecimal rate = new BigDecimal("45.50");

        CalculatedItem item = calculationService.calculateItem(meters, foldingLessPercent, rate);

        // folding_less_meters = 1000 * 2 / 100 = 20.000
        assertThat(item.getFoldingLessMeters()).isEqualByComparingTo("20.000");
        // total_meters = 1000 - 20 = 980.000
        assertThat(item.getTotalMeters()).isEqualByComparingTo("980.000");
        // taxable_amount = 980 * 45.50 = 44590.00
        assertThat(item.getTaxableAmount()).isEqualByComparingTo("44590.00");
    }

    @Test
    @DisplayName("Should handle 0% folding less properly")
    void testCalculateItem_zeroFoldingLess() {
        CalculatedItem item = calculationService.calculateItem(
                new BigDecimal("500.000"),
                BigDecimal.ZERO,
                new BigDecimal("60.00")
        );

        assertThat(item.getFoldingLessMeters()).isEqualByComparingTo("0.000");
        assertThat(item.getTotalMeters()).isEqualByComparingTo("500.000");
        assertThat(item.getTaxableAmount()).isEqualByComparingTo("30000.00");
    }

    @Test
    @DisplayName("Should throw exception for negative meters, zero rate, or invalid folding percent")
    void testCalculateItem_validations() {
        assertThatThrownBy(() -> calculationService.calculateItem(new BigDecimal("-10"), BigDecimal.ZERO, new BigDecimal("10")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Item meters must be greater than zero");

        assertThatThrownBy(() -> calculationService.calculateItem(new BigDecimal("10"), BigDecimal.ZERO, BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Item rate per meter must be greater than zero");

        assertThatThrownBy(() -> calculationService.calculateItem(new BigDecimal("10"), new BigDecimal("-5"), new BigDecimal("10")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Folding less percentage must be between 0 and 100");
    }

    @Test
    @DisplayName("Should calculate Intrastate GST (CGST 2.5% + SGST 2.5%) when state matches")
    void testCalculateInvoiceTotals_intrastate() {
        CalculatedItem item1 = calculationService.calculateItem(new BigDecimal("100.000"), BigDecimal.ZERO, new BigDecimal("100.00")); // 10,000.00
        CalculatedItem item2 = calculationService.calculateItem(new BigDecimal("50.000"), BigDecimal.ZERO, new BigDecimal("200.00"));  // 10,000.00

        InvoiceTotals totals = calculationService.calculateInvoiceTotals(
                List.of(item1, item2),
                "Tamil Nadu",
                "33",
                "Tamil Nadu",
                "33"
        );

        // Subtotal = 20,000.00
        assertThat(totals.getSubtotal()).isEqualByComparingTo("20000.00");
        // CGST = 2.5% = 500.00
        assertThat(totals.getCgstRate()).isEqualByComparingTo("2.50");
        assertThat(totals.getCgstAmount()).isEqualByComparingTo("500.00");
        // SGST = 2.5% = 500.00
        assertThat(totals.getSgstRate()).isEqualByComparingTo("2.50");
        assertThat(totals.getSgstAmount()).isEqualByComparingTo("500.00");
        // IGST = 0.00
        assertThat(totals.getIgstRate()).isEqualByComparingTo("0.00");
        assertThat(totals.getIgstAmount()).isEqualByComparingTo("0.00");
        // Grand Total = 21,000.00
        assertThat(totals.getGrandTotal()).isEqualByComparingTo("21000.00");
        assertThat(totals.getAmountInWords()).contains("Twenty One Thousand");
    }

    @Test
    @DisplayName("Should calculate Interstate GST (IGST 5.0%) when state differs")
    void testCalculateInvoiceTotals_interstate() {
        CalculatedItem item = calculationService.calculateItem(new BigDecimal("100.000"), BigDecimal.ZERO, new BigDecimal("100.00")); // 10,000.00

        InvoiceTotals totals = calculationService.calculateInvoiceTotals(
                List.of(item),
                "Tamil Nadu",
                "33",
                "Maharashtra",
                "27"
        );

        assertThat(totals.getSubtotal()).isEqualByComparingTo("10000.00");
        assertThat(totals.getCgstAmount()).isEqualByComparingTo("0.00");
        assertThat(totals.getSgstAmount()).isEqualByComparingTo("0.00");
        // IGST = 5.0% = 500.00
        assertThat(totals.getIgstRate()).isEqualByComparingTo("5.00");
        assertThat(totals.getIgstAmount()).isEqualByComparingTo("500.00");
        assertThat(totals.getGrandTotal()).isEqualByComparingTo("10500.00");
        assertThat(totals.getRoundOffAmount()).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("Should correctly round off grand total to nearest integer rupee")
    void testCalculateInvoiceTotals_rounding() {
        // Subtotal = 100.35, Intrastate 5% GST = 2.51 CGST + 2.51 SGST = 5.02
        // Grand total before rounding = 105.37 -> rounded to 105.00, round-off = -0.37
        CalculatedItem item = calculationService.calculateItem(new BigDecimal("1.000"), BigDecimal.ZERO, new BigDecimal("100.35"));

        InvoiceTotals totals = calculationService.calculateInvoiceTotals(
                List.of(item),
                "Tamil Nadu",
                "33",
                "Tamil Nadu",
                "33"
        );

        // 100.35 * 0.025 = 2.50875 -> 2.51
        assertThat(totals.getCgstAmount()).isEqualByComparingTo("2.51");
        assertThat(totals.getSgstAmount()).isEqualByComparingTo("2.51");
        // 100.35 + 2.51 + 2.51 = 105.37 -> nearest integer = 105.00
        assertThat(totals.getGrandTotal()).isEqualByComparingTo("105.00");
        assertThat(totals.getRoundOffAmount()).isEqualByComparingTo("-0.37");
    }

    @Test
    @DisplayName("Should calculate commission for PERCENTAGE and FIXED types")
    void testCalculateCommission() {
        BigDecimal subtotal = new BigDecimal("50000.00");

        // Percentage: 2.0% of 50000 = 1000.00
        BigDecimal commPct = calculationService.calculateCommission(subtotal, CommissionType.PERCENTAGE, new BigDecimal("2.00"));
        assertThat(commPct).isEqualByComparingTo("1000.00");

        // Fixed: Rs. 1500.00
        BigDecimal commFixed = calculationService.calculateCommission(subtotal, CommissionType.FIXED, new BigDecimal("1500.00"));
        assertThat(commFixed).isEqualByComparingTo("1500.00");
    }

    @Test
    @DisplayName("Should convert numbers to words in Indian numbering format")
    void testNumberToWordsConverter() {
        assertThat(NumberToWordsConverter.convertToIndianCurrencyWords(new BigDecimal("0.00"))).isEqualTo("Rupees Zero Only");
        assertThat(NumberToWordsConverter.convertToIndianCurrencyWords(new BigDecimal("105.00"))).isEqualTo("Rupees One Hundred Five Only");
        assertThat(NumberToWordsConverter.convertToIndianCurrencyWords(new BigDecimal("123456.78"))).isEqualTo("Rupees One Lakh Twenty Three Thousand Four Hundred Fifty Six and Seventy Eight Paise Only");
        assertThat(NumberToWordsConverter.convertToIndianCurrencyWords(new BigDecimal("10000000.00"))).isEqualTo("Rupees One Crore Only");
    }

    @Test
    @DisplayName("Should calculate financial year correctly based on April cutoff")
    void testFinancialYearCalculation() {
        // In or after April -> FY is year to (year+1)
        assertThat(numberGenerator.calculateFinancialYear(LocalDate.of(2026, 4, 1))).isEqualTo("2026-27");
        assertThat(numberGenerator.calculateFinancialYear(LocalDate.of(2026, 9, 18))).isEqualTo("2026-27");
        assertThat(numberGenerator.calculateFinancialYear(LocalDate.of(2026, 12, 31))).isEqualTo("2026-27");

        // Before April -> FY is (year-1) to year
        assertThat(numberGenerator.calculateFinancialYear(LocalDate.of(2026, 1, 15))).isEqualTo("2025-26");
        assertThat(numberGenerator.calculateFinancialYear(LocalDate.of(2026, 3, 31))).isEqualTo("2025-26");
    }
}
