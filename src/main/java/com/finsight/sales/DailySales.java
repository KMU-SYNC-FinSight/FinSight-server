package com.finsight.sales;

import java.time.LocalDate;

/**
 * 하루 매출.
 */
public record DailySales(
        LocalDate date,
        long salesAmount,
        int transactionCount
) {
}
