package com.tdb.booking.repository;

import com.tdb.booking.entity.TempleReceiptSequence;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for {@link TempleReceiptSequence} entities. Includes pessimistic
 * locking query to prevent duplicate sequence allocation during concurrent bookings.
 */
@Repository
public interface TempleReceiptSequenceRepository
        extends JpaRepository<TempleReceiptSequence, UUID> {

    /**
     * Retrieves the receipt sequence row for a temple and financial year using a pessimistic write
     * lock. Equivalent to SELECT ... FOR UPDATE.
     *
     * @param templeId unique identifier of the temple
     * @param financialYear financial year string, e.g. "2026-27"
     * @return Optional containing the locked sequence if found
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
            "SELECT s FROM TempleReceiptSequence s WHERE s.temple.id = :templeId AND"
                    + " s.financialYear = :financialYear")
    Optional<TempleReceiptSequence> findByTempleIdAndFinancialYearForUpdate(
            @Param("templeId") UUID templeId, @Param("financialYear") String financialYear);

    /**
     * Non-locking read of sequence for auditing or testing.
     *
     * @param templeId unique identifier of the temple
     * @param financialYear financial year string, e.g. "2026-27"
     * @return Optional containing sequence if found
     */
    Optional<TempleReceiptSequence> findByTempleIdAndFinancialYear(
            UUID templeId, String financialYear);
}
