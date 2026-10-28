package com.tdb.booking.repository;

import com.tdb.booking.entity.DarshanBooking;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DarshanBookingRepository extends JpaRepository<DarshanBooking, UUID> {

    List<DarshanBooking> findByDarshanSlotId(UUID darshanSlotId);

    List<DarshanBooking> findByPhoneNumber(String phoneNumber);
}
