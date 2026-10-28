package com.tdb.booking.repository;

import com.tdb.booking.entity.TempleMaster;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TempleMasterRepository extends JpaRepository<TempleMaster, UUID> {

    Optional<TempleMaster> findByTempleCode(String templeCode);

    boolean existsByTempleCode(String templeCode);

    List<TempleMaster> findByIsActiveTrue();
}
