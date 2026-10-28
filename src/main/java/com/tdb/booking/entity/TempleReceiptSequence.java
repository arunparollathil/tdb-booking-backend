package com.tdb.booking.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.Objects;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Entity maintaining the running sequence counter for receipt numbers per temple and financial
 * year. Ensures independent sequences starting from 000001 for each financial year.
 */
@Entity
@Table(
        name = "temple_receipt_sequence",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_temple_receipt_sequence_temple_fy",
                    columnNames = {"temple_id", "financial_year"})
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class TempleReceiptSequence extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "temple_id", nullable = false)
    private TempleMaster temple;

    @Column(name = "financial_year", nullable = false, length = 10)
    private String financialYear;

    @Column(name = "last_sequence_number", nullable = false)
    private Long lastSequenceNumber;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TempleReceiptSequence that = (TempleReceiptSequence) o;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
