package com.tdb.booking.repository;

import com.tdb.booking.entity.DarshanSlot;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DarshanSlotRepository extends JpaRepository<DarshanSlot, UUID> {

    Optional<DarshanSlot> findBySlotMasterIdAndSlotDate(UUID slotMasterId, LocalDate slotDate);

    boolean existsBySlotMasterIdAndSlotDate(UUID slotMasterId, LocalDate slotDate);

    List<DarshanSlot> findByTempleIdAndSlotDate(UUID templeId, LocalDate slotDate);

    List<DarshanSlot> findByTempleIdAndSlotDateBetween(
            UUID templeId, LocalDate startDate, LocalDate endDate);

    List<DarshanSlot> findByTempleIdAndSlotDateAndIsActiveTrue(UUID templeId, LocalDate slotDate);
}
