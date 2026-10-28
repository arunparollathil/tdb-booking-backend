package com.tdb.booking.service;

import com.tdb.booking.entity.TempleMaster;
import java.time.LocalDate;

/**
 * Service for generating unique, sequentially incremented receipt numbers for temple darshan
 * bookings. Enforces format: {TEMPLE_CODE}/{FINANCIAL_YEAR}/{SEQUENCE} (e.g. SBM/2026-27/000123).
 */
public interface ReceiptNumberService {

    /**
     * Generates a unique sequential receipt number for the given temple using the current date to
     * determine the financial year.
     *
     * @param temple the temple for which the receipt is issued
     * @return formatted receipt number string, e.g. "SBM/2026-27/000001"
     */
    String generateReceiptNumber(TempleMaster temple);

    /**
     * Generates a unique sequential receipt number for the given temple and specific reference
     * date.
     *
     * @param temple the temple for which the receipt is issued
     * @param date the reference date for calculating the financial year
     * @return formatted receipt number string, e.g. "SBM/2026-27/000001"
     */
    String generateReceiptNumber(TempleMaster temple, LocalDate date);

    /**
     * Calculates the Indian financial year (April 1 to March 31) for a given date. Format: YYYY-YY
     * (e.g. 2026-27).
     *
     * @param date date to evaluate (defaults to current date if null)
     * @return formatted financial year string
     */
    String calculateFinancialYear(LocalDate date);
}
