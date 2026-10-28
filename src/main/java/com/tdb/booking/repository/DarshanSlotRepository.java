package com.tdb.booking.repository;

import com.tdb.booking.entity.DarshanSlot;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for {@link DarshanSlot} entities. Includes pessimistic locking query
 * to prevent overbooking during high-concurrency ticket reservations.
 */
@Repository
public interface DarshanSlotRepository extends JpaRepository<DarshanSlot, UUID> {

    /**
     * Retrieves a darshan slot by ID with a pessimistic write lock (SELECT ... FOR UPDATE). Ensures
     * strict row-level locking during concurrency to prevent overbooking.
     *
     * @param slotId unique identifier of the darshan slot
     * @return {@link Optional} containing the locked {@link DarshanSlot} if found
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM DarshanSlot s WHERE s.id = :slotId")
    Optional<DarshanSlot> findByIdForUpdate(@Param("slotId") UUID slotId);

    /**
     * Backward-compatible alias for {@link #findByIdForUpdate(UUID)}.
     *
     * @param id unique identifier of the darshan slot
     * @return {@link Optional} containing the locked {@link DarshanSlot} if found
     */
    default Optional<DarshanSlot> findByIdWithLock(UUID id) {
        return findByIdForUpdate(id);
    }

    Optional<DarshanSlot> findBySlotMasterIdAndSlotDate(UUID slotMasterId, LocalDate slotDate);

    boolean existsBySlotMasterIdAndSlotDate(UUID slotMasterId, LocalDate slotDate);

    List<DarshanSlot> findByTempleIdAndSlotDate(UUID templeId, LocalDate slotDate);

    List<DarshanSlot> findByTempleIdAndSlotDateBetween(
            UUID templeId, LocalDate startDate, LocalDate endDate);

    List<DarshanSlot> findByTempleIdAndSlotDateAndIsActiveTrue(UUID templeId, LocalDate slotDate);
}
