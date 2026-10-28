package com.tdb.booking.repository;

import com.tdb.booking.entity.TempleMaster;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TempleMasterRepository extends JpaRepository<TempleMaster, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM TempleMaster t WHERE t.id = :id")
    Optional<TempleMaster> findByIdForUpdate(@Param("id") UUID id);

    Optional<TempleMaster> findByTempleCode(String templeCode);

    boolean existsByTempleCode(String templeCode);

    List<TempleMaster> findByIsActiveTrue();
}
