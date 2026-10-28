package com.tdb.booking.repository;

import com.tdb.booking.entity.SlotMaster;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SlotMasterRepository extends JpaRepository<SlotMaster, UUID> {

    List<SlotMaster> findByTempleId(UUID templeId);

    List<SlotMaster> findByTempleIdAndIsActiveTrue(UUID templeId);

    boolean existsByTempleIdAndSlotNameAndStartTime(
            UUID templeId, String slotName, LocalTime startTime);

    Optional<SlotMaster> findByIdAndTempleId(UUID id, UUID templeId);
}
